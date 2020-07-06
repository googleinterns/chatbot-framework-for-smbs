package com.chatbot.services.AsyncServices;

import java.io.IOException;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification;

// Class to asynchronously handle responding to messages from users and pubsub

abstract class AsyncService {

  // method to handle a user message
  abstract void chatServiceRequestHandler(final ChatServiceRequest chatServiceRequest) throws Exception;
  // method to send messages to a user using their userID 
  abstract void sendMessageUsingUserID(final String userID, final String message, final boolean isCard) throws IOException; 
  // method to send messages to a user using the ID generated for them by the chat client being used
  abstract void sendMessageUsingChatClientGeneratedID(final String chatClientGeneratedID,
      final String message, final boolean isCard) throws IOException;
  // method to handle an event trigger for a user
  abstract void triggerEventHandler(final TriggerEventNotification triggerEventNotification)
      throws Exception;

}