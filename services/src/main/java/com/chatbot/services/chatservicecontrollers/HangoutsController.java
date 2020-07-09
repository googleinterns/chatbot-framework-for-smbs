package com.chatbot.services.chatservicecontrollers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.services.chat.v1.model.ActionResponse;
import com.google.api.services.chat.v1.model.Card;
import com.google.api.services.chat.v1.model.Message;
import com.google.api.services.chat.v1.model.Section;
import com.google.api.services.chat.v1.model.TextParagraph;
import com.google.api.services.chat.v1.model.WidgetMarkup;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.chatbot.services.ChatServiceConstants;
import com.chatbot.services.IDMapping;
import com.chatbot.services.asyncservices.HangoutsAsyncService;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.MimeType;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.RequestType;

// Receive messages from hangouts and dispatch response calls
@RestController
public class HangoutsController extends ChatServiceController {

  private HangoutsAsyncService asyncService;
  private HangoutsAuth hangoutsAuth;
  
  private static final String REMOVED_FROM_SPACE_EVENT = "REMOVED_FROM_SPACE";
  private static final String ADDED_TO_SPACE_EVENT = "ADDED_TO_SPACE";
  private static final String MESSAGE_EVENT = "MESSAGE";
  private static final String CARD_CLICKED_EVENT = "CARD_CLICKED";

  public HangoutsController(HangoutsAsyncService asyncServiceToSet,
      HangoutsAuth hangoutsAuthToSet) {
    asyncService = asyncServiceToSet;
    hangoutsAuth = hangoutsAuthToSet;
  }

  @Override
  @PostMapping("/")
  Message onRequest(@RequestHeader final Map<String, String> headers,
      @RequestBody final JsonNode event) throws Exception {
    if (headers.get("user-agent").equals(ChatServiceConstants.HANGOUTS_USER_AGENT)) {
      hangoutsAuth.verifyRequest(headers);
      asyncService.chatServiceRequestHandler(buildChatServiceRequestFromHTTPRequest(event));
    } else {
      throw new IllegalArgumentException("Unknown user agent");
    }
    final Message reply = new Message();
    if (!event.at("/type").asText().equals(CARD_CLICKED_EVENT)) {
      // an empty message acts as an acknowledgement for a text message
      reply.setText("");
    } else {
      // replace the button with a text paragraph to render it unclickable
      reply.setActionResponse((new ActionResponse()).set("type", "UPDATE_MESSAGE"));
      // the selected option will be present as the first element in parameters field
      reply.setCards(Collections
          .singletonList(buildTextParagraphCard(event.at("/action/parameters/0/value").asText())));
    }
    return reply;
  }

  // build the chatServiceRequest protobuf for a HTTP request from the chat client
  ChatServiceRequest buildChatServiceRequestFromHTTPRequest(final JsonNode event) {
    if ("ROOM".equals(event.at("/space/type").asText())) {
      throw new IllegalArgumentException("The message was received from a room");
    }
    ChatServiceRequest.Builder chatServiceRequestBuilder = ChatServiceRequest.newBuilder()
        .setChatClient(ChatClient.HANGOUTS);
    switch (event.at("/type").asText()) {
      case ADDED_TO_SPACE_EVENT:
        chatServiceRequestBuilder.setRequestType(RequestType.ADDED);
        break;
      case MESSAGE_EVENT:
        chatServiceRequestBuilder.setRequestType(RequestType.MESSAGE);
        chatServiceRequestBuilder = parseHangoutsUserMessage(chatServiceRequestBuilder, event);
        break;
      case REMOVED_FROM_SPACE_EVENT:
        chatServiceRequestBuilder.setRequestType(RequestType.REMOVED);
        break;
      case CARD_CLICKED_EVENT:
        chatServiceRequestBuilder.setRequestType(RequestType.MESSAGE);
        chatServiceRequestBuilder = parseHangoutsCardClick(chatServiceRequestBuilder, event);
        break;
      default:
        throw new IllegalArgumentException("The request has no event type");
    }
    chatServiceRequestBuilder = parseHangoutsSender(chatServiceRequestBuilder, event);
    return chatServiceRequestBuilder.build();
  }

  // get the contents of the message from the request and add it to the protobuf
  private static ChatServiceRequest.Builder parseHangoutsUserMessage(
      final ChatServiceRequest.Builder chatServiceRequestBuilder, final JsonNode event) {
    final ChatServiceRequest.UserMessage.Builder userMessageBuilder =
        ChatServiceRequest.UserMessage.newBuilder();
    if (event.at("/message").has("attachment")) {
      if (event.at("/message").has("argumentText")) {
        userMessageBuilder.setText(event.at("/message/argumentText").asText());
      }
      final Iterator<JsonNode> attachmentIterator = event.at("/message/attachment").elements();
      while (attachmentIterator.hasNext()) {
        final JsonNode attachment = (JsonNode) attachmentIterator.next();
        final ChatServiceRequest.Attachment.Builder attachmentBuilder =
            ChatServiceRequest.Attachment.newBuilder();
        attachmentBuilder.setLink(attachment.at("/downloadUri").asText());
        switch (attachment.at("/contentType").asText()) {
          case "image/png":
            attachmentBuilder.setMimeType(MimeType.PNG);
            break;
          case "image/jpeg":
            attachmentBuilder.setMimeType(MimeType.JPEG);
            break;
          default:
            attachmentBuilder.setMimeType(MimeType.UNKNOWN_MIME_TYPE);
        }
        userMessageBuilder.addAttachments(attachmentBuilder);
      }
    } else {
      userMessageBuilder.setText(event.at("/message/text").asText());
    }
    chatServiceRequestBuilder.setUserMessage(userMessageBuilder);
    return chatServiceRequestBuilder;
  }

  // get the contents of the option clicked by the user from the request and add it to the protobuf
  private static ChatServiceRequest.Builder parseHangoutsCardClick(
      final ChatServiceRequest.Builder chatServiceRequestBuilder, final JsonNode event)
      throws IllegalArgumentException {
    final ChatServiceRequest.UserMessage.Builder userMessageBuilder =
        ChatServiceRequest.UserMessage.newBuilder();
    if(!event.at("/action").has("parameters")) {
      throw new IllegalArgumentException("No card click parameters available");
    }
    userMessageBuilder.setText(event.at("/action/parameters/0/value").asText());
    chatServiceRequestBuilder.setUserMessage(userMessageBuilder);
    return chatServiceRequestBuilder;
  }

  // get the information about the sender from the request and add it to the protobuf
  private static ChatServiceRequest.Builder parseHangoutsSender(
        final ChatServiceRequest.Builder chatServiceRequestBuilder, final JsonNode event)
        throws IllegalArgumentException, IndexOutOfBoundsException {
    if(!event.at("/user").has("displayName")) {
      throw new IllegalArgumentException("No display name available in request");
    }
    if(!event.at("/space").has("name")) {
      throw new IllegalArgumentException("No spaceID available in request");
    }
    if(!event.at("/user").has("name")) {
      throw new IllegalArgumentException("No userID available in request");
    }
    final ChatServiceRequest.Sender.Builder senderBuilder = ChatServiceRequest.Sender.newBuilder()
        .setDisplayName(event.at("/user/displayName").asText())
        .setChatClientGeneratedId(event.at("/space/name").asText()
        .substring(IDMapping.SPACEID_PREFIX_LENGTH))
        .setUserId(event.at("/user/name").asText()
        .substring(IDMapping.USERID_PREFIX_LENGTH));
    chatServiceRequestBuilder.setSender(senderBuilder); 
    return chatServiceRequestBuilder;
  }
  
  private Card buildTextParagraphCard(final String text) {
    final List<WidgetMarkup> widgets = new ArrayList<>();
    widgets.add(new WidgetMarkup().setTextParagraph(new TextParagraph().setText(text)));
    final Section section = new Section().setWidgets(widgets);
    return new Card().setSections(Collections.singletonList(section));
  }

}