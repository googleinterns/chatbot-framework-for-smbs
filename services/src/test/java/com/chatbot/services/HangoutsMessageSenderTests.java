package com.chatbot.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import com.chatbot.services.MessageSenders.HangoutsMessageSender;
import com.chatbot.services.MessageSenders.HangoutsMessageSender.HANGOUTS_MESSAGE_TYPE;
import com.google.api.services.chat.v1.model.Message;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HangoutsMessageSenderTests {
    
  @Test
  public void generateMessageListByType_cardWithHeader() {
    final List<Message> messageList = HangoutsMessageSender
        .generateMessageListByType("Choose\n1\n2", HANGOUTS_MESSAGE_TYPE.CARD_WITH_HEADER);
    assertEquals(2, messageList.size(), "Incorrect number of messages generated");
    assertEquals("Choose", messageList.get(0).getText(), "Incorrect header message");
  }

  @Test
  public void generateCardMessage_valid() {
    final Message cardMessage =
        HangoutsMessageSender.generateCardMessage(Arrays.asList("Foo", "Bar"));
    assertEquals(2, cardMessage.getCards().get(0).getSections().size(),
        "Incorrect number of sections in card");
    assertEquals("Foo", cardMessage
        .getCards().get(0)
        .getSections().get(0)
        .getWidgets().get(0)
        .getButtons().get(0)
        .getTextButton().getText(), "Incorrect option in message");
  }

}