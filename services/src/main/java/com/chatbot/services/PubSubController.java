package com.chatbot.services;

import java.io.IOException;
import java.security.GeneralSecurityException;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PubSubController {

  @Autowired
  private static AsyncService asyncService;
  private static final String TRIGGER_EVENT_MESSAGE = "TriggerEvent";
  private static final String SUGGEST_CATEGORY_CHANGE_EVENT = "SUGGEST_CATEGORY_CHANGE";
  private static final String SUGGEST_IMAGE_UPLOAD_EVENT = "SUGGEST_IMAGE_UPLOAD";
  private static final GoogleIdTokenVerifier verifier =
      new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
      new JacksonFactory()).setAudience(Collections.singletonList(System.getenv("pubsubAudience")))
      .build();

  @PostMapping("/pubsub")
  public String onRequest(@RequestHeader final Map<String, String> headers,
      @RequestBody final JsonNode message) throws GeneralSecurityException, IOException {
    final String authorizationHeader = headers.get("authorization");
    if (authorizationHeader == null || authorizationHeader.isEmpty()
        || authorizationHeader.split(" ").length != 2) {
      throw new IllegalArgumentException("Bad Request");
    }
    System.out.println(authorizationHeader.split(" ")[1]);
    if(verifier.verify(authorizationHeader.split(" ")[1]) == null) {
      throw new IllegalArgumentException("Invalid ID token in request");
    }
    final String messageData =
        new String(Base64.getDecoder().decode(message.at("/message/data").asText()));
    if (messageData.equals(TRIGGER_EVENT_MESSAGE)) {
      final TriggerEventNotification triggerEventNotification =
          buildNotificationFromMessage(message);
      try {
        asyncService.triggerEventHandler(triggerEventNotification);
      } catch (final IOException e) {
        e.printStackTrace();
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
        case SUGGEST_CATEGORY_CHANGE_EVENT:
          final com.google.protobuf.Value suggestedCategory = com.google.protobuf.Value.newBuilder()
          .setStringValue(message.at("/message/attributes/suggestedCategory").asText()).build();
          final Struct eventParams = Struct.newBuilder()
              .putFields("suggestedCategory", suggestedCategory).build();
          triggerEventNotificationBuilder
              .setEvent(Event.SUGGEST_CATEGORY_CHANGE).setEventParams(eventParams);
          break;
        case SUGGEST_IMAGE_UPLOAD_EVENT:
          triggerEventNotificationBuilder.setEvent(Event.SUGGEST_IMAGE_UPLOAD);
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