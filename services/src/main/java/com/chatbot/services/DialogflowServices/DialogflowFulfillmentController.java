package com.chatbot.services.DialogflowServices;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.chatbot.services.ChatServiceConstants;
import com.chatbot.services.AsyncServices.HangoutsAsyncService;
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
  private HangoutsAsyncService asyncService;

  private static Map<String, String> parameterMap = new HashMap<>();

  @PostMapping("/dgf")
  String onEvent(@RequestHeader final Map<String, String> headers,
      @RequestBody final JsonNode event) throws JsonParseException, IOException,
      IllegalArgumentException {
    if(!headers.get("authorization").equals(System.getenv("dialogflowAuthToken"))) {
      throw new IllegalArgumentException("Invalid auth token in request");
    }
    final String intentName = event.at("/queryResult/intent/displayName").asText();
    final String userID = event.at("/originalDetectIntentRequest/payload/userID").asText();
    // every active context has all the variables present so it is enough to get the parameters
    // from the first context
    final JsonNode parameters = event.at("/queryResult/outputContexts").get(0).at("/parameters");
    final Iterator<String> paramKeyIterator = parameters.fieldNames();
    while (paramKeyIterator.hasNext()) {
      final String paramName = (String) paramKeyIterator.next();
      parameterMap.put(paramName, parameters.get(paramName).asText());
    }
    switch (intentName) {
      case ChatServiceConstants.CHANGE_CATEGORY_INTENT_NAME:
        // TODO: update the category of the user to paramMap.get("suggestedCategory")
        asyncService.sendMessageUsingUserID(userID, buildCategoryChangedMessage(parameterMap
            .get("suggestedCategory")), false);
        break;
      case ChatServiceConstants.RECOMMEND_MORE_OPTIONS_INTENT_NAME:
        // TODO: get recommended options for the user from the backend
        asyncService.sendMessageUsingUserID(userID, buildRecommendMoreOptionsMessage(),
            true);
        break;
      case ChatServiceConstants.PROVIDE_TARGETED_QUERIES_INTENT_NAME:
        // TODO: get targeted queries for paramMap.get("suggestedCategory")
        asyncService.sendMessageUsingUserID(userID, buildTargetedQueriesMessage(parameterMap
            .get("suggestedCategory")), false);
        break;
      case ChatServiceConstants.EXPLAIN_MEANING_INTENT_NAME:
        // TODO: get information about the category paramMap.get("suggestedCategory")
        asyncService.sendMessageUsingUserID(userID, buildExplainMeaningMessage(parameterMap
            .get("suggestedCategory")), false);
      case ChatServiceConstants.CALL_FROM_OUTSIDE_SERVICE_AREA_INTENT:
        // TODO: convey to backend that parameterMap.get("mobileNumber")) was from outside of the
        // service area of userID
        break;
      case ChatServiceConstants.UNRELATED_CALL_INTENT:
        // TODO: convey to backend that parameterMap.get("mobileNumber")) was unrelated to the
        // business for userID
        break;
      case ChatServiceConstants.SUGGEST_TARGET_AREA_CHANGE:
        // TODO: get target area change suggestion for the user
        asyncService.sendMessageUsingUserID(userID, buildTargetAreaSuggestionMessage(),
            true);
        break;
      case ChatServiceConstants.CHANGE_TARGET_AREA:
        // TODO: change target area to parameterMap.get("radius.original") for the user 
        asyncService.sendMessageUsingUserID(userID, buildTargetAreaChangedMessage(
            parameterMap.get("radius.original")), false);
        break;
      case ChatServiceConstants.SUGGEST_SERVICE_TIME_CHANGE:
        // TODO: get the suggested timings for the user form the backend
        asyncService.sendMessageUsingUserID(userID, buildServiceTimingSuggestionMessage(),
            true);
        break;
        case ChatServiceConstants.CHANGE_SERVICE_TIME:
          // TODO: change service time to parameters.get("startTime.original") to
          // parameters.get("endTime.original") for the user
          asyncService.sendMessageUsingUserID(userID, buildServiceTimingChangedMessage(
              parameters.get("startTime.original").get(0).asText(),
              parameters.get("endTime.original").get(0).asText()),
              false);
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
    return "You can also change your category to any of these:\nDiner\nHotel\nBistro";
  }

  private static String buildTargetedQueriesMessage(final String suggestedCategory) {
    return "These are some targeted queries for" + suggestedCategory;
  }

  private static String buildExplainMeaningMessage(final String suggestedCategory) {
    return "This is some information about the" +  suggestedCategory;
  }

  private static String buildTargetAreaSuggestionMessage() {
    return "\nChange target city to Hyderabad\nChange target radius to 10Km\nChange target radius to 20Km";
  }

  private static String buildTargetAreaChangedMessage(final String radius) {
    return "Your target radius has been changed to " + radius;
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