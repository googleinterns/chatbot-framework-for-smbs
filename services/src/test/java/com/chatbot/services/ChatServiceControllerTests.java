package com.chatbot.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ChatServiceControllerTests {


    static JsonNode textMessageNode;
    static JsonNode attachmentMessageNode;
    static JsonNode messageFromRoomNode;
    static JsonNode messageWithNoTypeNode;
    static JsonNode messageWithInvalidSenderNode;
    static JsonNode messageWithNoSenderInfoNode;
    static JsonNode messageWithCardClickNode;
    static JsonNode messageWithInvalidCardClickNode;

    @BeforeClass
    public static void setUpEventObjects() throws IOException {
      final ObjectMapper mapper = new ObjectMapper();
      textMessageNode = mapper
          .readTree(ChatServiceControllerTests.class
          .getResourceAsStream("/textMessage.json"));
      attachmentMessageNode = mapper
          .readTree(ChatServiceControllerTests.class
          .getResourceAsStream("/attachmentMessage.json"));
      messageFromRoomNode = mapper
          .readTree(ChatServiceControllerTests.class
          .getResourceAsStream("/messageFromRoom.json"));
      messageWithNoTypeNode = mapper
          .readTree(ChatServiceControllerTests.class
          .getResourceAsStream("/messageWithNoType.json"));
      messageWithInvalidSenderNode = mapper
          .readTree(ChatServiceControllerTests.class
          .getResourceAsStream("/messageWithInvalidSender.json"));
      messageWithNoSenderInfoNode = mapper
          .readTree(ChatServiceControllerTests.class
          .getResourceAsStream("/messageWithNoSenderInfo.json"));
      messageWithCardClickNode = mapper
          .readTree(ChatServiceControllerTests.class
          .getResourceAsStream("/cardClickedMessage.json"));
      messageWithInvalidCardClickNode = mapper
          .readTree(ChatServiceControllerTests.class
          .getResourceAsStream("/invalidCardClickedMessage.json"));
    }

    @Test
    public void buildChatServiceRequestFromHangoutsRequest_textMessage() throws Exception {
      final ChatServiceRequest textMessageRequest =
          ChatServiceController.buildChatServiceRequestFromHangoutsRequest(textMessageNode);
      assertEquals("Hello", textMessageRequest.getUserMessage().getText(), "message parse error");
      assertEquals("123546", textMessageRequest.getSender().getChatClientGeneratedId(),
          "chat client ID parse error");
      assertEquals("78910", textMessageRequest.getSender().getUserId(), "user ID parse error");
      assertEquals(ChatServiceRequest.RequestType.MESSAGE.toString(),
          textMessageRequest.getRequestType().toString(), "request type parse error");
      assertEquals(ChatServiceRequest.ChatClient.HANGOUTS.toString(),
          textMessageRequest.getChatClient().toString(), "chat client parse error");
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildChatServiceRequestFromHangoutsRequest_messageFromRoom() throws Exception {
      ChatServiceController.buildChatServiceRequestFromHangoutsRequest(messageFromRoomNode);
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildChatServiceRequestFromHangoutsRequest_messageWithNoType() throws Exception {
      ChatServiceController.buildChatServiceRequestFromHangoutsRequest(messageWithNoTypeNode);
    }

    @Test
    public void parseHangoutsSender_validSender() {
      final ChatServiceRequest.Builder testBuilder = ChatServiceRequest.newBuilder();
      ChatServiceController.parseHangoutsSender(testBuilder, textMessageNode);
      assertEquals("123546", testBuilder.getSender().getChatClientGeneratedId(),
          "Chat client ID parse error");
      assertEquals("Foo", testBuilder.getSender().getDisplayName(), "Display name parse error");
      assertEquals("78910", testBuilder.getSender().getUserId(), "User ID parse error");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void parseHangoutsSender_invalidID() {
      final ChatServiceRequest.Builder testBuilder = ChatServiceRequest.newBuilder();
      ChatServiceController.parseHangoutsSender(testBuilder, messageWithInvalidSenderNode);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseHangoutsSender_noSenderInfo() {
      ChatServiceController.parseHangoutsSender(ChatServiceRequest.newBuilder(),
          messageWithNoSenderInfoNode);
    }

    @Test
    public void parseHangoutsCardClick_validClick() {
      final ChatServiceRequest.Builder testBuilder = ChatServiceRequest.newBuilder();
      ChatServiceController.parseHangoutsCardClick(testBuilder, messageWithCardClickNode);
      assertEquals("Receiving calls from other categories", testBuilder.getUserMessage().getText(),
          "Card option value parsing error");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseHangoutsCardClick_invalidClick() {
      ChatServiceController.parseHangoutsCardClick(ChatServiceRequest.newBuilder(),
          messageWithInvalidCardClickNode);
    }

    @Test
    public void parseHangoutsUserMessage_withAttachment() {
      final ChatServiceRequest.Builder testBuilder = ChatServiceRequest.newBuilder();
      ChatServiceController.parseHangoutsUserMessage(testBuilder, attachmentMessageNode);
      assertEquals("donwloadUri", testBuilder.getUserMessage().getAttachments(0).getLink(),
          "Error parsing image download url");
      assertEquals(ChatServiceRequest.MimeType.JPEG.toString(),
          testBuilder.getUserMessage().getAttachments(0).getMimeType().toString(),
          "Error parsing image mime type");
    }

}