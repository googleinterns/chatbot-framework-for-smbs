package com.chatbot.services.MessageSenders;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.chat.v1.HangoutsChat;
import com.google.api.services.chat.v1.model.ActionParameter;
import com.google.api.services.chat.v1.model.Button;
import com.google.api.services.chat.v1.model.Card;
import com.google.api.services.chat.v1.model.FormAction;
import com.google.api.services.chat.v1.model.Message;
import com.google.api.services.chat.v1.model.OnClick;
import com.google.api.services.chat.v1.model.Section;
import com.google.api.services.chat.v1.model.TextButton;
import com.google.api.services.chat.v1.model.WidgetMarkup;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// class to handle sending of messages to hangouts users

@Component
public class HangoutsMessageSender extends MessageSender{
  private static String CHAT_SCOPE;
  private static GoogleCredentials credentials;
  private static HttpRequestInitializer requestInitializer;
  private static HangoutsChat chatService;

  HangoutsMessageSender(@Value("${hangoutsAPIScope}") final String apiScope,
      @Value("${credentialsFile}") final String credentialsFile) throws GeneralSecurityException,
      IOException {
    CHAT_SCOPE = apiScope;
    credentials = GoogleCredentials.fromStream(
        HangoutsMessageSender.class.getResourceAsStream(credentialsFile))
        .createScoped(CHAT_SCOPE);
    requestInitializer = new HttpCredentialsAdapter(credentials);
    chatService = new HangoutsChat.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        JacksonFactory.getDefaultInstance(), requestInitializer)
        .setApplicationName("chatbot").build();
  }

  // send message to a user using their chatClientGeneratedID (spaceID for hangouts)
  @Override
  public void sendMessage(final String chatClientGeneratedID, final String msg) throws IOException {
    if(msg.isEmpty()) {
      return;
    }
    final Message message = new Message().setText(msg);
    chatService.spaces()
        .messages()
        .create("spaces/" + chatClientGeneratedID, message)
        .execute();
  }

  // send an interactive card message to a user using their chatClientGeneratedID
  public void sendCardMessage(final String chatClientGeneratedID, final String msg)
      throws IOException {
    // responses to be sent as cards are newline seperated, the first line is the description and
    // the following lines are options to be shown
    final List<String> messageParts = new ArrayList<>(Arrays.asList(msg.split("\n")));
    final List<Section> sectionList = new ArrayList<>();
    for (final String option : messageParts.subList(1, messageParts.size())) {
      final List<WidgetMarkup> widgets = new ArrayList<>();
      final List<ActionParameter> customParameters = Collections
          .singletonList(new ActionParameter()
          .setKey("message")
          .setValue(option));
      final FormAction action = new FormAction()
          .setActionMethodName("INTERACTIVE_TEXT_BUTTON_ACTION")
          .setParameters(customParameters);
      final OnClick onClick = new OnClick().setAction(action);
      final TextButton button = new TextButton()
          .setText(option)
          .setOnClick(onClick);
      final Button widget = new Button().setTextButton(button);
      widgets.add(new WidgetMarkup().setButtons(Collections.singletonList((widget))));
      final Section section = new Section().setWidgets(widgets);
      sectionList.add(section);
    }
    final Card card =
        (new Card()).setSections(Collections.unmodifiableList(sectionList));
    final Message message = new Message().setCards(Collections.singletonList(card));
    // the description is sent first followed by the options
    sendMessage(chatClientGeneratedID, messageParts.get(0));
    chatService.spaces()
        .messages()
        .create("spaces/" + chatClientGeneratedID, message)
        .execute();
  }
}