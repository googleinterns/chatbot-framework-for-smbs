package com.chatbot.services;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.chat.v1.HangoutsChat;
import com.google.api.services.chat.v1.model.Membership;
import com.google.api.services.chat.v1.model.Space;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// This class handles the loading, updating and queries for the google user IDs and chat client
// generated IDs of users
@Component
public class IDMapping {

  private static String CHAT_SCOPE;
  private static Map<ChatClient, BiMap<String, String>> ChatClientToChatClientBiMapMapping;
  private static String SERVICE_ACCOUNT_FILE;
  public static final int SPACEID_PREFIX_LENGTH = 7;
  public static final int USERID_PREFIX_LENGTH = 6;

  @Autowired
  public IDMapping(@Value("${hangoutsAPIScope}") final String apiScope,
      @Value("${credentialsFile}") final String credentialsFile) throws GeneralSecurityException,
      IOException {
    SERVICE_ACCOUNT_FILE = credentialsFile;
    CHAT_SCOPE = apiScope;
    ChatClientToChatClientBiMapMapping = new HashMap<ChatClient, BiMap<String, String>>();
    ChatClientToChatClientBiMapMapping.put(ChatClient.WHATSAPP, HashBiMap.create());
    ChatClientToChatClientBiMapMapping.put(ChatClient.HANGOUTS, HashBiMap.create());
    populateHangoutsBiMap();
  }

  public IDMapping() {
    ChatClientToChatClientBiMapMapping = new HashMap<ChatClient, BiMap<String, String>>();
    ChatClientToChatClientBiMapMapping.put(ChatClient.WHATSAPP, HashBiMap.create());
    ChatClientToChatClientBiMapMapping.put(ChatClient.HANGOUTS, HashBiMap.create());
  }

  // populate the IDs of Hangouts users
  private void populateHangoutsBiMap() throws GeneralSecurityException, IOException {
    final GoogleCredentials credentials =
        GoogleCredentials.fromStream(IDMapping.class.getResourceAsStream(SERVICE_ACCOUNT_FILE))
        .createScoped(CHAT_SCOPE);
    final HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
    HangoutsChat chatService =
        new HangoutsChat.Builder(GoogleNetHttpTransport.newTrustedTransport(),
        JacksonFactory.getDefaultInstance(),
        requestInitializer).setApplicationName("chatbot").build();
    final List<Space> spacesList = chatService.spaces().list().execute().getSpaces();
    for (final Space space : spacesList) {
      final String spaceName = space.getName();
      final List<Membership> memebershipList = 
          chatService.spaces().members().list(spaceName).execute().getMemberships();
      memebershipList.forEach(membership -> ChatClientToChatClientBiMapMapping
          .get(ChatClient.HANGOUTS)
          .put(spaceName.substring(SPACEID_PREFIX_LENGTH),
          membership.getMember().getName().substring(USERID_PREFIX_LENGTH)));
    }
  }

  public void addNewMapping(final String chatClientGeneratedID, final String userID,
      final ChatClient chatClient) {
    ChatClientToChatClientBiMapMapping.get(chatClient).put(chatClientGeneratedID, userID);
  }

  // get the chat client specific ID for a user given the userID and the chat
  // client
  public String getChatClientGeneratedID(final String userID, final ChatClient chatClient) {
    return ChatClientToChatClientBiMapMapping.get(chatClient).inverse().get(userID);
  }

  // get the chat client specific ID for a user given the userID
  public String getChatClientGeneratedID(final String userID) {
    for (final ChatClient chatClient : ChatClient.values()) {
      if (ChatClientToChatClientBiMapMapping.containsKey(chatClient)) {
        if (ChatClientToChatClientBiMapMapping.get(chatClient).inverse().containsKey(userID)) {
          return ChatClientToChatClientBiMapMapping.get(chatClient).inverse().get(userID);
        }
      }
    }
    return StringUtils.EMPTY;
  }

  // get the user ID associated with a given chat client generated ID
  public String getUserID(final String chatClientGeneratedID) {
    for (final ChatClient chatClient : ChatClient.values()) {
      if (ChatClientToChatClientBiMapMapping.containsKey(chatClient)) {
        if (ChatClientToChatClientBiMapMapping.get(chatClient).containsKey(chatClientGeneratedID)) {
          return ChatClientToChatClientBiMapMapping.get(chatClient).get(chatClientGeneratedID);
        }
      }
    }
    return StringUtils.EMPTY;
  }

  // get the user ID associated with a given chat client generated ID and the chat client
  public String getUserID(final String chatClientGeneratedID, final ChatClient chatClient) {
    if(ChatClientToChatClientBiMapMapping.get(chatClient).containsKey(chatClientGeneratedID)) {
      return ChatClientToChatClientBiMapMapping.get(chatClient).get(chatClientGeneratedID);
    }
    return StringUtils.EMPTY;
  }
}