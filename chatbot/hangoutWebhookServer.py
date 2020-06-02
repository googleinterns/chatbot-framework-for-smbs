#!/usr/bin/env python3
"""
Chat server for interfacing between clients and dialogflow
"""
from flask import Flask, request, json
import psycopg2
import dialogflowConversation as C
import hangoutsApi as H
import os

app = Flask(__name__)
projectID = os.getenv("projectID")
dbName = os.getenv("database")
dbUser = os.getenv("databaseUser")
dbAddress = os.getenv("databaseAddress")
dbPassword = os.getenv("databasePassword")

def getSpaceID(userID):
    try:
        conn = psycopg2.connect(database = dbName, user = dbUser, host = dbAddress, password=dbPassword)
        cur = conn.cursor()
        cur.execute("select * from spaces where userID='" + userID + "';")
        rows = cur.fetchall()
        return rows[0][0]
    except (Exception, psycopg2.Error) as error :
        print ("Error while connecting to PostgreSQL", error)
    finally:
        if(conn):
            cur.close()
            conn.close()
    return ""

def addUser(spaceID, userID):
    try:
        conn = psycopg2.connect(database = dbName, user = dbUser, host = dbAddress, password=dbPassword)
        cur = conn.cursor()
        print("insert into spaces (userID, spaceID) values('" + userID + "','" + str(spaceID) + "');")
        cur.execute("insert into spaces (userID, spaceID) values('" + userID + "','" + str(spaceID) + "');")
        conn.commit()
    except (Exception, psycopg2.Error) as error :
        print ("Error while connecting to PostgreSQL", error)
    finally:
        if(conn):
            cur.close()
            conn.close()

def triggerEvent(userID, eventName, params):
    spaceID = getSpaceID(userID)
    sessionID = spaceID
    x = C.Conversation(projectID = projectID, sessionID = sessionID)
    # fire dialogflow event
    response = x.triggerEvent(eventName, params)
    # get response from dialogflow and send it to the user hangouts using the spaceID
    H.sendMessageToSpace("spaces/" + spaceID, response)

@app.route('/', methods=['POST'])
def on_event():
  """Handles an event from Hangouts Chat."""
  event = request.get_json()
#   print(request.headers)
#   print("Header Done")
  print(json.dumps(event, indent=2))
  spaceID = event["space"]["name"][7:]
  userID = event["user"]["name"][6:]
  if event['type'] == 'ADDED_TO_SPACE':
    addUser(spaceID, userID)
    text = "Thanks for adding me"
    #text = 'Thanks for adding me to "%s"!' % event['space']['displayName']
  elif event['type'] == 'MESSAGE':
    sessionID = getSpaceID(userID)
    needToUpdate = False
    x = C.Conversation(projectID = projectID, sessionID = sessionID)
    userMessage = event["message"]["text"]
    text = x.sendMessage(userMessage)
  else:
      return json.jsonify({'text':""})
  return json.jsonify({'text': text})

@app.route('/notifyEvent', methods=['POST'])
def notifyEvent():
    """
    Handles requests from backend to trigger events
    for certain users
    """
    data = request.get_json()
    print(data)
    userID = data["userID"]
    event = data["event"]
    eventParams = data["params"]
    triggerEvent(userID, event, eventParams)
    return("Done")

if __name__ == '__main__':  
  app.run(port=8085, debug=True)