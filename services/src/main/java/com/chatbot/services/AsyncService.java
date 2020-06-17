package com.chatbot.services;

import java.io.IOException;
import java.util.List;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncService {

  @Autowired
  private HangoutsMessageSender hangoutsMessageSender;
  @Autowired
  private IDMapping iDMapping;
  private static final String IMAGES_RECEIVED_MESSAGE = "The images have been received!";
  private static final String THANKS_FOR_ADDING_MESSAGE = "Thank You for Adding me";
  private static final String NOT_EXPECTING_IMAGE_MESSAGE =
      "Sorry, we were not expecting any attachements from you.";
  private static final String EXPECTING_IMAGES_CONTEXT = "ExpectingImagesContext";

  @Async("asyncExecutor")
  public void hangoutsAsyncHandler(final ChatServiceRequest chatServiceRequest) throws Exception {
    final String spaceID = chatServiceRequest.getSender().getChatClientGeneratedId();
    switch (chatServiceRequest.getRequestType()) {
      case ADDED:
        hangoutsMessageSender.sendMessage(spaceID, THANKS_FOR_ADDING_MESSAGE);
        iDMapping.addNewMapping(chatServiceRequest.getSender().getChatClientGeneratedId(),
            chatServiceRequest.getSender().getUserId(), chatServiceRequest.getChatClient());
        break;
      case REMOVED:
        break;
      case MESSAGE:
        // The spaceID of the user is used as the sessionID for hangouts
        final DialogflowConversation dialogflowConversation = new DialogflowConversation(System.getenv("projectID"),
            spaceID);
        if (chatServiceRequest.getUserMessage().getAttachmentsCount() == 0) {
          final Value userID = Value.newBuilder().setStringValue(chatServiceRequest.getSender().getUserId()).build();
          final Struct payload = Struct.newBuilder().putFields("userID", userID).build();
          final String response = dialogflowConversation.sendMessage(chatServiceRequest.getUserMessage().getText(),
              payload);
          hangoutsMessageSender.sendMessage(spaceID, response);
        } else {
          final List<String> currentContextList = dialogflowConversation.getCurrentContexts();
          if (currentContextList.contains(EXPECTING_IMAGES_CONTEXT)) {
            // send images to backend
            hangoutsMessageSender.sendMessage(chatServiceRequest.getSender().getChatClientGeneratedId(),
                IMAGES_RECEIVED_MESSAGE);
          } else {
            hangoutsMessageSender.sendMessage(chatServiceRequest.getSender().getChatClientGeneratedId(),
                NOT_EXPECTING_IMAGE_MESSAGE);
          }
        }
        break;
      default:
        break;
    }
  }

  @Async("asyncExecutor")
  public void sendMessageUsingUserID(final String userID, final String message, final ChatClient chatClient)
      throws IOException {
    switch (chatClient) {
      case HANGOUTS:
        hangoutsMessageSender.sendMessage(iDMapping.getChatClientGeneratedID(userID, ChatClient.HANGOUTS), message);
        break;
      case WHATSAPP:
        hangoutsMessageSender.sendMessage(iDMapping.getChatClientGeneratedID(userID, ChatClient.WHATSAPP), message);
        break;
      default:
        throw new IllegalArgumentException("Unknown chat client found");
    }
  }

  @Async
  public void sendMessageUsingChatClientGeneratedID(final String chatClientGeneratedID, final String message,
      final ChatClient chatClient) throws IOException {
    switch (chatClient) {
      case HANGOUTS:
        hangoutsMessageSender.sendMessage(chatClientGeneratedID, message);
        break;
      case WHATSAPP:
        // send whatsapp message
      default:
        throw new IllegalArgumentException("Unknown chat client found");
    }
  }

  @Async
  public void triggerEventHandler(final TriggerEventNotification triggerEventNotification) throws IOException {
    final ChatClient chatClient = ChatClient.valueOf(triggerEventNotification.getChatClient().name());
    final String userID = triggerEventNotification.getUserID();
    final String chatClientGeneratedID = iDMapping.getChatClientGeneratedID(userID, chatClient);
    final DialogflowConversation dialogflowConversation = new DialogflowConversation(System.getenv("projectID"),
        chatClientGeneratedID);
    final Value userIDValue = Value.newBuilder().setStringValue(userID).build();
    final Struct payload = Struct.newBuilder().putFields("userID", userIDValue).build();
    final String triggerResponse = dialogflowConversation.triggerEvent(triggerEventNotification.getEvent().name(),
        triggerEventNotification.getEventParams(), payload);
    sendMessageUsingChatClientGeneratedID(chatClientGeneratedID, triggerResponse, chatClient);
  }

  @Async("asyncExecutor")
  public void whatsappAsyncHandler(final ChatServiceRequest chatServiceRequest) {
  }

}