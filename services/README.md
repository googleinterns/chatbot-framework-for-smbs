# Chatbot

## Prerequisites:
  - Java 1.8
  - Spring Boot 2.3.0
  - [protoc](https://github.com/protocolbuffers/protobuf/tree/master/src) compiler
  - [gcloud cli](https://cloud.google.com/sdk/install)
  - Create a GCP account and enable
    - Hangouts Chat API
    - Dialogflow API
    - Cloud Pub/Sub API
- Create a topic in Cloud Pub/Sub and configure a push subscriber for it.
- Create a service account with access to the above mentioned API and download its credentials file.

## Steps to run the chatbot
- Configure the server address for the cloud services used.
    - `<base-url>` for hangouts chat
    - `<base-url>/dgf` for dialogflow fulfillments
    - `<base-url>/pubsub` for pubsub push subscriber
- Add the service account key to `services/src/main/resources/service-acct.json`
- Set environment variables
        
        export projectid=<projectID>
        export GOOGLE_APPLICATION_CREDENTIALS=<path-to-credentials-file>
        export projectnumber=<projectNumber>
        export subscriptionid=<pubsub-subscriberID>
        export topicid=<pubsub-topic>
        export pubsubAudience=<subscription-audience>
        export dialogflowauthtoken=<dialogflowAuthToken>

- Start the chat service

        ./mvnw spring-boot:run
- To trigger events, push messages to pubsub

        gcloud pubsub topics publish test-topic --message "TriggerEvent" --attribute event=SUGGEST_CATEGORY_CHANGE,suggestedCategory=cafe,chatClient=HANGOUTS,userID=<userID>

## Using ngrok for testing
- Download binary from [ngrok](https://ngrok.com/download).
- Launch server using

        ./ngrok http 9000
- Use the generated https endpoint for configuring cloud services .