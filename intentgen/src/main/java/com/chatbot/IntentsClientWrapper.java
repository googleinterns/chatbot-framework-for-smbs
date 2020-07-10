package com.chatbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.cloud.dialogflow.v2.Intent;
import com.google.cloud.dialogflow.v2.IntentsClient;
import com.google.cloud.dialogflow.v2.ProjectAgentName;

// Wrapper class for IntentsClient
// TODO: use mockito inline extenstion to mock for testing instead of using the wrapper class
public class IntentsClientWrapper {

  private IntentsClient intentsClient;

  public IntentsClientWrapper() throws IOException {
    intentsClient = IntentsClient.create();
  }

  public List<Intent> getIntentsList(ProjectAgentName parent) throws IOException {
    List<Intent> intentsList = new ArrayList<>();
    intentsClient.listIntents(parent).iterateAll().forEach(intent->intentsList.add(intent));
    return intentsList;
  }

  public void updateIntent(Intent intent, String languageCode) {
    intentsClient.updateIntent(intent, languageCode);
  }

  public void createIntent(ProjectAgentName parent, Intent intent) {
    intentsClient.createIntent(parent, intent);
  }

  public void finalize() {
    intentsClient.close();
  }

}