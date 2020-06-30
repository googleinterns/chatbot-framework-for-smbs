package com.chatbot.services;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification.ChatClient;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification.Event;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.protobuf.Struct;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PubSubController {

  @Autowired
  private AsyncService asyncService;
  private static final Logger logger = LoggerFactory.getLogger(PubSubController.class);
  private static final GoogleIdTokenVerifier verifier =
      new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
      new JacksonFactory()).setAudience(Collections.singletonList(System.getenv("pubsubAudience")))
      .build();

  @PostMapping("/pubsub")
  public String onRequest(@RequestHeader final Map<String, String> headers,
      @RequestBody final JsonNode message) throws Exception {
    final String authorizationHeader = headers.get("authorization");
    // authorization header format: `authorization <token>`
    if (authorizationHeader == null || authorizationHeader.isEmpty()
        || authorizationHeader.split(" ").length != 2) {
      throw new IllegalArgumentException("Bad Request");
    }
    if(verifier.verify(authorizationHeader.split(" ")[1]) == null) {
      throw new IllegalArgumentException("Invalid ID token in request");
    }
    final String messageData =
        new String(Base64.getDecoder().decode(message.at("/message/data").asText()));
    if (messageData.equals(ChatServiceConstants.TRIGGER_EVENT_MESSAGE)) {
      final TriggerEventNotification triggerEventNotification =
          buildNotificationFromMessage(message);
      try {
        asyncService.triggerEventHandler(triggerEventNotification);
      } catch (final IOException e) {
        logger.error("Error while handling event trigger", e);
      }
    } else {
      throw new IllegalArgumentException("Unknown type of message received by subscriber");
    }
    return "";
  }

  private TriggerEventNotification buildNotificationFromMessage(final JsonNode message)
      throws IllegalArgumentException {
    final TriggerEventNotification.Builder triggerEventNotificationBuilder = 
        TriggerEventNotification.newBuilder();
    if(message.at("/message/attributes").has("userID")) {
      triggerEventNotificationBuilder.setUserID(message.at("/message/attributes/userID").asText());
    } else {
      throw new IllegalArgumentException("No userID provided in published message");
    }
    if(message.at("/message/attributes").has("chatClient")) {
      final String chatClient = message.at("/message/attributes/chatClient").asText();
      switch (chatClient) {
        case "HANGOUTS":
          triggerEventNotificationBuilder.setChatClient(ChatClient.HANGOUTS);
          break;
        case "WHATSAPP":
          triggerEventNotificationBuilder.setChatClient(ChatClient.WHATSAPP);
          break;
        default:
          throw new IllegalArgumentException("Unknown client provided in published message"); 
      }
    } else {
      throw new IllegalArgumentException("No chat client provided in published message");
    } 
    if(message.at("/message/attributes").has("event")) {
      final String eventName = message.at("/message/attributes/event").asText();
      switch (eventName) {
        case ChatServiceConstants.SUGGEST_CATEGORY_CHANGE_EVENT:
          final com.google.protobuf.Value suggestedCategory = com.google.protobuf.Value.newBuilder()
          .setStringValue(message.at("/message/attributes/suggestedCategory").asText()).build();
          final Struct paramsForCategoryChangeEvent = Struct.newBuilder()
              .putFields("suggestedCategory", suggestedCategory).build();
          triggerEventNotificationBuilder
              .setEvent(Event.SUGGEST_CATEGORY_CHANGE).setEventParams(paramsForCategoryChangeEvent);
          break;
        case ChatServiceConstants.SUGGEST_IMAGE_UPLOAD_EVENT:
          triggerEventNotificationBuilder.setEvent(Event.SUGGEST_IMAGE_UPLOAD);
          break;
        case ChatServiceConstants.GET_CALL_FEEDBACK_EVENT:
          triggerEventNotificationBuilder.setEvent(Event.GET_CALL_FEEDBACK);
          final com.google.protobuf.Value mobileNumber = com.google.protobuf.Value.newBuilder()
              .setStringValue(message.at("/message/attributes/mobileNumber").asText()).build();
          final Struct paramsForGetFeedbackEvent = Struct.newBuilder()
              .putFields("mobileNumber", mobileNumber).build();
          triggerEventNotificationBuilder
              .setEvent(Event.GET_CALL_FEEDBACK).setEventParams(paramsForGetFeedbackEvent);
          break;
        default:
          throw new IllegalArgumentException("Unknown event provided in published message");
      }
    } else {
      throw new IllegalArgumentException("No event provided in published message");
    }    
    return triggerEventNotificationBuilder.build();
  }
}