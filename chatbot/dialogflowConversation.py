import random
import json
import datetime
import dialogflow_v2 as dialogflow

class Conversation:
    def __init__(self, projectID, langCode = "en-US", sessionID = None):
        self.beginTime = None
        self.conversationStarted = False
        # any string of upto 36 bytes is a valid session ID
        if sessionID is None:
            self.sessionID = random.randint(10**10, 10**10*9)
        else:
            self.sessionID = sessionID
        self.projectID = projectID
        self.langCode = langCode
        self.session_client = dialogflow.SessionsClient()
        self.session = self.session_client.session_path(self.projectID, self.sessionID)
    def triggerEvent(self, event, params={}, webhookPayload={}):
        # these are the parameters which will be used to frame
        # a response for the event by dialogflow
        eventParams = dialogflow.types.struct_pb2.Struct()
        for i in params:
            eventParams[i] = params[i]
        event_input = dialogflow.types.EventInput(
            name = event, parameters = eventParams, language_code = self.langCode)
        # the payload would be sent to the webhook server in case
        # the matched intent for this event has fulfillment enabled
        payload = dialogflow.types.struct_pb2.Struct()
        for i in webhookPayload:
            payload[i] = webhookPayload[i]
        query_params = {"payload": payload}
        query_input = dialogflow.types.QueryInput(event = event_input)
        response = self.session_client.detect_intent(
        session = self.session, query_input=query_input, query_params = query_params)
        return response.query_result.fulfillment_text
    def sendMessage(self, msg, webhookPayload={}):
        text_input = dialogflow.types.TextInput(
            text=msg, language_code=self.langCode)
        payload = dialogflow.types.struct_pb2.Struct()
        for i in webhookPayload:
            payload[i] = webhookPayload[i]
        query_params = {"payload": payload}
        query_input = dialogflow.types.QueryInput(text=text_input)
        response = self.session_client.detect_intent(
            session=self.session, query_input=query_input, query_params=query_params)
        return response.query_result.fulfillment_text
