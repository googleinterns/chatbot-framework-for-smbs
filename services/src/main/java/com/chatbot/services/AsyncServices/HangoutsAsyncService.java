package com.chatbot.services.AsyncServices;

import java.io.IOException;
import java.util.List;

import com.chatbot.services.ChatServiceConstants;
import com.chatbot.services.IDMapping;
import com.chatbot.services.DialogflowServices.DialogflowConversation;
import com.chatbot.services.MessageSenders.HangoutsMessageSender;
import com.chatbot.services.MessageSenders.HangoutsMessageSender.HANGOUTS_MESSAGE_TYPE;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class HangoutsAsyncService extends AsyncService {

  private HangoutsMessageSender hangoutsMessageSender;
  private IDMapping iDMapping;
  private DialogflowConversation dialogflowConversation;

  public HangoutsAsyncService(HangoutsMessageSender hangoutsMessageSenderToSet,
      IDMapping iDMappingToSet,
      DialogflowConversation dialogflowConversationToSet) {
    hangoutsMessageSender = hangoutsMessageSenderToSet;
    iDMapping = iDMappingToSet;
    dialogflowConversation = dialogflowConversationToSet;
  }

  @Override
  @Async("asyncExecutor")
  public void chatServiceRequestHandler(final ChatServiceRequest chatServiceRequest)
      throws Exception {
    final String spaceID = chatServiceRequest.getSender().getChatClientGeneratedId();
    switch (chatServiceRequest.getRequestType()) {
      case ADDED:
        hangoutsMessageSender.sendMessageBasedOnMessageType(spaceID,
            ChatServiceConstants.THANKS_FOR_ADDING_MESSAGE, HANGOUTS_MESSAGE_TYPE.TEXT);
        iDMapping.addNewMapping(chatServiceRequest.getSender().getChatClientGeneratedId(),
            chatServiceRequest.getSender().getUserId(), chatServiceRequest.getChatClient());
        break;
      case REMOVED:
        break;
      case MESSAGE:
        handleMessageEvent(chatServiceRequest);
        break;
      default:
        break;
    }
  }

  // handle requests which contain a user message
  private void handleMessageEvent(final ChatServiceRequest chatServiceRequest) throws Exception {
    final String spaceID = chatServiceRequest.getSender().getChatClientGeneratedId();
    // The spaceID of the user is used as the sessionID for hangouts
    dialogflowConversation.setSessionID(spaceID);
    if (chatServiceRequest.getUserMessage().getAttachmentsCount() == 0) {
      final Value userID = Value.newBuilder()
          .setStringValue(chatServiceRequest.getSender().getUserId()).build();
      final Struct payload = Struct.newBuilder()
          .putFields("userID", userID)
          .build();
      final QueryResult queryResult = dialogflowConversation.sendMessage(
          chatServiceRequest.getUserMessage().getText(), payload);
      final String response = queryResult.getFulfillmentText();
      if(ChatServiceConstants.LIST_OF_INTENTS_WITH_INTERACTIVE_RESPONSE
          .contains(queryResult.getIntent().getDisplayName())) {
        hangoutsMessageSender.sendMessageBasedOnMessageType(spaceID, response,
            HANGOUTS_MESSAGE_TYPE.CARD_WITH_HEADER);  
      } else {
        hangoutsMessageSender.sendMessageBasedOnMessageType(spaceID, response,
            HANGOUTS_MESSAGE_TYPE.TEXT);
      }
    } else {
      final List<String> currentContextList = dialogflowConversation.getCurrentContexts();
      if (currentContextList.contains(ChatServiceConstants.EXPECTING_IMAGES_CONTEXT)) {
        // TODO: send images to backend
        hangoutsMessageSender.sendMessageBasedOnMessageType(
            chatServiceRequest.getSender().getChatClientGeneratedId(),
            ChatServiceConstants.IMAGES_RECEIVED_MESSAGE, HANGOUTS_MESSAGE_TYPE.TEXT);
      } else {
        hangoutsMessageSender.sendMessageBasedOnMessageType(
            chatServiceRequest.getSender().getChatClientGeneratedId(),
            ChatServiceConstants.NOT_EXPECTING_IMAGE_MESSAGE, HANGOUTS_MESSAGE_TYPE.TEXT);
      }
    }
  }

  @Override
  @Async("asyncExecutor")
  public void sendMessageUsingUserID(final String userID, final String message,
      final boolean isCard) throws IOException {
    sendMessageUsingChatClientGeneratedID(
        iDMapping.getChatClientGeneratedID(userID, ChatClient.HANGOUTS), message, isCard);
  }

  @Override
  @Async("asyncExecutor")
  void sendMessageUsingChatClientGeneratedID(final String chatClientGeneratedID,
      final String message, final boolean isCard) throws IOException {
    if(isCard) {
      hangoutsMessageSender.sendMessageBasedOnMessageType(chatClientGeneratedID, message,
          HANGOUTS_MESSAGE_TYPE.CARD_WITH_HEADER);
    } else {
      hangoutsMessageSender.sendMessageBasedOnMessageType(chatClientGeneratedID, message,
          HANGOUTS_MESSAGE_TYPE.TEXT);
    }
  }

  @Override
  @Async("asyncExecutor")
  public void triggerEventHandler(final TriggerEventNotification triggerEventNotification)
      throws Exception {
    final ChatClient chatClient =
        ChatClient.valueOf(triggerEventNotification.getChatClient().name());
    final String userID = triggerEventNotification.getUserID();
    final String chatClientGeneratedID = iDMapping.getChatClientGeneratedID(userID, chatClient);
    dialogflowConversation.setSessionID(chatClientGeneratedID);
    final Value userIDValue = Value.newBuilder()
        .setStringValue(userID)
        .build();
    final Struct payload = Struct.newBuilder()
        .putFields("userID", userIDValue)
        .build();
    final QueryResult triggerResponse = dialogflowConversation
        .triggerEvent(triggerEventNotification.getEvent().name(),
        triggerEventNotification.getEventParams(), payload);
    final String response = triggerResponse.getFulfillmentText();
    if(ChatServiceConstants.LIST_OF_INTENTS_WITH_INTERACTIVE_RESPONSE
        .contains(triggerResponse.getIntent().getDisplayName())) {
      hangoutsMessageSender.sendMessageBasedOnMessageType(chatClientGeneratedID, response,
          HANGOUTS_MESSAGE_TYPE.CARD_WITH_HEADER);  
    } else {
      hangoutsMessageSender.sendMessageBasedOnMessageType(chatClientGeneratedID, response,
          HANGOUTS_MESSAGE_TYPE.TEXT);
    }
  }
}