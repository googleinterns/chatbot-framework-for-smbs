package com.chatbot.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Struct;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AsyncServiceTests {
  
  @Spy
  @InjectMocks
  private static AsyncService asyncService;
  @Mock
  private static IDMapping iDMapping;
  @Mock
  private static HangoutsMessageSender hangoutsMessageSender;
  @Mock
  private static DialogflowConversation dialogflowConversation;

  static ChatServiceRequest textMessageRequest;
  static ChatServiceRequest addedToSpaceRequest;
  static TriggerEventNotification triggerEventNotification;

  @Before
  public void initMocks() throws Exception {
    MockitoAnnotations.initMocks(this);
    textMessageRequest = ChatServiceController.buildChatServiceRequestFromHangoutsRequest(
        (new ObjectMapper())
        .readTree(AsyncServiceTests.class.getResourceAsStream("/textMessage.json")));
    addedToSpaceRequest = ChatServiceController.buildChatServiceRequestFromHangoutsRequest(
        (new ObjectMapper())
        .readTree(AsyncServiceTests.class.getResourceAsStream("/addedToSpaceEvent.json")));
    when(iDMapping.getChatClientGeneratedID(anyString(), any(ChatClient.class))).thenReturn("123");
    triggerEventNotification = TriggerEventNotification.newBuilder()
        .setChatClient(TriggerEventNotification.ChatClient.HANGOUTS)
        .setEvent(Event.GET_CALL_FEEDBACK)
        .setEventParams(Struct.newBuilder().build())
        .setUserID("123")
        .build();
  }

  @Test
  public void hangoutsAsyncHandler_textMessage() throws Exception {
    asyncService.hangoutsAsyncHandler(textMessageRequest);
    verify(asyncService).handleMessageEvent(textMessageRequest);
  }

  @Test
  public void hangoutsAsyncHandler_addedToSpaceEvent() throws Exception {
    asyncService.hangoutsAsyncHandler(addedToSpaceRequest);
    verify(hangoutsMessageSender).sendMessage(anyString(), anyString());
    verify(iDMapping).addNewMapping(anyString(), anyString(), any());
  }

  @Test
  public void sendMessageUsingUserID_testMessage() throws IOException {
    asyncService.sendMessageUsingUserID("123", "foo", ChatClient.HANGOUTS, false);
    verify(hangoutsMessageSender).sendMessage(anyString(), anyString());  
  }

  @Test
  public void sendMessageUsingUserID_cardMessage() throws IOException {
    asyncService.sendMessageUsingUserID("123", "foo", ChatClient.HANGOUTS, true);
    verify(hangoutsMessageSender).sendCardMessage(anyString(), anyString());  
  }
}