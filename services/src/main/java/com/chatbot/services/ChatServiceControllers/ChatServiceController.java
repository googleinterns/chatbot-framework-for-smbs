package com.chatbot.services.ChatServiceControllers;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.services.chat.v1.model.Message;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

// Receive messages from chat clients and dispatch response calls

abstract class ChatServiceController {
  // handle a request from the chat client
  abstract Message onRequest(@RequestHeader final Map<String, String> headers,
      @RequestBody final JsonNode event) throws IOException, GeneralSecurityException, Exception;
  // build the ChatServiceRequest protobuf from the HTTP request at the chat servcie endpoint
  abstract ChatServiceRequest buildChatServiceRequestFromHTTPRequest(final JsonNode event)
      throws Exception;
}