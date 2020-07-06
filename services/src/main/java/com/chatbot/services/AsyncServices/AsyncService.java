package com.chatbot.services.AsyncServices;

import java.io.IOException;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification;

abstract class AsyncService {

  protected static final String IMAGES_RECEIVED_MESSAGE = "The images have been received!";
  protected static final String THANKS_FOR_ADDING_MESSAGE = "Thank You for Adding me";
  protected static final String NOT_EXPECTING_IMAGE_MESSAGE =
      "Sorry, we were not expecting any attachements from you.";

  abstract void chatServiceRequestHandler(final ChatServiceRequest chatServiceRequest) throws Exception;
  abstract void sendMessageUsingUserID(final String userID, final String message, final boolean isCard) throws IOException; 
  abstract void sendMessageUsingChatClientGeneratedID(final String chatClientGeneratedID,
      final String message, final boolean isCard) throws IOException;
  abstract void triggerEventHandler(final TriggerEventNotification triggerEventNotification)
      throws Exception;

}