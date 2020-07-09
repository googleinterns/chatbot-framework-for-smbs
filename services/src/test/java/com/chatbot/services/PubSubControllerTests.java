package com.chatbot.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import com.chatbot.services.asyncservices.HangoutsAsyncService;
import com.chatbot.services.pubsubservices.PubSubAuth;
import com.chatbot.services.pubsubservices.PubSubController;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.NestedServletException;

@RunWith(SpringJUnit4ClassRunner.class)
public class PubSubControllerTests {

  @InjectMocks
  private PubSubController pubSubController;

  @Mock
  private HangoutsAsyncService asyncService;
  @Mock
  private PubSubAuth pubSubAuth;

  private MockMvc mockMvc;
  private final String uri = "/pubsub";
  final ObjectMapper mapper = new ObjectMapper();
  MockHttpServletRequestBuilder mockRequestBuilder;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(pubSubController).build();
    mockRequestBuilder =
        MockMvcRequestBuilders.post(uri).contentType(MediaType.APPLICATION_JSON_VALUE);

  }

  @Test
  public void onEvent_suggestCategoryChange() throws Exception {
    performRequest("pubSubSuggestCategoryChange.json");
    ArgumentCaptor<TriggerEventNotification> notification =
        ArgumentCaptor.forClass(TriggerEventNotification.class);
    verify(asyncService).triggerEventHandler(notification.capture());
    assertEquals(TriggerEventNotification.Event.SUGGEST_CATEGORY_CHANGE,
        notification.getValue().getEvent(), "Error parsing event");
    assertEquals("cafe",
        notification.getValue().getEventParams().getFieldsMap()
        .get("suggestedCategory").getStringValue(), "Error parsing event params");
    assertEquals("123456", notification.getValue().getUserID(), "Error parsing userID");
  }

  @Test
  public void onEvent_getCallFeedback() throws Exception {
    performRequest("pubSubGetCallFeedback.json");
    ArgumentCaptor<TriggerEventNotification> notification =
        ArgumentCaptor.forClass(TriggerEventNotification.class);
    verify(asyncService).triggerEventHandler(notification.capture());
    assertEquals(TriggerEventNotification.Event.GET_CALL_FEEDBACK,
        notification.getValue().getEvent(), "Error parsing event");
    assertEquals("+91 987456321",
        notification.getValue().getEventParams().getFieldsMap()
        .get("mobileNumber").getStringValue(),
        "Error parsing userID");
    assertEquals("123456", notification.getValue().getUserID(), "Error parsing userID");
  }

  @Test
  public void onEvent_suggestImageUpload() throws Exception {
    performRequest("pubSubSuggestImageUpload.json");
    ArgumentCaptor<TriggerEventNotification> notification =
        ArgumentCaptor.forClass(TriggerEventNotification.class);
    verify(asyncService).triggerEventHandler(notification.capture());
    assertEquals(TriggerEventNotification.Event.SUGGEST_IMAGE_UPLOAD,
        notification.getValue().getEvent(), "Error parsing event");
    assertEquals("123456", notification.getValue().getUserID(), "Error parsing userID");
  }

  @Test(expected = NestedServletException.class)
  public void onEvent_noUserID() throws Exception {
    performRequest("pubSubNoUserID.json");
  }

  @Test(expected = NestedServletException.class)
  public void onEvent_unknownEvent() throws Exception {
    performRequest("pubSubUnknownEvent.json");
  }

  private void performRequest(String fileName) throws IOException, Exception {
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder.content(mapper
        .readTree(PubSubControllerTests.class.getResourceAsStream("/" + fileName)).toString()))
        .andReturn();       
    assertEquals(200, mvcResult.getResponse().getStatus());
  }
}