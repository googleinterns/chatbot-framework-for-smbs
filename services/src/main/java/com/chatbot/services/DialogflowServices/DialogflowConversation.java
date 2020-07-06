package com.chatbot.services.DialogflowServices;

import com.chatbot.services.ChatServiceConstants;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import com.google.cloud.dialogflow.v2.EventInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DialogflowConversation {

  private static final Logger logger = LoggerFactory.getLogger(DialogflowConversation.class);
  private static String projectID;
  private String langCode;
  private String sessionID;

  DialogflowConversation(@Value("${languageCode}") final String langCodeToSet) {
    projectID = System.getenv("projectID");
    langCode = langCodeToSet;
  }

  public void setSessionID(final String sessionIDToSet) {
    sessionID = sessionIDToSet;
  }
 
  public void setLanguageCode(final String langCodeToSet) {
    langCode = langCodeToSet;
  }

  // function to send the user message to dialogflow 
  public QueryResult sendMessage(final String message, final Struct payload) throws Exception {
    try (SessionsClient sessionsClient = SessionsClient.create()) {
      final SessionName session = SessionName.of(projectID, sessionID);
      final TextInput.Builder textInput = TextInput.newBuilder().setText(message)
          .setLanguageCode(langCode);
      final QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();
      final QueryParameters queryParameters = QueryParameters.newBuilder().setPayload(payload)
          .build();
      final DetectIntentResponse response = sessionsClient.detectIntent(
          buildDetectIntentRequest(session, queryInput, queryParameters));
      return response.getQueryResult();
    }
  }

  // function to trigger an event in dialogflow
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
      // before triggering an event, we would need to set the context to the input context of the
      // intent that we want to be matched
      ChatServiceConstants.EVENT_TO_CONTEXT_MAPPING.get(event)
          .forEach(contextName -> {
            try {
              setContextForSession(contextName);
            } catch (final IOException e) {
              logger.error("Error while setting contexts for session", e);
            }
          });
      final DetectIntentResponse response = sessionsClient.detectIntent(
          buildDetectIntentRequest(session, queryInput, queryParameters));
      return response.getQueryResult();
    }
  }

  // function activate given context for the session
  private void setContextForSession(final String ContextName) throws IOException {
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

  // function to get the current active contexts for the session
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

  // function to build the detect intent request
  private DetectIntentRequest buildDetectIntentRequest(SessionName session, QueryInput queryInput,
      QueryParameters queryParameters) {
    return DetectIntentRequest.newBuilder()
        .setSession(session.toString())
        .setQueryInput(queryInput)
        .setQueryParams(queryParameters)
        .build();
  }
}