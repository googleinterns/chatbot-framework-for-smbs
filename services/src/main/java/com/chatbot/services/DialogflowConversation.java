package com.chatbot.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.cloud.dialogflow.v2.Context;
import com.google.cloud.dialogflow.v2.ContextsClient;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.EventInput;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryParameters;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.protobuf.Struct;

public class DialogflowConversation {

  private String projectID;
  private String langCode;
  private String sessionID;

  public DialogflowConversation(String projectID, String langCode, String sessionID) {
    this.projectID = projectID;
    this.langCode = langCode;
    this.sessionID = sessionID;
  }

  public DialogflowConversation(String projectID, String sessionID) {
    this.projectID = projectID;
    this.langCode = "en";
    this.sessionID = sessionID;
  }

  // function to get the response for a user message from dialogflow
  public String sendMessage(String message, Struct payload) throws IOException {
    try (SessionsClient sessionsClient = SessionsClient.create()) {
      SessionName session = SessionName.of(this.projectID, this.sessionID);
      TextInput.Builder textInput = TextInput.newBuilder()
          .setText(message).setLanguageCode(this.langCode);
      QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();
      QueryParameters queryParameters = QueryParameters.newBuilder().setPayload(payload).build();
      DetectIntentRequest detectIntentRequest = DetectIntentRequest.newBuilder()
          .setSession(session.toString()).setQueryInput(queryInput).setQueryParams(queryParameters)
          .build();
      DetectIntentResponse response = sessionsClient.detectIntent(detectIntentRequest);
      QueryResult queryResult = response.getQueryResult();
      return queryResult.getFulfillmentText();
    }
  }

  // function to get the response for an event from dialogflow
  public String triggerEvent(String event, Struct parameters, Struct payload) throws IOException {
    try (SessionsClient sessionsClient = SessionsClient.create()) {
      SessionName session = SessionName.of(this.projectID, this.sessionID);
      EventInput.Builder eventInput = EventInput.newBuilder().setName(event)
          .setParameters(parameters).setLanguageCode(this.langCode);
      QueryInput queryInput = QueryInput.newBuilder().setEvent(eventInput).build();
      QueryParameters queryParameters = QueryParameters.newBuilder().setPayload(payload)
          .build();
      DetectIntentRequest detectIntentRequest = DetectIntentRequest.newBuilder()
          .setSession(session.toString()).setQueryInput(queryInput).setQueryParams(queryParameters)
          .build();
      DetectIntentResponse response = sessionsClient.detectIntent(detectIntentRequest);
      QueryResult queryResult = response.getQueryResult();
      return queryResult.getFulfillmentText();
    }
  }

  public List<String> getCurrentContexts() throws Exception {
    List<String> contextList = new ArrayList<String>();
    try (ContextsClient contextsClient = ContextsClient.create()) {
      SessionName session = SessionName.of(this.projectID, this.sessionID);
      for (Context context : contextsClient.listContexts(session).iterateAll()) {
        // the name returned is the complete path of the context, of which we only need the name
        String[] contextNameParts = context.getName().split("/");
        contextList.add(contextNameParts[contextNameParts.length - 1]);
      }
    }
    return contextList;
  }
}