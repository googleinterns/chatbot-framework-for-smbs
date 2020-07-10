# intent-gen

## Prerequisites:
  - Java 1.8
  - Maven
  - [protoc](https://github.com/protocolbuffers/protobuf/tree/master/src) compiler
  - Keys for a service account with the Dialogflow API enabled
 

## Steps to run intent-gen
- Add the service account key to `intentgen/src/main/resources/service-acct.json`
- Set the projectID environment

        export projectID=<projectID>
        export GOOGLE_APPLICATION_CREDENTIALS=<path-to-credentials-file>
- Add the protxt file describing the usecase to `intentgen/src/main/resources`
- Generate the intents

        mvn exec:java -D exec.mainClass=com.chatbot.IntentGenerator -D exec.args="-f usecase.prototxt"

## Usecase description
The use cases must be described in a prototxt file for message described at [UseCase.proto](https://github.com/googleinterns/chatbot-framework-for-smbs/blob/master/intentgen/src/main/proto/UseCase.proto)
- `intent_name` - The name of the intent to be created
- `intent_type` - Used to categorise the intent as yes/no, to avoid having to add the training phrases for them
- `successors` - The list of possible intents which could follow the current intent
- `variables` - The list of variables other than the ones used in the training phrases. These are used to keep the variable alive in the intermediate contexts where they are not used.
- `trainingPhrases` - The list of training phrases for the intent. The syntax for encoding entities into training phrases:

        text |entity-type;alisas;value| some more text

- `responses` - The list of responses to provide when the intent is matched.
- `events` - This list of events that could match the intent.
- `fulfillment_enabled` - Used to determine if fulfillments are enabled for the intent.
