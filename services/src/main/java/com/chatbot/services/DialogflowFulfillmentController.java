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

  private static final String SUGGEST_SERVICE_TIME_CHANGE = "SuggestServiceTimeChange";
  private static final String CHANGE_SERVICE_TIME = "ChangeServiceTime";
  private static final String PROVIDE_TARGETED_QUERIES_INTENT_NAME = "ProvideTargetedQueries";
  private static final String RECOMMEND_MORE_OPTIONS_INTENT_NAME = "RecommendMoreOptions";
  private static final String CHANGE_CATEGORY_INTENT_NAME = "ChangeCategory";
  private static final String EXPLAIN_MEANING_INTENT_NAME = "ExplainMeaning";
  private static final String UNRELATED_CALL_INTENT = "CallUnrelatedToBusinessCategory";
  private static final String SUGGEST_TARGET_AREA_CHANGE = "SuggestTargetAreaChange";
  private static final String CHANGE_TARGET_AREA = "ChangeTargetArea";
  private static final String CALL_FROM_OUTSIDE_SERVICE_AREA_INTENT = 
      "CallFromOutsideServiceArea";

  private static Map<String, String> parameterMap = new HashMap<>();

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
    final Iterator<String> paramKeyIterator = parameters.fieldNames();
    while (paramKeyIterator.hasNext()) {
      final String paramName = (String) paramKeyIterator.next();
      parameterMap.put(paramName, parameters.get(paramName).asText());
    }
    switch (intentName) {
      case CHANGE_CATEGORY_INTENT_NAME:
        // change the category to paramMap.get("suggestedCategory") for userID
        asyncService.sendMessageUsingUserID(userID, buildCategoryChangedMessage(parameterMap
            .get("suggestedCategory")), ChatClient.HANGOUTS, false);
        break;
      case RECOMMEND_MORE_OPTIONS_INTENT_NAME:
        // get all alternatives for the userID
        asyncService.sendMessageUsingUserID(userID, buildRecommendMoreOptionsMessage(),
            ChatClient.HANGOUTS, false);
        break;
      case PROVIDE_TARGETED_QUERIES_INTENT_NAME:
        // get targeted queries for the paramMap.get("suggestedCategory")
        asyncService.sendMessageUsingUserID(userID, buildTargetedQueriesMessage(parameterMap
            .get("suggestedCategory")), ChatClient.HANGOUTS, false);
        break;
      case EXPLAIN_MEANING_INTENT_NAME:
        // get information about the category paramMap.get("suggestedCategory")
        asyncService.sendMessageUsingUserID(userID, buildExplainMeaningMessage(parameterMap
            .get("suggestedCategory")), ChatClient.HANGOUTS, false);
      case CALL_FROM_OUTSIDE_SERVICE_AREA_INTENT:
        // parameterMap.get("mobileNumber")) was from outside of the service area of userID
        break;
      case UNRELATED_CALL_INTENT:
        //  parameterMap.get("mobileNumber")) was unrelated for userID
        break;
      case SUGGEST_TARGET_AREA_CHANGE:
        asyncService.sendMessageUsingUserID(userID, buildTargetAreaSuggestionMessage(),
            ChatClient.HANGOUTS, true);
        break;
      case CHANGE_TARGET_AREA:
        // change target area to parameterMap.get("radius.original") 
        asyncService.sendMessageUsingUserID(userID, buildTargetAreaChangedMessage(
            parameterMap.get("radius.original")), ChatClient.HANGOUTS, false);
        break;
      case SUGGEST_SERVICE_TIME_CHANGE:
        asyncService.sendMessageUsingUserID(userID, buildServiceTimingSuggestionMessage(),
            ChatClient.HANGOUTS, true);
        break;
        case CHANGE_SERVICE_TIME:
          // change service time to parameters.get("startTime.original") to
          // parameters.get("endTime.original")
          asyncService.sendMessageUsingUserID(userID, buildServiceTimingChangedMessage(
              parameters.get("startTime.original").get(0).asText(),
              parameters.get("endTime.original").get(0).asText()),
              ChatClient.HANGOUTS, false);
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

  private static String buildTargetAreaSuggestionMessage() {
    return "Please choose one:\nChange it to 5Km\nChange it to 4Km\nChange it to 3Km";
  }

  private static String buildTargetAreaChangedMessage(final String radius) {
    return "Your target area has been changed to " + radius;
  }

  private static String buildServiceTimingSuggestionMessage() {
    return "Please choose one:\nChange to 08:00 to 20:00\nChange to 09:00 to 17:00\nChange to 10:00 to 19:00";
  }
  
  private static String buildServiceTimingChangedMessage(final String startTime,
      final String endTime) {
    return "The service timings of your business have been changed to: " + startTime + " to "
        + endTime;
  }

}