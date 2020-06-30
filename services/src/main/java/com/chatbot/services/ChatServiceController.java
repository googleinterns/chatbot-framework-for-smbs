package com.chatbot.services;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GooglePublicKeysManager;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.chat.v1.model.ActionResponse;
import com.google.api.services.chat.v1.model.Card;
import com.google.api.services.chat.v1.model.Message;
import com.google.api.services.chat.v1.model.Section;
import com.google.api.services.chat.v1.model.TextParagraph;
import com.google.api.services.chat.v1.model.WidgetMarkup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.MimeType;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.RequestType;

@RestController
public class ChatServiceController {

  @Autowired
  private AsyncService asyncService;
  private static final Logger logger = LoggerFactory.getLogger(ChatServiceController.class);
  private static final String REMOVED_FROM_SPACE_EVENT = "REMOVED_FROM_SPACE";
  private static final String ADDED_TO_SPACE_EVENT = "ADDED_TO_SPACE";
  private static final String MESSAGE_EVENT = "MESSAGE";
  private static final String CARD_CLICKED_EVENT = "CARD_CLICKED";
  private static final String AUDIENCE = System.getenv("projectNumber");

  @PostMapping("/")
  public Message onRequest(@RequestHeader final Map<String, String> headers,
      @RequestBody final JsonNode event) throws IOException, GeneralSecurityException {
    if (headers.get("user-agent").equals(ChatServiceConstants.HANGOUTS_USER_AGENT)) {
      // authorization header format: `authorization <token>`
      final String BEARER_TOKEN = headers.get("authorization").split(" ")[1];
      final JacksonFactory factory = new JacksonFactory();
      final GooglePublicKeysManager.Builder keyManagerBuilder = new GooglePublicKeysManager.Builder(
          new NetHttpTransport(), factory);
      final String certUrl = ChatServiceConstants.PUBLIC_CERT_URL_PREFIX
          + ChatServiceConstants.CHAT_ISSUER;
      keyManagerBuilder.setPublicCertsEncodedUrl(certUrl);
      final GoogleIdTokenVerifier.Builder verifierBuilder = new GoogleIdTokenVerifier.Builder(
          keyManagerBuilder.build());
      verifierBuilder.setIssuer(ChatServiceConstants.CHAT_ISSUER);
      final GoogleIdTokenVerifier verifier = verifierBuilder.build();
      final GoogleIdToken idToken = GoogleIdToken.parse(factory, BEARER_TOKEN);
      if (idToken == null) {
        throw new IllegalArgumentException("Token cannot be parsed");
      }
      if (!verifier.verify(idToken) || !idToken.verifyAudience(Collections.singletonList(AUDIENCE))
          || !idToken.verifyIssuer(ChatServiceConstants.CHAT_ISSUER)) {
        throw new IllegalArgumentException("Invalid token");
      }
      try {
        asyncService.hangoutsAsyncHandler(BuildChatServiceRequestFromHangoutsRequest(event));
      } catch (final Exception e) {
        // If there was an error in parsing the request, either we do not support the
        // type of request or the format of the request is incorrect, in both these cases
        // returning an empty string is an option.
        logger.error("Error while sending hangouts reply", e);
      }
    } else {
      // parseWhatsappRequest(event);
      // whatsapp async handler
      // return acknowledgement
    }
    final Message reply = new Message();
    if (!event.at("/type").asText().equals(CARD_CLICKED_EVENT)) {
      // an empty message acts as an acknowledgement for a text message
      reply.setText("");
    } else {
      // replace the button with a text paragraph to render it unclickable
      reply.setActionResponse((new ActionResponse()).set("type", "UPDATE_MESSAGE"));
      final List<WidgetMarkup> widgets = new ArrayList<>();
      final TextParagraph widget =
          new TextParagraph().setText(event.at("/action/parameters/0/value").asText());
      widgets.add(new WidgetMarkup().setTextParagraph(widget));
      final Section section = new Section()
          .setWidgets(widgets);
      reply.setCards(Collections
          .singletonList((new Card()).setSections(Collections.singletonList(section))));
    }
    return reply;
  }

  private ChatServiceRequest BuildChatServiceRequestFromHangoutsRequest(final JsonNode event)
      throws Exception {
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

  private ChatServiceRequest.Builder parseHangoutsUserMessage(
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

  private ChatServiceRequest.Builder parseHangoutsCardClick(
      final ChatServiceRequest.Builder chatServiceRequestBuilder, final JsonNode event) {
    final ChatServiceRequest.UserMessage.Builder userMessageBuilder =
        ChatServiceRequest.UserMessage.newBuilder();
    userMessageBuilder.setText(event.at("/action/parameters/0/value").asText());
    chatServiceRequestBuilder.setUserMessage(userMessageBuilder);
    return chatServiceRequestBuilder;
  }

  private ChatServiceRequest.Builder parseHangoutsSender(
        final ChatServiceRequest.Builder chatServiceRequestBuilder, final JsonNode event) {
    final ChatServiceRequest.Sender.Builder senderBuilder = ChatServiceRequest.Sender.newBuilder()
        .setDisplayName(event.at("/user/displayName").asText())
        .setChatClientGeneratedId(event.at("/space/name").asText()
        .substring(IDMapping.SPACEID_PREFIX_LENGTH))
        .setUserId(event.at("/user/name").asText().substring(IDMapping.USERID_PREFIX_LENGTH));
    chatServiceRequestBuilder.setSender(senderBuilder); 
    return chatServiceRequestBuilder;
  }
}