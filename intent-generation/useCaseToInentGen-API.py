import json
import dialogflow_v2 as dialogflow
from collections import defaultdict
import os
from dotenv import load_dotenv

load_dotenv()
projectID = os.getenv("projectID")

# assumed format of the training phrases:
# "some text |type;alias;value| some more text |type;alias;value|"
# This function returns the dict for a single training phrase
def getTrainingPhrase(text):
    textParts = text.split("|")
    parts = []
    # each training phrase is reprsented as a list of "parts"
    # with each part being the maximal string before a parameter
    # or a parameter itself
    for i in textParts:
        # if ; is present in any part, that part must be
        # a parameter
        if ";" in i:
            varD = i.split(";")
            x = dialogflow.types.Intent.TrainingPhrase.Part(text = varD[2],
            alias=varD[1], entity_type=varD[0], user_defined=True)
        else:
            x = dialogflow.types.Intent.TrainingPhrase.Part(text = i)
        # append this dict to the phrase dict
        parts.append(x)    
    trainingPhrase = dialogflow.types.Intent.TrainingPhrase(parts=parts)
    return trainingPhrase

def getParameters(variables, intentName):
    parameters = []
    for i in variables:
        # the most general default type for the parameters would be sys.any
        # the default value is assumed to be present in the context
        parameters.append(dialogflow.types.Intent.Parameter(value="$"+i, display_name=i,
        default_value="#"+intentName+"."+i, entity_type_display_name="@sys.any", mandatory=False))
    return parameters

def getResponses(responses):
    text = dialogflow.types.Intent.Message.Text(text=responses)
    return [dialogflow.types.Intent.Message(text=text)]

with open("useCase.json") as inp:
    useCaseDict = json.load(inp)

intent_client = dialogflow.IntentsClient()
parent = intent_client.project_agent_path(projectID)
# the list of intents already present is needed since if an intent is already
# present then the create_intent call would fail and an update intent call would
# need to be called which requires the UUID of the intent
response = intent_client.list_intents(parent)
intentNameDict = {i.display_name: i.name for i in response}
print(intentNameDict)
for i in useCaseDict:
    trainingPhrasesList = []
    if "trainingPhrases" in useCaseDict[i]:
        for j in useCaseDict[i]["trainingPhrases"]:
            trainingPhrasesList.append(getTrainingPhrase(j))
        print(trainingPhrasesList)
    # there is only one input context required which is the identifier of the intent, any
    # intent which we would like to precede this intent will have this context as one of
    # its output context
    inputContexts = ["projects/" + projectID + "/agent/sessions/-/contexts/" + i + "Context"]
    # the input contexts of all the successors to an intent have to be present in the output
    # context list of the intent
    outputContexts = [dialogflow.types.Context(name = "projects/" + projectID + "/agent/sessions/-/contexts/" + j + "Context", lifespan_count=1) for j in useCaseDict[i]["successors"]]
    # these are the parameters which do not show up in the training phrases but could perhaps
    # be used for a parametric response where there values could be set using a detect intent
    # request
    parameters = getParameters(useCaseDict[i]["variables"], i)
    responses = getResponses(useCaseDict[i]["responses"])
    if i not in intentNameDict:
        newIntent = dialogflow.types.Intent(display_name = i, input_context_names=inputContexts, training_phrases=trainingPhrasesList, output_contexts=outputContexts, parameters=parameters, messages=responses)
        response = intent_client.create_intent(parent, newIntent)
    else:
        newIntent = dialogflow.types.Intent(name = intentNameDict[i], display_name = i, input_context_names=inputContexts, training_phrases=trainingPhrasesList, output_contexts=outputContexts, parameters=parameters, messages=responses)
        response = intent_client.update_intent(newIntent)