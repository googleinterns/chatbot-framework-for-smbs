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

import com.chatbot.protobuf.UseCaseOuterClass.UseCase;
import com.google.cloud.dialogflow.v2.Intent;
import com.google.cloud.dialogflow.v2.Context;
import com.google.cloud.dialogflow.v2.Intent.TrainingPhrase;
import com.google.cloud.dialogflow.v2.Intent.TrainingPhrase.Part;
import com.google.cloud.dialogflow.v2.IntentsClient;
import com.google.cloud.dialogflow.v2.ProjectAgentName;
import com.google.protobuf.TextFormat;

public class App {
  public static void main(final String[] args) throws IOException {
    String projectID = System.getenv("projectID");
    // this map is needed to check if an intent display name already exists and if so, instead of 
    // creating an intent (which would return an error) we update the intent using the name
    Map<String, String> intentDisplayNameToName = new HashMap<String, String>();
    try (IntentsClient intentsClient = IntentsClient.create()) {
      // Set the project agent name using the projectID
      ProjectAgentName parent = ProjectAgentName.of(projectID);
      // Performs the list intents request
      for (Intent intent : intentsClient.listIntents(parent).iterateAll()) {
        intentDisplayNameToName.put(intent.getDisplayName(), intent.getName());
      }
      // the name of the file which contains the definition of the use case in proto text format
      final String filename = args[0];
      final Path useCaseFilePath = Paths.get(filename);
      String fileContent = "";
      try {
        fileContent = Files.readString(useCaseFilePath, StandardCharsets.US_ASCII);
      } catch (final IOException e1) {
        e1.printStackTrace();
      }
      final UseCase.Builder useCaseBuilder = UseCase.newBuilder();
      // the use case file is a proto text file, so it is parsed using the textformat parser
      final TextFormat.Parser textFromatParser = TextFormat.getParser();
      try {
        textFromatParser.merge(fileContent, useCaseBuilder);
      } catch (final Exception e) {
        e.printStackTrace();
      }
      UseCase useCase = useCaseBuilder.build();
      List<UseCase.Intent> intentList = useCase.getIntentsList();
      List<Intent> intentProtobufList = new ArrayList<Intent>();
      // if the type of an intent is YES/NO some general training phrases can be added to the list
      // of training phrases for the intent
      List<String> YES_TRAINING_PHRASES = new ArrayList<String>(Arrays.asList("yes", "okay I will",
          "why not", "yes that's alright", "yes I do", "exactly", "of course", "yep that's ok",
          "okay", "ok", "sure"));
      List<String> NO_TRAINING_PHRASES = new ArrayList<String>(Arrays.asList("No", "thanks but no",
          "no way", "no no don't", "na", "no it isn't", "don't", "nah I'm good", "no I cannot",
          "I can't"));
      for(UseCase.Intent intent: intentList) {
        Intent.Builder intentProtobufBuilder = Intent.newBuilder();
        intentProtobufBuilder.setDisplayName(intent.getIntentName());        
        List<Context> outputContextProtobufList= new ArrayList<Context>();
        // output contexts need to be set to the input contexts of the successor intents
        for(String successor: intent.getSuccessorsList()) {
          Context contextProtobuf = Context.newBuilder()
          .setLifespanCount(1)
          .setName("projects/" + projectID + "/agent/sessions/-/contexts/" + successor + "Context")
          .build();
          outputContextProtobufList.add(contextProtobuf);
        }
        intentProtobufBuilder.addAllOutputContexts(outputContextProtobufList);
        intentProtobufBuilder.addInputContextNames("projects/" + projectID +
            "/agent/sessions/-/contexts/" + intent.getIntentName() + "Context");
        List<TrainingPhrase> trainingPhrasesProtobufList = new ArrayList<TrainingPhrase>();
        List<String> trainingPhrasesStringList =
            new ArrayList<String>(intent.getTrainingPhrasesList());
        if(intent.getIntentType() == UseCase.IntentType.YES) {
          trainingPhrasesStringList.addAll(YES_TRAINING_PHRASES);
        }
        else if (intent.getIntentType() == UseCase.IntentType.NO) {
          trainingPhrasesStringList.addAll(NO_TRAINING_PHRASES);
        }
        for(String trainingPhrase: trainingPhrasesStringList) {
          // Trainging phrases are encoded into strings to accomodate entities into them, so they
          // need to be converted into the training phrase protobuf
          TrainingPhrase trainingPhraseProtobuf = encodedStringToTrainingPhrase(trainingPhrase);
          trainingPhrasesProtobufList.add(trainingPhraseProtobuf);
        }
        intentProtobufBuilder.addAllTrainingPhrases(trainingPhrasesProtobufList);
        
        Intent.Message message = Intent.Message.newBuilder()
        .setText(Intent.Message.Text.newBuilder().addAllText(intent.getResponsesList()).build())
        .build();
        intentProtobufBuilder.addMessages(message);

        intentProtobufBuilder.addAllEvents(intent.getEventsList());

        // if the intent already exists, the intent name must be added to the protobuf
        if(intentDisplayNameToName.containsKey(intent.getIntentName())) {
          intentProtobufBuilder.setName(intentDisplayNameToName.get(intent.getIntentName()));
        }
        intentProtobufList.add(intentProtobufBuilder.build());
      }
      for(Intent intent: intentProtobufList) {
        if(intentDisplayNameToName.containsKey(intent.getDisplayName())) {
          intentsClient.updateIntent(intent, "en");
        }
        else {
          intentsClient.createIntent(parent, intent);
        }
      } 
    }
  }

  // function to convert a training phrase string into a trainging phrase protobuf
  public static TrainingPhrase encodedStringToTrainingPhrase(String trainingPhrase) {
    TrainingPhrase.Builder trainingPhraseProtobufBuilder = TrainingPhrase.newBuilder();
    String[] trainingPhraseParts = trainingPhrase.split("\\|");
    List<Part> partProtobufList = new ArrayList<Part>();
    for(String part: trainingPhraseParts) {
      Part partProtobuf;
      // semicolon implies that this part is an entity
      if(part.indexOf(";") != -1) {
        String[] entityParts = part.split(";");
        partProtobuf = Part.newBuilder()
        .setEntityType(entityParts[0])
        .setAlias(entityParts[1])
        .setText(entityParts[2])
        .build();
      } else {
        partProtobuf = Part.newBuilder().setText(part).build();
      }
      partProtobufList.add(partProtobuf);
    }
    trainingPhraseProtobufBuilder.addAllParts(partProtobufList);
    return trainingPhraseProtobufBuilder.build();
  }
}