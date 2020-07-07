package com.chatbot.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import com.chatbot.services.asyncservices.HangoutsAsyncService;
import com.chatbot.services.pubsubservices.PubSubAuth;
import com.chatbot.services.pubsubservices.PubSubController;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification;
import com.fasterxml.jackson.databind.JsonNode;
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
    final JsonNode message = mapper
        .readTree(AsyncServiceTests.class.getResourceAsStream("/pubSubSuggestCategoryChange.json"));
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder
        .content(message.toString())).andReturn();       
    assertEquals(200, mvcResult.getResponse().getStatus());
    ArgumentCaptor<TriggerEventNotification> notification =
        ArgumentCaptor.forClass(TriggerEventNotification.class);
    verify(asyncService).triggerEventHandler(notification.capture());
    assertEquals(TriggerEventNotification.Event.SUGGEST_CATEGORY_CHANGE,
        notification.getValue().getEvent(), "Error parsing event");
    assertEquals("cafe",
        notification.getValue().getEventParams().getFieldsMap()
        .get("suggestedCategory").getStringValue(),
        "Error parsing event params");
    assertEquals("123456", notification.getValue().getUserID(), "Error parsing userID");
  }

  @Test
  public void onEvent_getCallFeedback() throws Exception {
    final JsonNode message = mapper
        .readTree(AsyncServiceTests.class.getResourceAsStream("/pubSubGetCallFeedback.json"));
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder
        .content(message.toString())).andReturn();       
    assertEquals(200, mvcResult.getResponse().getStatus());
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
    final JsonNode message = mapper
        .readTree(AsyncServiceTests.class.getResourceAsStream("/pubSubSuggestImageUpload.json"));
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder
        .content(message.toString())).andReturn();       
    assertEquals(200, mvcResult.getResponse().getStatus());
    ArgumentCaptor<TriggerEventNotification> notification =
        ArgumentCaptor.forClass(TriggerEventNotification.class);
    verify(asyncService).triggerEventHandler(notification.capture());
    assertEquals(TriggerEventNotification.Event.SUGGEST_IMAGE_UPLOAD,
        notification.getValue().getEvent(), "Error parsing event");
    assertEquals("123456", notification.getValue().getUserID(), "Error parsing userID");
  }

  @Test(expected = Exception.class)
  public void onEvent_invalidAuthToken() throws Exception {
    final JsonNode message = mapper
        .readTree(AsyncServiceTests.class.getResourceAsStream("/pubSubNoUserID.json"));
    mockMvc.perform(mockRequestBuilder
        .content(message.toString())).andReturn(); 
  }

  @Test(expected = Exception.class)
  public void onEvent_unknownEvent() throws Exception {
    final JsonNode message = mapper
        .readTree(AsyncServiceTests.class.getResourceAsStream("/pubSubUnknownEvent.json"));
    mockMvc.perform(mockRequestBuilder
        .content(message.toString())).andReturn(); 
  }
}