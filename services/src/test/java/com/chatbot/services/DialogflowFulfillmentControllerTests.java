package com.chatbot.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import com.chatbot.services.AsyncServices.HangoutsAsyncService;
import com.chatbot.services.DialogflowServices.DialogflowFulfillmentController;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(SpringJUnit4ClassRunner.class)
public class DialogflowFulfillmentControllerTests {

  @InjectMocks
  private DialogflowFulfillmentController dialogflowFulfillmentController;

  @Mock
  private HangoutsAsyncService asyncService;

  private MockMvc mockMvc;
  private final String uri = "/dgf";
  private JsonNode validRequest;
  private JsonNode changeCategoryRequest;
  private JsonNode changeTargetLocationRequest;
  private final String authToken = System.getenv("dialogflowAuthToken");

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(dialogflowFulfillmentController).build();
    validRequest = (new ObjectMapper())
        .readTree(AsyncServiceTests.class.getResourceAsStream("/fulfillmentRequest.json"));
    changeCategoryRequest = (new ObjectMapper())
        .readTree(AsyncServiceTests.class
        .getResourceAsStream("/changeCategoryFulfillmentRequest.json"));
    changeTargetLocationRequest = (new ObjectMapper())
        .readTree(AsyncServiceTests.class
        .getResourceAsStream("/changeTargetLocationFulfillmentRequest.json"));
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
    final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(uri)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header("authorization", authToken)
        .content(validRequest.toString())).andReturn();       
    assertEquals(200, mvcResult.getResponse().getStatus(), "Error handling fulfillment request");
  }

  @Test
  public void onEvent_changeCategoryRequest() throws Exception {
    final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(uri)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header("authorization", authToken)
        .content(changeCategoryRequest.toString())).andReturn();
    assertEquals(200, mvcResult.getResponse().getStatus(), "Error handling fulfillment request");
    verify(asyncService).sendMessageUsingUserID("123456", "Your category has been changed to cafe",
        false);
  }

  @Test
  public void onEvent_changeTargetArea() throws Exception {
    final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(uri)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header("authorization", authToken)
        .content(changeTargetLocationRequest.toString())).andReturn();
    assertEquals(200, mvcResult.getResponse().getStatus(), "Error handling fulfillment request");
    verify(asyncService).sendMessageUsingUserID("123456",
        "Your target radius has been changed to 10Km", false);
  }
}