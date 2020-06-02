from httplib2 import Http
from oauth2client.service_account import ServiceAccountCredentials
from apiclient.discovery import build
from dotenv import load_dotenv
import os

load_dotenv()

GOOGLE_APPLICATION_CREDENTIALS = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
def sendMessageToSpace(spaceID, text):
    scopes = 'https://www.googleapis.com/auth/chat.bot'
    credentials = ServiceAccountCredentials.from_json_keyfile_name(GOOGLE_APPLICATION_CREDENTIALS, scopes)
    chat = build('chat', 'v1', http=credentials.authorize(Http()))
    resp = chat.spaces().messages().create(
        parent=spaceID, # use your space here
        body={'text': text}).execute()