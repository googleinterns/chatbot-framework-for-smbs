package com.chatbot.services.pubsubservices;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import org.springframework.stereotype.Component;

@Component
public class PubSubAuth {
  public void verifyRequest(final Map<String, String> headers)
      throws GeneralSecurityException, IOException {
    final GoogleIdTokenVerifier verifier =
    new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
    new JacksonFactory()).setAudience(Collections.singletonList(System.getenv("pubsubAudience")))
        .build();
    final String authorizationHeader = headers.get("authorization");
    // authorization header format: `authorization <token>`
    if (authorizationHeader == null || authorizationHeader.isEmpty()
        || authorizationHeader.split(" ").length != 2) {
      throw new IllegalArgumentException("Bad Request");
    }
    if(verifier.verify(authorizationHeader.split(" ")[1]) == null) {
      throw new IllegalArgumentException("Invalid ID token in request");
    }
  }
}