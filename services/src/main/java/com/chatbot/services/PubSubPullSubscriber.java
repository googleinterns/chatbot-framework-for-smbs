package com.chatbot.services;

import java.io.IOException;
import java.util.Map;

import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification.ChatClient;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification.Event;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.protobuf.Struct;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

// Annotating this class with @Component will launch the pull subscriber on application start up
public class PubSubPullSubscriber {

  @Autowired
  private AsyncService asyncService;
  private static String projectID;
  private static String subscriptionID;
  private static final String TRIGGER_EVENT_MESSAGE = "TriggerEvent";
  private static final String SUGGEST_CATEGORY_CHANGE_EVENT = "SUGGEST_CATEGORY_CHANGE";
  private static final String SUGGEST_IMAGE_UPLOAD_EVENT = "SUGGEST_IMAGE_UPLOAD";
  private static Long maxOutstandingElements;
  private static Long maxOutstandingBytes;

  public PubSubPullSubscriber(
      @Value("${pubsubConfig.maxOutstandingElements}") final String maxOutstandingElementsToSet,
      @Value("${pubsubConfig.maxOutstandingBytes}") final String maxOutstandingBytesToSet)
      throws InterruptedException {
    projectID = System.getenv("projectID");
    subscriptionID = System.getenv("subscriptionID");
    maxOutstandingElements = Long.parseLong(maxOutstandingElementsToSet);
    maxOutstandingBytes = Long.parseLong(maxOutstandingBytesToSet);
    launchSubscriber();
  }

  private void launchSubscriber()
      throws InterruptedException, IllegalArgumentException {
    final ProjectSubscriptionName subscriptionName = ProjectSubscriptionName
        .of(projectID, subscriptionID);
    final MessageReceiver receiver =
        (final PubsubMessage message, final AckReplyConsumer consumer) -> {
      final String messageData = message.getData().toStringUtf8();
      final Map<String, String> messageAttributesMap = message.getAttributesMap();
      if(messageData.equals(TRIGGER_EVENT_MESSAGE)) {
        final TriggerEventNotification triggerEventNotification = 
            buildNotificationFromMessage(messageAttributesMap);
        try {
          asyncService.triggerEventHandler(triggerEventNotification);
        } catch (final IOException e) {
          e.printStackTrace();
        }
      } else {
        throw new IllegalArgumentException("Unknown message received at subscriber");
      }
      consumer.ack();
    };
    final FlowControlSettings flowControlSettings = FlowControlSettings.newBuilder()
        .setMaxOutstandingElementCount(maxOutstandingElements)
        .setMaxOutstandingRequestBytes(maxOutstandingBytes)
        .build();
    final ExecutorProvider executorProvider = InstantiatingExecutorProvider.newBuilder()
        .setExecutorThreadCount(4)
        .build();
    final Subscriber subscriber = Subscriber.newBuilder(subscriptionName, receiver)
        .setFlowControlSettings(flowControlSettings)
        .setExecutorProvider(executorProvider)
        .build();
    subscriber.startAsync().awaitRunning();
  }

  private TriggerEventNotification buildNotificationFromMessage(
      final Map<String, String> messageAttributesMap) throws IllegalArgumentException {
    final TriggerEventNotification.Builder triggerEventNotificationBuilder = 
        TriggerEventNotification.newBuilder();
    if(messageAttributesMap.containsKey("userID")) {
      triggerEventNotificationBuilder.setUserID(messageAttributesMap.get("userID"));
    } else {
      throw new IllegalArgumentException("No userID provided in published message");
    }
    if(messageAttributesMap.containsKey("chatClient")) {
      switch (messageAttributesMap.get("chatClient")) {
        case "HANGOUTS":
          triggerEventNotificationBuilder.setChatClient(ChatClient.HANGOUTS);
          break;
        case "WHATSAPP":
          triggerEventNotificationBuilder.setChatClient(ChatClient.WHATSAPP);
          break;
        default:
          throw new IllegalArgumentException("Unknown client provided in published message"); 
      }
    } else {
      throw new IllegalArgumentException("No chat client provided in published message");
    } 
    if(messageAttributesMap.containsKey("event")) {
      switch (messageAttributesMap.get("event")) {
        case SUGGEST_CATEGORY_CHANGE_EVENT:
          final com.google.protobuf.Value suggestedCategory = com.google.protobuf.Value.newBuilder()
              .setStringValue(messageAttributesMap.get("suggestedCategory")).build();
          final Struct eventParams =
              Struct.newBuilder().putFields("suggestedCategory", suggestedCategory).build();
          triggerEventNotificationBuilder
              .setEvent(Event.SUGGEST_CATEGORY_CHANGE).setEventParams(eventParams);
          break;
        case SUGGEST_IMAGE_UPLOAD_EVENT:
          triggerEventNotificationBuilder.setEvent(Event.SUGGEST_IMAGE_UPLOAD);
          break;
        default:
          throw new IllegalArgumentException("Unknown event provided in published message");
      }
    } else {
      throw new IllegalArgumentException("No event provided in published message");
    }    
    return triggerEventNotificationBuilder.build();
  }
}