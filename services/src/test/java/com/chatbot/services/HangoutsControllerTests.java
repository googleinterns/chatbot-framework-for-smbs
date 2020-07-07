package com.chatbot.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.chatbot.services.asyncservices.HangoutsAsyncService;
import com.chatbot.services.chatservicecontrollers.HangoutsAuth;
import com.chatbot.services.chatservicecontrollers.HangoutsController;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.MimeType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// tests for {@link com.chatbot.services.chatservicecontrollers.HangoutsController}
@RunWith(SpringJUnit4ClassRunner.class)
public class HangoutsControllerTests {

  @InjectMocks
  private HangoutsController hangoutsController;

  @Mock
  private HangoutsAsyncService asyncService;
  @Mock
  private HangoutsAuth hangoutsAuth;

  private MockMvc mockMvc;
  private final String uri = "/";
  private JsonNode textMessageNode;
  private JsonNode attachmentMessageNode;
  private JsonNode messageFromRoomNode;
  private JsonNode messageWithNoTypeNode;
  private JsonNode messageWithInvalidSenderNode;
  private JsonNode messageWithNoSenderInfoNode;
  private JsonNode messageWithCardClickNode;
  private MockHttpServletRequestBuilder mockRequestBuilder;

  @Before
  public void setUp() throws IOException, GeneralSecurityException {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(hangoutsController).build();
    mockRequestBuilder = MockMvcRequestBuilders.post(uri)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header("user-agent", ChatServiceConstants.HANGOUTS_USER_AGENT)
        .header("authorization", "Bearer 123");
    final ObjectMapper mapper = new ObjectMapper();
    textMessageNode = mapper
        .readTree(HangoutsControllerTests.class
        .getResourceAsStream("/textMessage.json"));
    attachmentMessageNode = mapper
        .readTree(HangoutsControllerTests.class
        .getResourceAsStream("/attachmentMessage.json"));
    messageFromRoomNode = mapper
        .readTree(HangoutsControllerTests.class
        .getResourceAsStream("/messageFromRoom.json"));
    messageWithNoTypeNode = mapper
        .readTree(HangoutsControllerTests.class
        .getResourceAsStream("/messageWithNoType.json"));
    messageWithInvalidSenderNode = mapper
        .readTree(HangoutsControllerTests.class
        .getResourceAsStream("/messageWithInvalidSender.json"));
    messageWithNoSenderInfoNode = mapper
        .readTree(HangoutsControllerTests.class
        .getResourceAsStream("/messageWithNoSenderInfo.json"));
    messageWithCardClickNode = mapper
        .readTree(HangoutsControllerTests.class
        .getResourceAsStream("/cardClickedMessage.json"));
  }

  @Test
  public void onRequest_textMessage() throws Exception {
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder
        .content(textMessageNode.toString())).andReturn();
    assertEquals(200, mvcResult.getResponse().getStatus());
    final ArgumentCaptor<ChatServiceRequest> request =
        ArgumentCaptor.forClass(ChatServiceRequest.class);
    verify(asyncService).chatServiceRequestHandler(request.capture());
    assertEquals(ChatClient.HANGOUTS, request.getValue().getChatClient(),
        "Error parsing chat client");
    assertEquals("Hello", request.getValue().getUserMessage().getText(),
        "Error parsing user message");
    assertEquals("123456", request.getValue().getSender().getChatClientGeneratedId(),
        "Error parsing chat client generated id");

  }

  @Test
  public void onRequest_attachmentMessage() throws Exception {
    mockMvc.perform(mockRequestBuilder.content(attachmentMessageNode.toString())).andReturn();
    final ArgumentCaptor<ChatServiceRequest> request =
        ArgumentCaptor.forClass(ChatServiceRequest.class);
    verify(asyncService).chatServiceRequestHandler(request.capture());
    assertEquals(1, request.getValue().getUserMessage().getAttachmentsCount(),
        "Error parsing attachements");
    assertEquals(MimeType.JPEG, request.getValue().getUserMessage().getAttachments(0).getMimeType(),
        "Error parsing mime type");
    assertEquals("donwloadUri", request.getValue().getUserMessage().getAttachments(0).getLink(),
        "Error parsing download link");
  }

  @Test
  public void onRequest_cardClick() throws Exception {
    mockMvc.perform(mockRequestBuilder.content(messageWithCardClickNode.toString())).andReturn();
    final ArgumentCaptor<ChatServiceRequest> request =
        ArgumentCaptor.forClass(ChatServiceRequest.class);
    verify(asyncService).chatServiceRequestHandler(request.capture());
    assertEquals("Receiving calls from other categories",
        request.getValue().getUserMessage().getText(), "Error parsing option clicked by user");
  }

  @Test(expected = Exception.class)
  public void onRequest_messageFromRoom() throws Exception {
    mockMvc.perform(mockRequestBuilder.content(messageFromRoomNode.toString())).andReturn();   
  }

  @Test(expected = Exception.class)
  public void onRequest_messageWithNoType() throws Exception {
    mockMvc.perform(mockRequestBuilder.content(messageWithNoTypeNode.toString())).andReturn();   
  }

  @Test(expected = Exception.class)
  public void onRequest_messageWithNoSender() throws Exception {
    mockMvc.perform(mockRequestBuilder.content(messageWithNoSenderInfoNode.toString())).andReturn();   
  }

  @Test(expected = Exception.class)
  public void onRequest_messageWithInvalidSenderInfo() throws Exception {
    mockMvc.perform(mockRequestBuilder.content(messageWithInvalidSenderNode.toString())).andReturn();   
  }
}