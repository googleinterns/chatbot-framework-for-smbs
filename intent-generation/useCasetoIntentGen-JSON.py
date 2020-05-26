import json
import dialogflow_v2 as dialogflow
import os
import sys

# assumed format of the training phrases:
# "some text |type;alias;value| some more text |type;alias;value|"
# This function returns the dict for a single training phrase
def getPhraseDict(text):
    # a single sentence has to be split up into several 'data' objects
    phraseDict = {"data":[], "isTemplate": False, "count": 0, "updated": 0}
    parts = text.split("|")
    for i in parts:
        x = {}
        # if ; is present in any part, that part must be
        # a parameter
        if ";" in i:
            varD = i.split(";")
            x["text"] = varD[2]
            x["alias"] = varD[1]
            x["meta"] = varD[0]
            x["userDefined"] = True
        else:
            x["text"] = i
            x["userDefined"] = False
        phraseDict["data"].append(x)
    return phraseDict

def getIntentDict(name, inputContexts, outputContexts, params):
    with open("templateIntent.json") as f:
        intentDict = json.load(f)
    intentDict["name"] = name
    intentDict["contexts"] = inputContexts
    # ouput context population
    affectedContexts = []
    for i in range(len(outputContexts)):
        temp = {"name": outputContexts[i], "parameters":{}, "lifespan": 1}
        affectedContexts.append(temp)
    intentDict["responses"][0]["affectedContexts"] = affectedContexts
    # parameter population
    parameters = []
    for i in range(len(params)):
        # using default datatype as '@sys.any'
        # not adding in any default value for the parameter
        temp = {"required": False, "dataType": "@sys.any",
        "name": params[i], "value": "$" + params[i],
        "promptMessages": [], "noMatchPromptMessages": [], "noInputPromptMessages":[],
        "outputDialogContexts":[], "defaultValue": name + "Context" + params[i], "isList": False}
        parameters.append(temp)
    intentDict["responses"][0]["parameters"] = parameters
    return intentDict

# the file describing the use cases
with open(sys.argv[1]) as inp:
    useCaseDict = json.load(inp)

# the directory where the output json files must be stored
outputDir = sys.argv[2]

# intent json file creation
for i in useCaseDict:
    # for our use cases the only input context is the 
    # name of the intent + "context"
    inputContexts = [i + "Context"]
    outputContexts = []
    # output contexts will be the input contexts
    # of all the intents which can be successors of
    # the current intent
    for j in useCaseDict[i]["successors"]:
        outputContexts.append(j + "Context")
    params = useCaseDict[i]["variables"]

    name = i
    intentDict = getIntentDict(name, inputContexts, outputContexts, params)
    compeltePath = os.path.join(outputDir, i+".json")
    print(compeltePath)
    # this file will contain the description of the intent
    with open(compeltePath, "w") as outputFile:
        json.dump(intentDict, outputFile, indent = 4)

# training phrases for intents are stored in a seperate file
for i in useCaseDict:
    if "trainingPhrases" in useCaseDict[i]:
        fileName = i + "_usersays_en.json"
        trainingPhrasesDict = []
        for j in useCaseDict[i]["trainingPhrases"]:
            trainingPhrasesDict.append(getPhraseDict(j))
        compeltePath = os.path.join(outputDir, fileName)
        print(compeltePath)
        with open(compeltePath, "w") as o:
            json.dump(trainingPhrasesDict, o, indent=4)