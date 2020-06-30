package com.chatbot.services;

import com.google.cloud.dialogflow.v2.Context;
import com.google.cloud.dialogflow.v2.ContextsClient;
import com.google.cloud.dialogflow.v2.CreateContextRequest;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryParameters;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.protobuf.Struct;
import com.google.cloud.dialogflow.v2.EventInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DialogflowConversation {

  private static String projectID;
  private final String langCode;
  private final String sessionID;

  public DialogflowConversation(final String projectIDToSet, final String langCodeToSet,
      final String sessionIDToSet) {
    projectID = projectIDToSet;
    langCode = langCodeToSet;
    sessionID = sessionIDToSet;
  }

  public DialogflowConversation(final String projectIDToSet, final String sessionIDToSet) {
    this(projectIDToSet, "en", sessionIDToSet);
  }

  // function to get the response for a user message from dialogflow
  public QueryResult sendMessage(final String message, final Struct payload) throws Exception {
    try (SessionsClient sessionsClient = SessionsClient.create()) {
      final SessionName session = SessionName.of(projectID, sessionID);
      final TextInput.Builder textInput = TextInput.newBuilder().setText(message)
          .setLanguageCode(langCode);
      final QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();
      final QueryParameters queryParameters = QueryParameters.newBuilder().setPayload(payload)
          .build();
      final DetectIntentRequest detectIntentRequest =
          DetectIntentRequest.newBuilder().setSession(session.toString()).setQueryInput(queryInput)
          .setQueryParams(queryParameters).build();
      final DetectIntentResponse response = sessionsClient.detectIntent(detectIntentRequest);
      return response.getQueryResult();
    }
  }

  // function to get the response for an event from dialogflow
  public QueryResult triggerEvent(final String event, final Struct parameters, final Struct payload)
      throws Exception {
    try (SessionsClient sessionsClient = SessionsClient.create()) {
      final SessionName session = SessionName.of(projectID, sessionID);
      final EventInput.Builder eventInput = EventInput.newBuilder()
          .setName(event)
          .setParameters(parameters)
          .setLanguageCode(langCode);
      final QueryInput queryInput = QueryInput.newBuilder().setEvent(eventInput).build();
      final QueryParameters queryParameters = QueryParameters.newBuilder()
          .setPayload(payload)
          .build();
      final DetectIntentRequest detectIntentRequest = DetectIntentRequest.newBuilder()
          .setSession(session.toString())
          .setQueryInput(queryInput)
          .setQueryParams(queryParameters)
          .build();
      // before triggering an event, we would need to set the context to the input context of the
      // intent that we want to be matched
      for(final String contextName : ChatServiceConstants.EVENT_TO_CONTEXT_MAPPING.get(event)) {
        setContextForSession(contextName);
      }
      final DetectIntentResponse response = sessionsClient.detectIntent(detectIntentRequest);
      return response.getQueryResult();
    }
  }


  public void setContextForSession(final String ContextName) throws IOException {
    try (ContextsClient contextsClient = ContextsClient.create()) {
      final SessionName parent = SessionName.of(projectID, sessionID);
      final Context context = Context
          .newBuilder()
          .setName("projects/" + projectID + "/agent/sessions/" + sessionID + "/contexts/"
          + ContextName)
          .setLifespanCount(1)
          .build();
      contextsClient.createContext(CreateContextRequest.newBuilder()
          .setParent(parent.toString())
          .setContext(context).build());
    }
  }

  public List<String> getCurrentContexts() throws Exception {
    final List<String> contextList = new ArrayList<String>();
    try (ContextsClient contextsClient = ContextsClient.create()) {
      final SessionName session = SessionName.of(projectID, sessionID);
      for (final Context context : contextsClient.listContexts(session).iterateAll()) {
        // the name returned is the complete path of the context, of which we only need the name
        final String[] contextNameParts = context.getName().split("/");
        contextList.add(contextNameParts[contextNameParts.length - 1]);
      }
    }
    return contextList;
  }
}