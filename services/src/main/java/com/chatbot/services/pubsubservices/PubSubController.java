package com.chatbot.services.pubsubservices;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;

import com.chatbot.services.ChatServiceConstants;
import com.chatbot.services.asyncservices.HangoutsAsyncService;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification.ChatClient;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification.Event;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Struct;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// Handle messages from cloud sub using a push subscriber
@RestController
public class PubSubController {

  // TODO: make these variables final
  private HangoutsAsyncService asyncService;
  private PubSubAuth pubSubAuth;

  public PubSubController(HangoutsAsyncService asyncService, PubSubAuth pubSubAuth) {
    this.asyncService = asyncService;
    this.pubSubAuth = pubSubAuth;
  }

  @PostMapping("/pubsub")
  String onRequest(@RequestHeader final Map<String, String> headers,
      @RequestBody final JsonNode message) throws GeneralSecurityException, IOException{
    pubSubAuth.verifyRequest(headers);
    final String messageData =
        new String(Base64.getDecoder().decode(message.at("/message/data").asText()));
    if (messageData.equals(ChatServiceConstants.TRIGGER_EVENT_MESSAGE)) {
      final TriggerEventNotification triggerEventNotification =
          buildNotificationFromMessage(message);
      asyncService.triggerEventHandler(triggerEventNotification);
    } else {
      throw new IllegalArgumentException(buildMessageTypeErrorMessage(messageData));
    }
    // empty body is just a 200 response to acknowledge message
    return "";
  }

  private TriggerEventNotification buildNotificationFromMessage(final JsonNode message)
      throws IllegalArgumentException {
    final TriggerEventNotification.Builder triggerEventNotificationBuilder = 
        TriggerEventNotification.newBuilder();
    if (message.at("/message/attributes").has("userID")) {
      triggerEventNotificationBuilder.setUserID(message.at("/message/attributes/userID").asText());
    } else {
      throw new IllegalArgumentException("No userID provided in published message");
    }
    if (message.at("/message/attributes").has("chatClient")) {
      final String chatClient = message.at("/message/attributes/chatClient").asText();
      switch (chatClient) {
        case "HANGOUTS":
          triggerEventNotificationBuilder.setChatClient(ChatClient.HANGOUTS);
          break;
        case "WHATSAPP":
          triggerEventNotificationBuilder.setChatClient(ChatClient.WHATSAPP);
          throw new IllegalArgumentException(buildChatClientErrorMessage("Whatsapp"));
        default:
          throw new IllegalArgumentException(buildChatClientErrorMessage(chatClient)); 
      }
    } else {
      throw new IllegalArgumentException("No chat client provided in published message");
    } 
 
    return parseEventParams(triggerEventNotificationBuilder, message).build();
  }

  // get the parameters of the event to be triggered and add them to the protobuf
  private TriggerEventNotification.Builder parseEventParams(
      TriggerEventNotification.Builder triggerEventNotificationBuilder, JsonNode message) {
    if (message.at("/message/attributes").has("event")) {
      final String eventName = message.at("/message/attributes/event").asText();
      switch (eventName) {
        case ChatServiceConstants.SUGGEST_CATEGORY_CHANGE_EVENT:
          final com.google.protobuf.Value suggestedCategory =
              com.google.protobuf.Value.newBuilder()
              .setStringValue(message.at("/message/attributes/suggestedCategory")
              .asText())
              .build();
          final Struct paramsForCategoryChangeEvent = Struct.newBuilder()
              .putFields("suggestedCategory", suggestedCategory)
              .build();
          triggerEventNotificationBuilder
              .setEvent(Event.SUGGEST_CATEGORY_CHANGE)
              .setEventParams(paramsForCategoryChangeEvent);
          break;
        case ChatServiceConstants.SUGGEST_IMAGE_UPLOAD_EVENT:
          triggerEventNotificationBuilder.setEvent(Event.SUGGEST_IMAGE_UPLOAD);
          break;
        case ChatServiceConstants.GET_CALL_FEEDBACK_EVENT:
          triggerEventNotificationBuilder.setEvent(Event.GET_CALL_FEEDBACK);
          final com.google.protobuf.Value mobileNumber = com.google.protobuf.Value.newBuilder()
              .setStringValue(message.at("/message/attributes/mobileNumber").asText())
              .build();
          final Struct paramsForGetFeedbackEvent = Struct.newBuilder()
              .putFields("mobileNumber", mobileNumber)
              .build();
          triggerEventNotificationBuilder
              .setEvent(Event.GET_CALL_FEEDBACK)
              .setEventParams(paramsForGetFeedbackEvent);
          break;
        default:
          throw new IllegalArgumentException("Unknown event provided in published message");
      }
    } else {
      throw new IllegalArgumentException("No event provided in published message");
    }
    return triggerEventNotificationBuilder;    
  }

  private String buildMessageTypeErrorMessage(String messageType) {
    return "Received unsupported message type: " + messageType;
  }

  private String buildChatClientErrorMessage(String chatClient) {
    return chatClient + " client provided in published message";
  }
}