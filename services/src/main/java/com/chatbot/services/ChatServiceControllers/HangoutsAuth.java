package com.chatbot.services.ChatServiceControllers;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GooglePublicKeysManager;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import org.springframework.stereotype.Component;

import com.chatbot.services.ChatServiceConstants;

// Authenticate requests at the chat service endpoint

@Component
public class HangoutsAuth {

  // verify the request using header params
  public void verifyRequest(Map<String, String> headers)
    throws IOException, GeneralSecurityException {
    final String AUDIENCE = System.getenv("projectNumber");
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
  }
}