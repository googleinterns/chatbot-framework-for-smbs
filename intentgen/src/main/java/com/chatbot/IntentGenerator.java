package com.chatbot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.chatbot.protobuf.UseCaseOuterClass.UseCase;
import com.google.cloud.dialogflow.v2.Context;
import com.google.cloud.dialogflow.v2.Intent;
import com.google.cloud.dialogflow.v2.Intent.Parameter;
import com.google.cloud.dialogflow.v2.Intent.TrainingPhrase;
import com.google.cloud.dialogflow.v2.Intent.WebhookState;
import com.google.cloud.dialogflow.v2.Intent.TrainingPhrase.Part;
import com.google.cloud.dialogflow.v2.ProjectAgentName;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.protobuf.TextFormat;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class IntentGenerator {
  private static final List<String> YES_TRAINING_PHRASES = new ArrayList<String>(Arrays.asList(
      "yes", "okay I will", "why not", "yes that's alright", "yes I do", "exactly", "of course",
      "yep that's ok", "okay", "ok", "sure"));
  private static final List<String> NO_TRAINING_PHRASES = new ArrayList<String>(Arrays.asList("No",
      "thanks but no", "no way", "no no don't", "na", "no it isn't", "don't", "nah I'm good",
      "no I cannot", "I can't"));
  private static final int ENTITY_TYPE_INDEX = 0;
  private static final int ALIAS_INDEX = 1;
  private static final int VALUE_INDEX = 2;
  private static Map<String, String> intentDisplayNameToName;
  private static String projectID;

  public static void main(final String[] args) throws ParseException, IllegalArgumentException,
      IOException {
    final Options options = new Options();
    final Option useCaseFile = Option.builder("f").hasArg()
        .desc("prototxt file describing the use case" ).build();
    options.addOption(useCaseFile);
    final CommandLineParser parser = new DefaultParser();
    final CommandLine cmd = parser.parse(options, args);
    final String fileName = cmd.getOptionValue("f");
    if(fileName.isEmpty()) {
      throw new IllegalArgumentException("No file name provided");
    }
    IntentsClientWrapper intentsClient = new IntentsClientWrapper();
    generateIntents(fileName, ProjectAgentName.of(System.getenv("projectID")), intentsClient);
    intentsClient.finalize();
  }

  static void generateIntents(final String fileName, final ProjectAgentName parent,
      final IntentsClientWrapper intentsClient) throws IllegalArgumentException,
      NullPointerException, IOException {
    // this map is needed to check if an intent display name already exists and if so, instead of 
    // creating an intent (which would return an error) we update the intent using the name
    generateIntentDisplayNametoNameMapping(intentsClient, parent);
    final List<Intent> intentProtobufList = getIntentsInUseCase(fileName).stream()
        .map(x->buildIntentProtobufForIntent(x, projectID))
        .collect(Collectors.toList());
    createOrUpdateIntents(intentProtobufList, intentsClient, parent);
  }

  // To include entities into strings the syntax used is: |entity-type;alias;value|
  // A valid string would be "some text |@sys.name;name;Foo| some text"
  static TrainingPhrase buildTrainingPhraseFromEncodedString(final String trainingPhrase)
      throws IllegalArgumentException {
    final TrainingPhrase.Builder trainingPhraseProtobufBuilder = TrainingPhrase.newBuilder();
    final String[] trainingPhraseParts = trainingPhrase.split("\\|");
    final List<Part> partProtobufList = new ArrayList<Part>();
    for(final String part: trainingPhraseParts) {
      Part partProtobuf;
      // semicolon implies that this part is an entity
      if(part.indexOf(";") != -1) {
        final String[] entityParts = part.split(";");
        if(entityParts.length != 3) {
          throw new IllegalArgumentException("Incorrect entity encoding in training string");
        }
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
    return trainingPhraseProtobufBuilder.addAllParts(partProtobufList).build();
  }
  
  static List<UseCase.Intent> getIntentsInUseCase(final String filename) throws IOException,
      IllegalArgumentException, NullPointerException {
    final InputStream inputStream = IntentGenerator.class.getResourceAsStream("/" + filename);
    final String fileContent = CharStreams.toString(new InputStreamReader(
      inputStream, Charsets.UTF_8));
    if(fileContent.isEmpty()) {
      throw new IllegalArgumentException("File provided is empty");
    }
    final UseCase.Builder useCaseBuilder = UseCase.newBuilder();
    final TextFormat.Parser textFromatParser = TextFormat.getParser();
    textFromatParser.merge(fileContent, useCaseBuilder);
    return useCaseBuilder.build().getIntentsList();
  }

  private static Intent.Builder addTrainingPhrases(final UseCase.Intent intent,
      final Intent.Builder intentProtobufBuilder) {
    final List<TrainingPhrase> trainingPhrasesProtobufList = new ArrayList<TrainingPhrase>();
    final List<String> trainingPhrasesStringList =
        new ArrayList<String>(intent.getTrainingPhrasesList());
    if(intent.getIntentType() == UseCase.IntentType.YES) {
      trainingPhrasesStringList.addAll(YES_TRAINING_PHRASES);
    }
    else if (intent.getIntentType() == UseCase.IntentType.NO) {
      trainingPhrasesStringList.addAll(NO_TRAINING_PHRASES);
    }
    final List<TrainingPhrase> userTrainingPhrases = trainingPhrasesStringList.stream()
        .map(trainingPhrase -> buildTrainingPhraseFromEncodedString(trainingPhrase))
        .collect(Collectors.toList()); 
    trainingPhrasesProtobufList.addAll(userTrainingPhrases);
    intentProtobufBuilder.addAllTrainingPhrases(trainingPhrasesProtobufList);
    return intentProtobufBuilder;
  }

  private static Intent.Builder addResponses(final UseCase.Intent intent,
      final Intent.Builder intentProtobufBuilder) {
    final Intent.Message message = Intent.Message.newBuilder()
        .setText(Intent.Message.Text.newBuilder().addAllText(intent.getResponsesList()).build())
        .build();
    return intentProtobufBuilder.addMessages(message);
  }

  private static Intent.Builder addVariables(final UseCase.Intent intent,
      final Intent.Builder intentProtobufBuilder) {
      final List<String> variables = intent.getVariablesList();
      final List<Parameter> parameters = variables.stream().map(variable -> 
          Parameter.newBuilder().setDisplayName(variable).setValue("$" + variable)
          .setDefaultValue("#" + intent.getIntentName() + "Context." + variable).build())
          .collect(Collectors.toList());
      return intentProtobufBuilder.addAllParameters(parameters);
  }

  private static Intent.Builder addOutputContexts(final UseCase.Intent intent,
      final Intent.Builder intentProtobufBuilder, final String projectID) {
    final List<Context> outputContextProtobufList= new ArrayList<Context>();
    for(final String successor: intent.getSuccessorsList()) {
      final Context contextProtobuf = Context.newBuilder()
      .setLifespanCount(1)
      .setName("projects/" + projectID + "/agent/sessions/-/contexts/" + successor + "Context")
      .build();
      outputContextProtobufList.add(contextProtobuf);
    }
    return intentProtobufBuilder.addAllOutputContexts(outputContextProtobufList);
  }

  private static void createOrUpdateIntents(final List<Intent> intentProtobufList,
      final IntentsClientWrapper intentsClient, final ProjectAgentName parent) {
    for(final Intent intent: intentProtobufList) {
      if(intentDisplayNameToName.containsKey(intent.getDisplayName())) {
        intentsClient.updateIntent(intent, "en");
      }
      else {
        intentsClient.createIntent(parent, intent);
      }
    }
  }

  private static Intent buildIntentProtobufForIntent(final UseCase.Intent intent,
      final String projectID) {
    Intent.Builder intentProtobufBuilder = Intent.newBuilder();
    intentProtobufBuilder.setDisplayName(intent.getIntentName());        
    // output contexts need to be set to the input contexts of the successor intents
    addOutputContexts(intent, intentProtobufBuilder, projectID);
    intentProtobufBuilder.addInputContextNames("projects/" + projectID +
        "/agent/sessions/-/contexts/" + intent.getIntentName() + "Context");
    intentProtobufBuilder = addTrainingPhrases(intent, intentProtobufBuilder);
    intentProtobufBuilder = addResponses(intent, intentProtobufBuilder);        
    intentProtobufBuilder = addVariables(intent, intentProtobufBuilder);
    intentProtobufBuilder.addAllEvents(intent.getEventsList());
    if(intent.getFulfillmentEnabled()) {
      intentProtobufBuilder.setWebhookStateValue(WebhookState.WEBHOOK_STATE_ENABLED_VALUE);
    }
    if(intentDisplayNameToName.containsKey(intent.getIntentName())) {
      intentProtobufBuilder.setName(intentDisplayNameToName.get(intent.getIntentName()));
    }
    return intentProtobufBuilder.build();
  }

  private static void generateIntentDisplayNametoNameMapping(
      final IntentsClientWrapper intentsClient, final ProjectAgentName parent) throws IOException {
    intentDisplayNameToName = new HashMap<String, String>();
    intentsClient.getIntentsList(parent)
        .forEach(intent -> intentDisplayNameToName.put(intent.getDisplayName(), intent.getName()));
  }
}