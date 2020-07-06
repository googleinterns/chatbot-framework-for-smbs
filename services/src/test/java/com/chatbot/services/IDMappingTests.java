package com.chatbot.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IDMappingTests {

  static IDMapping iDMapping;

  @Mock
  static HangoutsChatService hangoutsChatService;

  @BeforeClass
  public static void setUp() throws GeneralSecurityException, IOException {
    iDMapping = new IDMapping(hangoutsChatService);
    iDMapping.addNewMapping("chatClientID1", "userID1", ChatClient.HANGOUTS);
  }

  @Test
  public void getID_IDExists() {
    assertEquals("chatClientID1",
        iDMapping.getChatClientGeneratedID("userID1", ChatClient.HANGOUTS),
        "Error getting chat client generated ID");
    assertEquals("chatClientID1", iDMapping.getChatClientGeneratedID("userID1"),
        "Error getting chat client generated ID");
    assertEquals("userID1", iDMapping.getUserID("chatClientID1", ChatClient.HANGOUTS),
        "Error getting user ID");
    assertEquals("userID1", iDMapping.getUserID("chatClientID1"), "Error getting user ID");
  }
}