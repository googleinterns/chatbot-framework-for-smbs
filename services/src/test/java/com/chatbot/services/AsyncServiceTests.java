package com.chatbot.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;

import com.chatbot.services.AsyncServices.HangoutsAsyncService;
import com.chatbot.services.DialogflowServices.DialogflowConversation;
import com.chatbot.services.MessageSenders.HangoutsMessageSender;
import com.chatbot.services.MessageSenders.HangoutsMessageSender.HANGOUTS_MESSAGE_TYPE;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.Attachment;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.MimeType;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.RequestType;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.Sender;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.UserMessage;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification.Event;
import com.google.cloud.dialogflow.v2.Intent;
import com.google.cloud.dialogflow.v2.QueryResult;
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
  private static HangoutsAsyncService asyncService;
  @Mock
  private static IDMapping iDMapping;
  @Mock
  private static HangoutsMessageSender hangoutsMessageSender;
  @Mock
  private static DialogflowConversation dialogflowConversation;
  @Mock
  private static HangoutsAsyncService mockAsyncService;

  private static ChatServiceRequest textMessageRequest;
  private static ChatServiceRequest addedToSpaceRequest;
  private static ChatServiceRequest attachmentMessageRequest;
  private static TriggerEventNotification triggerEventNotification;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    final Sender sender = Sender.newBuilder()
        .setChatClientGeneratedId("123456")
        .setUserId("123456")
        .build();
    final Attachment attachment = Attachment.newBuilder()
        .setLink("link")
        .setMimeType(MimeType.JPEG)
        .build();
    final UserMessage textMessage = UserMessage.newBuilder()
        .setText("Hello")
        .build();
    final UserMessage attachmentMessage = UserMessage.newBuilder()
        .addAttachments(attachment)
        .build();
    textMessageRequest = ChatServiceRequest.newBuilder()
        .setChatClient(ChatClient.HANGOUTS)
        .setRequestType(RequestType.MESSAGE)
        .setUserMessage(textMessage)
        .setSender(sender)
        .build();
    addedToSpaceRequest = ChatServiceRequest.newBuilder()
        .setChatClient(ChatClient.HANGOUTS)
        .setRequestType(RequestType.ADDED)
        .setSender(sender)
        .build();
    attachmentMessageRequest = ChatServiceRequest.newBuilder()
       .setChatClient(ChatClient.HANGOUTS)
        .setRequestType(RequestType.MESSAGE)
        .setUserMessage(attachmentMessage)
        .setSender(sender)
        .build();
    final Intent mockIntent = Intent.newBuilder().setDisplayName("ChangeCategory").build();
    final QueryResult mockQueryResult = QueryResult.newBuilder()
        .setIntent(mockIntent)
        .setFulfillmentText("")
        .build();
    triggerEventNotification = TriggerEventNotification.newBuilder()
        .setChatClient(TriggerEventNotification.ChatClient.HANGOUTS)
        .setEvent(Event.GET_CALL_FEEDBACK)
        .setEventParams(Struct.newBuilder().build())
        .setUserID("123")
        .build();
    when(iDMapping.getChatClientGeneratedID(anyString(), any(ChatClient.class))).thenReturn("123");
    when(dialogflowConversation.sendMessage(anyString(), any(Struct.class)))
        .thenReturn(mockQueryResult);
    when(dialogflowConversation.triggerEvent(anyString(), any(Struct.class), any(Struct.class)))
        .thenReturn(mockQueryResult);
    when(dialogflowConversation.getCurrentContexts()).thenReturn(new ArrayList<String>());
  }

  @Test
  public void triggerEventHandler_validTrigger() throws Exception {
    asyncService.triggerEventHandler(triggerEventNotification);
    verify(hangoutsMessageSender).sendMessageBasedOnMessageType(anyString(), anyString(),
        any(HANGOUTS_MESSAGE_TYPE.class));
  }

  @Test
  public void chatServiceRequestHandler_textMessage() throws Exception {
    asyncService.chatServiceRequestHandler(textMessageRequest);
    verify(hangoutsMessageSender).sendMessageBasedOnMessageType("123456", "",
        HANGOUTS_MESSAGE_TYPE.TEXT);
  }

  @Test
  public void chatServiceRequestHandler_attachmentMessage() throws Exception {
    asyncService.chatServiceRequestHandler(attachmentMessageRequest);
    verify(hangoutsMessageSender).sendMessageBasedOnMessageType("123456",
        ChatServiceConstants.NOT_EXPECTING_IMAGE_MESSAGE, HANGOUTS_MESSAGE_TYPE.TEXT);
  }

  @Test
  public void chatServiceRequestHandler_addedToSpaceEvent() throws Exception {
    asyncService.chatServiceRequestHandler(addedToSpaceRequest);
    verify(hangoutsMessageSender).sendMessageBasedOnMessageType(anyString(), anyString(),
        any(HANGOUTS_MESSAGE_TYPE.class));
    verify(iDMapping).addNewMapping(anyString(), anyString(), any());
  }

  @Test
  public void sendMessageUsingUserID_textMessage() throws IOException {
    asyncService.sendMessageUsingUserID("123", "foo", false);
    verify(hangoutsMessageSender).sendMessageBasedOnMessageType("123", "foo",
        HANGOUTS_MESSAGE_TYPE.TEXT);  
  }

  @Test
  public void sendMessageUsingUserID_cardMessage() throws IOException {
    asyncService.sendMessageUsingUserID("123", "foo", true);
    verify(hangoutsMessageSender).sendMessageBasedOnMessageType("123", "foo",
        HANGOUTS_MESSAGE_TYPE.CARD_WITH_HEADER);  
  }
}