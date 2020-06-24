package com.chatbot.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DialogflowFulfillmentController {

  @Autowired
  private AsyncService asyncService;

  private static final String PROVIDE_TARGETED_QUERIES_INTENT_NAME = "ProvideTargetedQueries";
  private static final String RECOMMEND_MORE_OPTIONS_INTENT_NAME = "RecommendMoreOptions";
  private static final String CHANGE_CATEGORY_INTENT_NAME = "ChangeCategory";
  private static final String EXPLAIN_MEANING_INTENT_NAME = "ExplainMeaning";
  private static final String UNRELATED_CALL_INTENT = "CallUnrelatedToBusinessCategory";
  private static final String CALL_FROM_OUTSIDE_SERVICE_AREA_INTENT = 
      "CallFromOutsideServiceArea";

  @PostMapping("/dgf")
  public String onEvent(@RequestHeader final Map<String, String> headers,
      @RequestBody final JsonNode event) throws JsonParseException, IOException,
      IllegalArgumentException {
    if(!headers.get("authorization").equals(System.getenv("dialogflowAuthToken"))) {
      throw new IllegalArgumentException("Invalid auth token in request");
    }
    final String intentName = event.at("/queryResult/intent/displayName").asText();
    final String userID = event.at("/originalDetectIntentRequest/payload/userID").asText();
    final JsonNode parameters = event.at("/queryResult/outputContexts").get(0).at("/parameters");
    final Map<String, String> parameterMap = new HashMap<>();
    final Iterator<String> paramKeyIterator = parameters.fieldNames();
    while (paramKeyIterator.hasNext()) {
      final String paramName = (String) paramKeyIterator.next();
      parameterMap.put(paramName, parameters.get(paramName).asText());
    }
    switch (intentName) {
      case CHANGE_CATEGORY_INTENT_NAME:
        // change the category to paramMap.get("suggestedCategory") for userID
        asyncService.sendMessageUsingUserID(userID, buildCategoryChangedMessage(parameterMap
            .get("suggestedCategory")), ChatClient.HANGOUTS);
        break;
      case RECOMMEND_MORE_OPTIONS_INTENT_NAME:
        // get all alternatives for the userID
        asyncService.sendMessageUsingUserID(userID, buildRecommendMoreOptionsMessage(),
            ChatClient.HANGOUTS);
        break;
      case PROVIDE_TARGETED_QUERIES_INTENT_NAME:
        // get targeted queries for the paramMap.get("suggestedCategory")
        asyncService.sendMessageUsingUserID(userID, buildTargetedQueriesMessage(parameterMap
            .get("suggestedCategory")), ChatClient.HANGOUTS);
        break;
      case EXPLAIN_MEANING_INTENT_NAME:
        // get information about the category paramMap.get("suggestedCategory")
        asyncService.sendMessageUsingUserID(userID, buildExplainMeaningMessage(parameterMap
            .get("suggestedCategory")), ChatClient.HANGOUTS);
      case CALL_FROM_OUTSIDE_SERVICE_AREA_INTENT:
        // parameterMap.get("mobileNumber")) was from outside of the service area of userID
        break;
      case UNRELATED_CALL_INTENT:
        //  parameterMap.get("mobileNumber")) was unrelated for userID
        break;
      default:
        break;
    }
    // an empty string is returned so that the responses added to the dialogflow
    // console are used
    return "";
  }

  private static String buildCategoryChangedMessage(final String suggestedCategory) {
    return "Your category has been changed to " + suggestedCategory;
  }

  private static String buildRecommendMoreOptionsMessage() {
    return "You can also change your category to A, B or C";
  }

  private static String buildTargetedQueriesMessage(final String suggestedCategory) {
    return "These are some targeted queries for" + suggestedCategory;
  }

  private static String buildExplainMeaningMessage(final String suggestedCategory) {
    return "This is some information about the" +  suggestedCategory;
  }
}