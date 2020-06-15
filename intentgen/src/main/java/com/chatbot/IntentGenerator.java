package com.chatbot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.chatbot.protobuf.UseCaseOuterClass.UseCase;
import com.google.cloud.dialogflow.v2.Intent;
import com.google.cloud.dialogflow.v2.Context;
import com.google.cloud.dialogflow.v2.Intent.TrainingPhrase;
import com.google.cloud.dialogflow.v2.Intent.TrainingPhrase.Part;
import com.google.cloud.dialogflow.v2.IntentsClient;
import com.google.cloud.dialogflow.v2.ProjectAgentName;
import com.google.protobuf.TextFormat;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class IntentGenerator {
  static final List<String> YES_TRAINING_PHRASES = new ArrayList<String>(Arrays.asList("yes",
      "okay I will", "why not", "yes that's alright", "yes I do", "exactly", "of course",
      "yep that's ok", "okay", "ok", "sure"));
  static final List<String> NO_TRAINING_PHRASES = new ArrayList<String>(Arrays.asList("No",
      "thanks but no", "no way", "no no don't", "na", "no it isn't", "don't", "nah I'm good",
      "no I cannot", "I can't"));
  static final int ENTITY_TYPE_INDEX = 0;
  static final int ALIAS_INDEX = 1;
  static final int VALUE_INDEX = 2;
  private static Map<String, String> intentDisplayNameToName;

  public static void main(final String[] args) throws ParseException {
    Options options = new Options();
    final Option useCaseFile = Option.builder("f").hasArg()
        .desc("prototxt file describing the use case" ).build();
    options.addOption(useCaseFile);
    final CommandLineParser parser = new DefaultParser();
    final CommandLine cmd = parser.parse(options, args);
    final String fileName = cmd.getOptionValue("f");
    if(fileName.isEmpty()) {
      throw new ParseException("No file name provided");
    }
    final String projectID = System.getenv("projectID");
    final ProjectAgentName parent = ProjectAgentName.of(projectID);
    try(IntentsClient intentsClient = IntentsClient.create()) {
      // this map is needed to check if an intent display name already exists and if so, instead of 
      // creating an intent (which would return an error) we update the intent using the name
      intentDisplayNameToName = getIntentDisplayNametoNameMapping(intentsClient, parent);
      final List<Intent> intentProtobufList = getIntentsInUseCase(fileName).stream()
          .map(x->buildIntentProtobufForIntent(x, projectID))
          .collect(Collectors.toList());
      createOrUpdateIntents(intentProtobufList, intentsClient, intentDisplayNameToName, parent);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // To include entities into strings the syntax used is: |entity-type;alias;value|
  // A valid string would be "some text |@sys.name;name;Foo| some text"
  public static TrainingPhrase buildTrainingPhraseFromEncodedString(String trainingPhrase) {
    TrainingPhrase.Builder trainingPhraseProtobufBuilder = TrainingPhrase.newBuilder();
    final String[] trainingPhraseParts = trainingPhrase.split("\\|");
    List<Part> partProtobufList = new ArrayList<Part>();
    for(String part: trainingPhraseParts) {
      Part partProtobuf;
      // semicolon implies that this part is an entity
      if(part.indexOf(";") != -1) {
        final String[] entityParts = part.split(";");
        partProtobuf = Part.newBuilder()
        .setEntityType(entityParts[ENTITY_TYPE_INDEX])
        .setAlias(entityParts[ALIAS_INDEX])
        .setText(entityParts[VALUE_INDEX])
        .build();
      } else {
        partProtobuf = Part.newBuilder().setText(part).build();
      }
      partProtobufList.add(partProtobuf);
    }
    trainingPhraseProtobufBuilder.addAllParts(partProtobufList);
    return trainingPhraseProtobufBuilder.build();
  }
  
  private static List<UseCase.Intent> getIntentsInUseCase(String filename) throws IOException {
    final Path useCaseFilePath = Paths.get(filename);
    String fileContent = "";
    fileContent = Files.readString(useCaseFilePath, StandardCharsets.US_ASCII);
    final UseCase.Builder useCaseBuilder = UseCase.newBuilder();
    // the use case file is a proto text file, so it is parsed using the textformat parser
    final TextFormat.Parser textFromatParser = TextFormat.getParser();
    textFromatParser.merge(fileContent, useCaseBuilder);
    return useCaseBuilder.build().getIntentsList();
  }

  private static Intent.Builder addTrainingPhrases(UseCase.Intent intent,
      Intent.Builder intentProtobufBuilder) {
    List<TrainingPhrase> trainingPhrasesProtobufList = new ArrayList<TrainingPhrase>();
    List<String> trainingPhrasesStringList =
        new ArrayList<String>(intent.getTrainingPhrasesList());
    if(intent.getIntentType() == UseCase.IntentType.YES) {
      trainingPhrasesStringList.addAll(YES_TRAINING_PHRASES);
    }
    else if (intent.getIntentType() == UseCase.IntentType.NO) {
      trainingPhrasesStringList.addAll(NO_TRAINING_PHRASES);
    }
    List<TrainingPhrase> userTrainingPhrases = trainingPhrasesStringList.stream()
        .map(trainingPhrase -> buildTrainingPhraseFromEncodedString(trainingPhrase))
        .collect(Collectors.toList()); 
    trainingPhrasesProtobufList.addAll(userTrainingPhrases);
    intentProtobufBuilder.addAllTrainingPhrases(trainingPhrasesProtobufList);
    return intentProtobufBuilder;
  }

  private static Intent.Builder addResponses(UseCase.Intent intent,
      Intent.Builder intentProtobufBuilder) {
    final Intent.Message message = Intent.Message.newBuilder()
        .setText(Intent.Message.Text.newBuilder().addAllText(intent.getResponsesList()).build())
        .build();
    return intentProtobufBuilder.addMessages(message);
  }

  private static Intent.Builder addOutputContexts(UseCase.Intent intent,
      Intent.Builder intentProtobufBuilder, String projectID) {
    List<Context> outputContextProtobufList= new ArrayList<Context>();
    for(String successor: intent.getSuccessorsList()) {
      Context contextProtobuf = Context.newBuilder()
      .setLifespanCount(1)
      .setName("projects/" + projectID + "/agent/sessions/-/contexts/" + successor + "Context")
      .build();
      outputContextProtobufList.add(contextProtobuf);
    }
    return intentProtobufBuilder.addAllOutputContexts(outputContextProtobufList);
  }

  private static void createOrUpdateIntents(List<Intent> intentProtobufList,
    IntentsClient intentsClient, Map<String, String> intentDisplayNameToName,
    ProjectAgentName parent) {
    for(Intent intent: intentProtobufList) {
      if(intentDisplayNameToName.containsKey(intent.getDisplayName())) {
        intentsClient.updateIntent(intent, "en");
      }
      else {
        intentsClient.createIntent(parent, intent);
      }
    }
  }

  private static Intent buildIntentProtobufForIntent(UseCase.Intent intent, String projectID) {
    Intent.Builder intentProtobufBuilder = Intent.newBuilder();
    intentProtobufBuilder.setDisplayName(intent.getIntentName());        
    // output contexts need to be set to the input contexts of the successor intents
    addOutputContexts(intent, intentProtobufBuilder, projectID);
    intentProtobufBuilder.addInputContextNames("projects/" + projectID +
        "/agent/sessions/-/contexts/" + intent.getIntentName() + "Context");

    intentProtobufBuilder = addTrainingPhrases(intent, intentProtobufBuilder);
    intentProtobufBuilder = addResponses(intent, intentProtobufBuilder);        
    intentProtobufBuilder.addAllEvents(intent.getEventsList());

    if(intentDisplayNameToName.containsKey(intent.getIntentName())) {
      intentProtobufBuilder.setName(intentDisplayNameToName.get(intent.getIntentName()));
    }
    return intentProtobufBuilder.build();
  }

  private static Map<String, String> getIntentDisplayNametoNameMapping(
        IntentsClient intentsClient, ProjectAgentName parent) {
    Map<String, String> intentDisplayNameToName = new HashMap<String, String>();
    for (Intent intent : intentsClient.listIntents(parent).iterateAll()) {
      intentDisplayNameToName.put(intent.getDisplayName(), intent.getName());
    }
    return intentDisplayNameToName;
  }
}