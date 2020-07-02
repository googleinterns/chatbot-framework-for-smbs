package com.chatbot.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification.ChatClient;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification.Event;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PubSubControllerTests {
  private static JsonNode normalMessageNode;
  private static JsonNode noUserIDMessageNode;
  private static JsonNode unknownChatClientMessageNode;
  private static JsonNode unknownEventMessageNode;

  @BeforeClass
  public static void setUpEventObjects() throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    normalMessageNode = mapper
        .readTree(ChatServiceControllerTests.class
        .getResourceAsStream("/pubSubMessage.json"));
    noUserIDMessageNode = mapper
        .readTree(ChatServiceControllerTests.class
        .getResourceAsStream("/pubSubNoUserID.json"));
    unknownChatClientMessageNode = mapper
        .readTree(ChatServiceControllerTests.class
        .getResourceAsStream("/pubSubUnknownChatClient.json"));
    unknownEventMessageNode = mapper
        .readTree(ChatServiceControllerTests.class
        .getResourceAsStream("/pubSubUnknownEvent.json"));
  }

  @Test
  public void buildNotificationFromMessage_normalMessage() {
    TriggerEventNotification triggerEventNotification =
        PubSubController.buildNotificationFromMessage(normalMessageNode);
    assertEquals(ChatClient.HANGOUTS, triggerEventNotification.getChatClient(),
        "Error parsing chat client");
    assertEquals(Event.SUGGEST_CATEGORY_CHANGE, triggerEventNotification.getEvent(),
        "Error parsing event");
    assertEquals("123456", triggerEventNotification.getUserID(), "Error parsing userID");
    assertEquals("cafe",
        triggerEventNotification.getEventParams().getFieldsMap().get("suggestedCategory").getStringValue(),
        "Error parsing event params");
  }
  @Test(expected = IllegalArgumentException.class)
  public void buildNotificationFromMessage_noUserID() {
    PubSubController.buildNotificationFromMessage(noUserIDMessageNode);
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildNotificationFromMessage_unknownChatClient() {
    PubSubController.buildNotificationFromMessage(unknownChatClientMessageNode);
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildNotificationFromMessage_unknownEvent() {
    PubSubController.buildNotificationFromMessage(unknownEventMessageNode);
  }
}