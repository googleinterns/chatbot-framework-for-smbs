# Chatbot

## Prerequisites:
  - Java 1.8
  - Spring Boot 2.3.0
  - [protoc](https://github.com/protocolbuffers/protobuf/tree/master/src) compiler
  - [gcloud cli](https://cloud.google.com/sdk/install)
  - Keys for a service account with the following enabled:
    - Hangouts Chat API
    - Dialogflow API
    - Cloud Pub/Sub API

## Steps to run the chatbot
- Configure the server address for the cloud services used.
    -  `<base-url>` for hangouts chat
    - `<base-url>/dgf` for dialogflow fulfillments
    - `<base-url>/pubsub` for pubsub push subscriber
- Add the service account key to `services/src/main/resources/service-acct.json`
- Set environment variables
        
        export projectID=<projectID>
        export GOOGLE_APPLICATION_CREDENTIALS=<path-to-credentials-file>
        export projectNumber=<projectNumber>
        export subscriptionID=<pubsub-subscriberID>
        export topicID=<pubsub-topic>
        export pubsubAudience=<subscription-audience>
        export dialogflowAuthToken=<dialogflowAuthToken>

- Start the chat service

        ./mvnw spring-boot:run
- To trigger events, push messages to pubsub

        gcloud pubsub topics publish test-topic --message "TriggerEvent" --attribute event=SUGGEST_CATEGORY_CHANGE,suggestedCategory=cafe,chatClient=HANGOUTS,userID=<userID>

