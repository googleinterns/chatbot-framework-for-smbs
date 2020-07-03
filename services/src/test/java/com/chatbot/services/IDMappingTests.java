package com.chatbot.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class IDMappingTests {

  private static IDMapping iDMapping;

  @BeforeClass
  public static void setUp() {
    iDMapping = new IDMapping();
    iDMapping.addNewMapping("chatClientID1", "userID1", ChatClient.HANGOUTS);
    iDMapping.addNewMapping("chatClientID2", "userID2", ChatClient.WHATSAPP);
  }

  @Test
  public void getID_IDExists() {
    assertEquals("chatClientID1",
        iDMapping.getChatClientGeneratedID("userID1", ChatClient.HANGOUTS),
        "Error getting chat client generated ID");
    assertEquals("chatClientID2", iDMapping.getChatClientGeneratedID("userID2"),
        "Error getting chat client generated ID");
    assertEquals("userID2", iDMapping.getUserID("chatClientID2", ChatClient.WHATSAPP),
        "Error getting user ID");
    assertEquals("userID1", iDMapping.getUserID("chatClientID1"), "Error getting user ID");
  }
}