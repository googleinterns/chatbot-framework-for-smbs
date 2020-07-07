package com.chatbot.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import com.chatbot.services.asyncservices.HangoutsAsyncService;
import com.chatbot.services.dialogflowservices.DialogflowFulfillmentController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

// tests for {@link com.chatbot.services.dialogflowservices.DialogflowFulfillmentController}
@RunWith(SpringJUnit4ClassRunner.class)
public class DialogflowFulfillmentControllerTests {

  @InjectMocks
  private DialogflowFulfillmentController dialogflowFulfillmentController;

  @Mock
  private HangoutsAsyncService asyncService;

  private MockMvc mockMvc;
  private final String uri = "/dgf";
  private JsonNode validRequest;
  private JsonNode changeCategoryFulfillmentRequest;
  private JsonNode changeTargetLocationFulfillmentRequest;
  private JsonNode callFromOutsideServiceAreaFulfillmentRequest;
  private JsonNode changeServiceTimeFulfillmentRequest;
  private JsonNode explainMeanginFulfillmentRequest;
  private JsonNode provideTargettedQueriesFulfillmentRequest;
  private JsonNode recommendMoreOptionsFulfillmentRequest;
  private JsonNode suggestServiceTimeChangeFulfillmentRequest;
  private JsonNode suggestTargetAreaFulfillmentRequest;
  private JsonNode unrelatedCallFulfillmentRequest;
  private MockHttpServletRequestBuilder mockRequestBuilder;
  private final String authToken = System.getenv("dialogflowauthtoken");

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(dialogflowFulfillmentController).build();
    mockRequestBuilder = MockMvcRequestBuilders.post(uri)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header("authorization", authToken);
    ObjectMapper mapper = new ObjectMapper();
    validRequest = mapper
        .readTree(DialogflowFulfillmentControllerTests.class
        .getResourceAsStream("/fulfillmentRequest.json"));
    changeCategoryFulfillmentRequest = mapper
        .readTree(DialogflowFulfillmentControllerTests.class
        .getResourceAsStream("/changeCategoryFulfillmentRequest.json"));
    changeTargetLocationFulfillmentRequest = mapper
        .readTree(DialogflowFulfillmentControllerTests.class
        .getResourceAsStream("/changeTargetLocationFulfillmentRequest.json"));
    callFromOutsideServiceAreaFulfillmentRequest = mapper
        .readTree(DialogflowFulfillmentControllerTests.class
        .getResourceAsStream("/callFromOutsideServiceAreaFulfillmentRequest.json"));
    changeServiceTimeFulfillmentRequest = mapper
        .readTree(DialogflowFulfillmentControllerTests.class
        .getResourceAsStream("/changeServiceTimeFulfillmentRequest.json"));
    explainMeanginFulfillmentRequest = mapper
        .readTree(DialogflowFulfillmentControllerTests.class
        .getResourceAsStream("/explainMeanginFulfillmentRequest.json"));
    provideTargettedQueriesFulfillmentRequest = mapper
        .readTree(DialogflowFulfillmentControllerTests.class
        .getResourceAsStream("/provideTargettedQueriesFulfillmentRequest.json"));
    recommendMoreOptionsFulfillmentRequest = mapper
        .readTree(DialogflowFulfillmentControllerTests.class
        .getResourceAsStream("/recommendMoreOptionsFulfillmentRequest.json"));
    suggestServiceTimeChangeFulfillmentRequest = mapper
        .readTree(DialogflowFulfillmentControllerTests.class
        .getResourceAsStream("/suggestServiceTimeChangeFulfillmentRequest.json"));
    suggestTargetAreaFulfillmentRequest = mapper
        .readTree(DialogflowFulfillmentControllerTests.class
        .getResourceAsStream("/suggestTargetAreaFulfillmentRequest.json"));
    unrelatedCallFulfillmentRequest = mapper
        .readTree(DialogflowFulfillmentControllerTests.class
        .getResourceAsStream("/unrelatedCallFulfillmentRequest.json"));

  }

  @Test(expected = Exception.class)
  public void onEvent_invalidAuthToken() throws Exception {
    final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(uri)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header("authorization", "invalidAuthToken")
        .content(validRequest.toString())).andReturn();   
    mvcResult.getResponse().getStatus();
  }

  @Test
  public void onEvent_validRequest() throws Exception {
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder
        .content(validRequest.toString())).andReturn();       
    assertEquals(200, mvcResult.getResponse().getStatus(), "Error handling fulfillment request");
  }

  @Test
  public void onEvent_changeCategoryRequest() throws Exception {
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder
        .content(changeCategoryFulfillmentRequest.toString())).andReturn();
    assertEquals(200, mvcResult.getResponse().getStatus(), "Error handling fulfillment request");
    verify(asyncService).sendMessageUsingUserID("123456", "Your category has been changed to cafe",
        false);
  }

  @Test
  public void onEvent_callFromOutsideServiceArea() throws Exception {
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder
        .content(callFromOutsideServiceAreaFulfillmentRequest.toString())).andReturn();
    assertEquals(200, mvcResult.getResponse().getStatus(), "Error handling fulfillment request");
  }
  @Test
  public void onEvent_changeServiceTime() throws Exception {
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder
        .content(changeServiceTimeFulfillmentRequest.toString())).andReturn();
    assertEquals(200, mvcResult.getResponse().getStatus(), "Error handling fulfillment request");
    verify(asyncService).sendMessageUsingUserID("123456",
        "The service timings of your business have been changed to: 08:00 to 17:00", false);
  }
  @Test
  public void onEvent_explainMeaning() throws Exception {
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder
        .content(explainMeanginFulfillmentRequest.toString())).andReturn();
    assertEquals(200, mvcResult.getResponse().getStatus(), "Error handling fulfillment request");
    verify(asyncService).sendMessageUsingUserID("123456",
        "This is some information about the cafe", false);
  }
  @Test
  public void onEvent_provideTargettedQueries() throws Exception {
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder
        .content(provideTargettedQueriesFulfillmentRequest.toString())).andReturn();
    assertEquals(200, mvcResult.getResponse().getStatus(), "Error handling fulfillment request");
    verify(asyncService).sendMessageUsingUserID("123456",
        "These are some targeted queries for cafe", false);
  }
  @Test
  public void onEvent_recommendMoreOptions() throws Exception {
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder
        .content(recommendMoreOptionsFulfillmentRequest.toString())).andReturn();
    assertEquals(200, mvcResult.getResponse().getStatus(), "Error handling fulfillment request");
    verify(asyncService).sendMessageUsingUserID("123456",
        "You can also change your category to any of these:\nDiner\nHotel\nBistro", true);
  }
  @Test
  public void onEvent_suggestServiceTimeChange() throws Exception {
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder
        .content(suggestServiceTimeChangeFulfillmentRequest.toString())).andReturn();
    assertEquals(200, mvcResult.getResponse().getStatus(), "Error handling fulfillment request");
    verify(asyncService).sendMessageUsingUserID("123456",
        "Please choose one:\nChange to 08:00 to 20:00\nChange to 09:00 to 17:00\nChange to 10:00 to 19:00", true);
  }
  @Test
  public void onEvent_suggestTargetArea() throws Exception {
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder
        .content(suggestTargetAreaFulfillmentRequest.toString())).andReturn();
    assertEquals(200, mvcResult.getResponse().getStatus(), "Error handling fulfillment request");
    verify(asyncService).sendMessageUsingUserID("123456",
        "Please choose one:\nChange target city to Hyderabad\nChange target radius to 10Km\nChange target radius to 20Km",
        true);
  }
  @Test
  public void onEvent_unrelatedCall() throws Exception {
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder
        .content(unrelatedCallFulfillmentRequest.toString())).andReturn();
    assertEquals(200, mvcResult.getResponse().getStatus(), "Error handling fulfillment request");
  }
  @Test
  public void onEvent_changeTargetArea() throws Exception {
    final MvcResult mvcResult = mockMvc.perform(mockRequestBuilder
        .content(changeTargetLocationFulfillmentRequest.toString())).andReturn();
    assertEquals(200, mvcResult.getResponse().getStatus(), "Error handling fulfillment request");
    verify(asyncService).sendMessageUsingUserID("123456",
        "Your target radius has been changed to 10Km", false);
  }
}