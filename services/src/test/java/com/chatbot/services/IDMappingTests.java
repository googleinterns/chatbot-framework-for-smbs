package com.chatbot.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

// tests for {@link com.chatbot.services.IDMapping}
@RunWith(MockitoJUnitRunner.class)
public class IDMappingTests {

  @InjectMocks
  static IDMapping iDMapping;

  @Mock
  static HangoutsChatService hangoutsChatService;

  @Before
  public void setUp() throws GeneralSecurityException, IOException {
    MockitoAnnotations.initMocks(this);
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