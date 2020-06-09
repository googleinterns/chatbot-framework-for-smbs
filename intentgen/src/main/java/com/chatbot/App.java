package com.chatbot;

import com.chatbot.protobuf.UseCaseOuterClass.UseCase;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.cloud.dialogflow.v2.*;
import com.google.cloud.dialogflow.v2.Intent.TrainingPhrase;
import com.google.cloud.dialogflow.v2.Intent.TrainingPhrase.Part;


public class App {
  public static void main(final String[] args) throws IOException {
    String projectID = System.getenv("projectID");
    // this map is needed to check if an intent display name already exists and if so, instead of 
    // creating an intent (which would return an error) we update the intent using the name
    Map<String, String> intent_display_name_to_name = new HashMap<String, String>();
    try (IntentsClient intentsClient = IntentsClient.create()) {
      // Set the project agent name using the projectID
      ProjectAgentName parent = ProjectAgentName.of(projectID);
      // Performs the list intents request
      for (Intent intent : intentsClient.listIntents(parent).iterateAll()) {
        intent_display_name_to_name.put(intent.getDisplayName(), intent.getName());
      }
      // the name of the file which contains the definition of the use case in proto text format
      final String filename = args[0];
      final Path use_case_file_path = Paths.get(filename);
      String file_content = "";
      try {
        file_content = Files.readString(use_case_file_path, StandardCharsets.US_ASCII);
      } catch (final IOException e1) {
        e1.printStackTrace();
      }
      final UseCase.Builder use_case_builder = UseCase.newBuilder();
      // the use case file is a proto text file, so it is parsed using the textformat parser
      final TextFormat.Parser text_format_parser = TextFormat.getParser();
      try {
        text_format_parser.merge(file_content, use_case_builder);
      } catch (final ParseException e) {
        e.printStackTrace();
      }
      UseCase use_case = use_case_builder.build();
      List<UseCase.Intent> intent_list = use_case.getIntentsList();
      List<Intent> intent_protobuf_list = new ArrayList<Intent>();
      for(UseCase.Intent intent: intent_list) {
        Intent.Builder intent_protobuf_builder = Intent.newBuilder();
        intent_protobuf_builder.setDisplayName(intent.getIntentName());        
        List<Context> output_context_protobuf_list= new ArrayList<Context>();
        // output contexts need to be set to the input contexts of the successor intents
        for(String successor: intent.getSuccessorsList()) {
          Context context_protobuf = Context.newBuilder()
          .setLifespanCount(1)
          .setName("projects/" + projectID + "/agent/sessions/-/contexts/" + successor + "Context")
          .build();
          output_context_protobuf_list.add(context_protobuf);
        }
        intent_protobuf_builder.addAllOutputContexts(output_context_protobuf_list);
        intent_protobuf_builder.addInputContextNames("projects/" + projectID +
            "/agent/sessions/-/contexts/" + intent.getIntentName() + "Context");
        List<TrainingPhrase> training_phrases_protobuf_list = new ArrayList<TrainingPhrase>();
        for(String training_phrase: intent.getTrainingPhrasesList()) {
          // Trainging phrases are encoded into strings to accomodate entities into them so they
          // need to be converted into the training phrase protobuf
          TrainingPhrase training_phrase_protobuf = encodedStringToTrainingPhrase(training_phrase);
          training_phrases_protobuf_list.add(training_phrase_protobuf);
        }
        intent_protobuf_builder.addAllTrainingPhrases(training_phrases_protobuf_list);
        
        Intent.Message message = Intent.Message.newBuilder()
        .setText(Intent.Message.Text.newBuilder().addAllText(intent.getResponsesList()).build())
        .build();
        intent_protobuf_builder.addMessages(message);

        intent_protobuf_builder.addAllEvents(intent.getEventsList());

        // if the intent already exists, the intent name must be added to the protobuf
        if(intent_display_name_to_name.containsKey(intent.getIntentName()))
          intent_protobuf_builder.setName(intent_display_name_to_name.get(intent.getIntentName()));
        intent_protobuf_list.add(intent_protobuf_builder.build());
      }
      for(Intent intent: intent_protobuf_list) {
        if(intent_display_name_to_name.containsKey(intent.getDisplayName()))
          intentsClient.updateIntent(intent, "en");
        else
          intentsClient.createIntent(parent, intent);
      } 
    }
  }

  // function to convert a training phrase string into a trainging phrase protobuf
  public static TrainingPhrase encodedStringToTrainingPhrase(String training_phrase) {
    TrainingPhrase.Builder training_phrase_protobuf_builder = TrainingPhrase.newBuilder();
    String[] training_phrase_parts = training_phrase.split("\\|");
    List<Part> part_protobuf_list = new ArrayList<Part>();
    for(String part: training_phrase_parts) {
      Part part_protobuf;
      // semicolon implies that this part is an entity
      if(part.indexOf(";") != -1) {
        String[] entity_parts = part.split(";");
        part_protobuf = Part.newBuilder()
        .setEntityType(entity_parts[0])
        .setAlias(entity_parts[1])
        .setText(entity_parts[2])
        .build();
      } else {
        part_protobuf = Part.newBuilder().setText(part).build();
      }
      part_protobuf_list.add(part_protobuf);
    }
    training_phrase_protobuf_builder.addAllParts(part_protobuf_list);
    return training_phrase_protobuf_builder.build();
  }
}