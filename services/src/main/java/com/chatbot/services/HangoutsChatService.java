package com.chatbot.services;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.chat.v1.HangoutsChat;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HangoutsChatService {
  
  public static HangoutsChat chatService;

  HangoutsChatService(@Value("${hangoutsAPIScope}") final String apiScope,
      @Value("${credentialsFile}") final String credentialsFile) throws GeneralSecurityException,
      IOException {
    final GoogleCredentials credentials = GoogleCredentials.fromStream(
        HangoutsChatService.class.getResourceAsStream(credentialsFile))
        .createScoped(apiScope);
    final HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
    chatService = new HangoutsChat.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        JacksonFactory.getDefaultInstance(), requestInitializer)
        .setApplicationName("chatbot").build();
  }

}