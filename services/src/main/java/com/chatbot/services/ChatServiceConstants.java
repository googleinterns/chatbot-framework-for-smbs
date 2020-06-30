package com.chatbot.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

 class ChatServiceConstants {
  static final Map<String, List<String>> EVENT_TO_CONTEXT_MAPPING = Map.of(
      "SUGGEST_CATEGORY_CHANGE", Arrays.asList("SuggestChangeCatgeoryContext"),
      "SUGGEST_IMAGE_UPLOAD", Arrays.asList("SuggestImageUploadContext"),
      "GET_CALL_FEEDBACK", Arrays.asList("GetCallFeedbackContext")); 
  static final List<String> LIST_OF_INTENTS_WITH_INTERACTIVE_RESPONSE = new ArrayList<>(
      Arrays.asList("Default Welcome Intent", "Default Fallback Intent", "NotGettingEnoughCalls", 
      "ProbeIrrelevantCall", "GettingCallsFromOutsideServiceArea", "SuggestImageUpload",
      "GetCallFeedback", "ProbeReason", "ShouldRecommendOtherOptions", "SuggestCategoryChange"));    
  static final String SUGGEST_SERVICE_TIME_CHANGE = "SuggestServiceTimeChange";
  static final String CHANGE_SERVICE_TIME = "ChangeServiceTime";
  static final String PROVIDE_TARGETED_QUERIES_INTENT_NAME = "ProvideTargetedQueries";
  static final String RECOMMEND_MORE_OPTIONS_INTENT_NAME = "RecommendMoreOptions";
  static final String CHANGE_CATEGORY_INTENT_NAME = "ChangeCategory";
  static final String EXPLAIN_MEANING_INTENT_NAME = "ExplainMeaning";
  static final String UNRELATED_CALL_INTENT = "CallUnrelatedToBusinessCategory";
  static final String SUGGEST_TARGET_AREA_CHANGE = "SuggestTargetAreaChange";
  static final String CHANGE_TARGET_AREA = "ChangeTargetArea";
  static final String CALL_FROM_OUTSIDE_SERVICE_AREA_INTENT = 
      "CallFromOutsideServiceArea";
  static final String TRIGGER_EVENT_MESSAGE = "TriggerEvent";
  static final String SUGGEST_CATEGORY_CHANGE_EVENT = "SUGGEST_CATEGORY_CHANGE";
  static final String SUGGEST_IMAGE_UPLOAD_EVENT = "SUGGEST_IMAGE_UPLOAD";
  static final String GET_CALL_FEEDBACK_EVENT = "GET_CALL_FEEDBACK";
  static final String EXPECTING_IMAGES_CONTEXT = "expectingimagescontext";
  static final String CHAT_ISSUER = "chat@system.gserviceaccount.com";
  static final String PUBLIC_CERT_URL_PREFIX =
      "https://www.googleapis.com/service_accounts/v1/metadata/x509/";
  static final String HANGOUTS_USER_AGENT = "Google-Dynamite";
}