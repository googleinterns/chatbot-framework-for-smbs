package com.chatbot.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// Collection of constants used in the package
public class ChatServiceConstants {
  public static final Map<String, List<String>> EVENT_TO_CONTEXT_MAPPING = Map.of(
      "SUGGEST_CATEGORY_CHANGE", Arrays.asList("SuggestChangeCatgeoryContext"),
      "SUGGEST_IMAGE_UPLOAD", Arrays.asList("SuggestImageUploadContext"),
      "GET_CALL_FEEDBACK", Arrays.asList("GetCallFeedbackContext")); 
  public static final List<String> LIST_OF_INTENTS_WITH_INTERACTIVE_RESPONSE = new ArrayList<>(
      Arrays.asList("Default Welcome Intent", "Default Fallback Intent", "NotGettingEnoughCalls", 
      "ProbeIrrelevantCall", "GettingCallsFromOutsideServiceArea", "SuggestImageUpload",
      "GetCallFeedback", "ProbeReason", "ShouldRecommendOtherOptions", "SuggestCategoryChange"));
  public static final String IMAGES_RECEIVED_MESSAGE = "The images have been received!";
  public static final String THANKS_FOR_ADDING_MESSAGE = "Thank You for Adding me";
  public static final String NOT_EXPECTING_IMAGE_MESSAGE =
          "Sorry, we were not expecting any attachements from you.";    
  public static final String SUGGEST_SERVICE_TIME_CHANGE = "SuggestServiceTimeChange";
  public static final String CHANGE_SERVICE_TIME = "ChangeServiceTime";
  public static final String PROVIDE_TARGETED_QUERIES_INTENT_NAME = "ProvideTargetedQueries";
  public static final String RECOMMEND_MORE_OPTIONS_INTENT_NAME = "RecommendMoreOptions";
  public static final String CHANGE_CATEGORY_INTENT_NAME = "ChangeCategory";
  public static final String EXPLAIN_MEANING_INTENT_NAME = "ExplainMeaning";
  public static final String UNRELATED_CALL_INTENT = "CallUnrelatedToBusinessCategory";
  public static final String SUGGEST_TARGET_AREA_CHANGE = "SuggestTargetAreaChange";
  public static final String CHANGE_TARGET_AREA = "ChangeTargetArea";
  public static final String CALL_FROM_OUTSIDE_SERVICE_AREA_INTENT = 
      "CallFromOutsideServiceArea";
  public static final String TRIGGER_EVENT_MESSAGE = "TriggerEvent";
  public static final String SUGGEST_CATEGORY_CHANGE_EVENT = "SUGGEST_CATEGORY_CHANGE";
  public static final String SUGGEST_IMAGE_UPLOAD_EVENT = "SUGGEST_IMAGE_UPLOAD";
  public static final String GET_CALL_FEEDBACK_EVENT = "GET_CALL_FEEDBACK";
  public static final String EXPECTING_IMAGES_CONTEXT = "expectingimagescontext";
  public static final String CHAT_ISSUER = "chat@system.gserviceaccount.com";
  public static final String PUBLIC_CERT_URL_PREFIX =
      "https://www.googleapis.com/service_accounts/v1/metadata/x509/";
  public static final String HANGOUTS_USER_AGENT = "Google-Dynamite";
}