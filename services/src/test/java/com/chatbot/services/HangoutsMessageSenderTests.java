package com.chatbot.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.chatbot.services.messagesenders.HangoutsMessageSender;
import com.chatbot.services.messagesenders.HangoutsMessageSender.HANGOUTS_MESSAGE_TYPE;
import com.google.api.services.chat.v1.HangoutsChat;
import com.google.api.services.chat.v1.HangoutsChat.Spaces;
import com.google.api.services.chat.v1.HangoutsChat.Spaces.Messages;
import com.google.api.services.chat.v1.HangoutsChat.Spaces.Messages.Create;
import com.google.api.services.chat.v1.model.ActionParameter;
import com.google.api.services.chat.v1.model.Button;
import com.google.api.services.chat.v1.model.Card;
import com.google.api.services.chat.v1.model.FormAction;
import com.google.api.services.chat.v1.model.Message;
import com.google.api.services.chat.v1.model.OnClick;
import com.google.api.services.chat.v1.model.Section;
import com.google.api.services.chat.v1.model.TextButton;
import com.google.api.services.chat.v1.model.WidgetMarkup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

// tests for {@link com.chatbot.services.messagesenders.HangoutsMessageSender}
@RunWith(MockitoJUnitRunner.class)
public class HangoutsMessageSenderTests {

  @InjectMocks
  private HangoutsMessageSender hangoutsMessageSender;
  @Mock
  private HangoutsChatService mockHangoutsChatService;
  @Mock
  HangoutsChat mockHangoutsChat;
  @Mock
  Spaces mockSpaces;
  @Mock
  Messages mockMessages;
  @Mock
  Create mockCreate;

  Message cardMessage;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    when(mockHangoutsChatService.getChatService()).thenReturn(mockHangoutsChat);
    when(mockHangoutsChat.spaces()).thenReturn(mockSpaces);
    when(mockSpaces.messages()).thenReturn(mockMessages);
    when(mockMessages.create(anyString(), any(Message.class))).thenReturn(mockCreate);
    when(mockCreate.execute()).thenReturn(new Message());

    List<Section> sectionList = new ArrayList<>();
    for (final String option : new String[] {"foo", "bar", "foobar"}) {
      final List<WidgetMarkup> widgets = new ArrayList<>();
      final List<ActionParameter> customParameters = Collections
          .singletonList(new ActionParameter()
          .setKey("message")
          .setValue(option));
      final FormAction action = new FormAction()
          .setActionMethodName("INTERACTIVE_TEXT_BUTTON_ACTION")
          .setParameters(customParameters);
      final OnClick onClick = new OnClick().setAction(action);
      final TextButton button = new TextButton()
          .setText(option)
          .setOnClick(onClick);
      final Button widget = new Button().setTextButton(button);
      widgets.add(new WidgetMarkup().setButtons(Collections.singletonList((widget))));
      final Section section = new Section().setWidgets(widgets);
      sectionList.add(section);
    }
    final Card card =
        (new Card()).setSections(Collections.unmodifiableList(sectionList));
    cardMessage = new Message().setCards(Collections.singletonList(card));
  }

  @Test
  public void sendMessageBasedOnMessageType_validMessafe() throws IOException {
    hangoutsMessageSender.sendMessageBasedOnMessageType("123", "foo", HANGOUTS_MESSAGE_TYPE.TEXT);
    verify(mockCreate).execute();
  }

  @Test
  public void sendMessageBasedOnMessageType_textMessage() throws IOException {
    hangoutsMessageSender.sendMessageBasedOnMessageType("123", "foo", HANGOUTS_MESSAGE_TYPE.TEXT);
    verify(mockMessages).create("spaces/123", new Message().setText("foo"));
  }

  @Test
  public void sendMessageBasedOnMessageType_cardMessage() throws IOException {
    hangoutsMessageSender.sendMessageBasedOnMessageType("123", "foo\nbar\nfoobar",
        HANGOUTS_MESSAGE_TYPE.CARD);
    verify(mockMessages).create("spaces/123", cardMessage);
  }

  @Test
  public void sendMessageBasedOnMessageType_cardMessageWithHeader() throws IOException {
    hangoutsMessageSender.sendMessageBasedOnMessageType("123", "header\nfoo\nbar\nfoobar",
        HANGOUTS_MESSAGE_TYPE.CARD_WITH_HEADER);
    verify(mockMessages).create("spaces/123", cardMessage);
    verify(mockMessages).create("spaces/123", new Message().setText("header"));
  }

}