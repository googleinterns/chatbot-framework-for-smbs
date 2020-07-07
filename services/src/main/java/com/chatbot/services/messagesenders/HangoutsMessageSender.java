package com.chatbot.services.messagesenders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.chatbot.services.HangoutsChatService;
import com.google.api.services.chat.v1.model.ActionParameter;
import com.google.api.services.chat.v1.model.Button;
import com.google.api.services.chat.v1.model.Card;
import com.google.api.services.chat.v1.model.FormAction;
import com.google.api.services.chat.v1.model.Message;
import com.google.api.services.chat.v1.model.OnClick;
import com.google.api.services.chat.v1.model.Section;
import com.google.api.services.chat.v1.model.TextButton;
import com.google.api.services.chat.v1.model.WidgetMarkup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// This class handles the creation and sending of messages to Hangouts users

@Component
public class HangoutsMessageSender extends MessageSender {

  private static final Logger logger = LoggerFactory.getLogger(HangoutsMessageSender.class);

  HangoutsChatService hangoutsChatService;

  public HangoutsMessageSender(final HangoutsChatService hangoutsChatServiceToSet) {
    hangoutsChatService = hangoutsChatServiceToSet;
  }

  public enum HANGOUTS_MESSAGE_TYPE {
    TEXT,
    CARD,
    CARD_WITH_HEADER
  }

  // send message to a user using their chatClientGeneratedID (spaceID for hangouts)
  @Override
  void sendMessage(final String chatClientGeneratedID, final String msg) throws IOException {
    if(msg.isEmpty()) {
      return;
    }
    sendMessage(chatClientGeneratedID, (new Message().setText(msg)));
  }

  private void sendMessage(final String chatClientGeneratedID, final Message message)
      throws IOException {
    hangoutsChatService.getChatService().spaces()
        .messages()
        .create("spaces/" + chatClientGeneratedID, message)
        .execute();
  }

  // generate the list of messages to send based on the type of the message
  private List<Message> generateMessageListByType(final String msg,
      final HANGOUTS_MESSAGE_TYPE messageType) {
    final List<Message> messageList = new ArrayList<>();
    switch (messageType) {
      case TEXT:
        if(!msg.isEmpty()) {
          messageList.add((new Message().setText(msg)));
        }
        break;
      case CARD:
        messageList.add(generateCardMessage(Arrays.asList(msg.split("\n"))));
        break;
      case CARD_WITH_HEADER:
        final List<String> messageParts = Arrays.asList(msg.split("\n"));
        messageList.add((new Message().setText(messageParts.get(0))));
        messageList.add(generateCardMessage(messageParts.subList(1, messageParts.size())));
    }
    return messageList;
  }

  // send the message(s) to the user
  public void sendMessageBasedOnMessageType(final String chatClientGeneratedID, final String msg,
      final HANGOUTS_MESSAGE_TYPE messageType) {
    generateMessageListByType(msg, messageType).forEach(message-> {
      try {
        sendMessage(chatClientGeneratedID, message);
      } catch (final IOException e) {
        logger.error("error sending hangouts message", e);
      }
    });
  }
  // create an interactive card message with given list as options
  private Message generateCardMessage(final List<String> options) {
    final List<Section> sectionList = new ArrayList<>();
    for (final String option : options) {
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
    return (new Message().setCards(Collections.singletonList(card)));
  }

}