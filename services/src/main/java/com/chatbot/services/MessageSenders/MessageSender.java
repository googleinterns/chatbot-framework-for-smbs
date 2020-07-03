package com.chatbot.services.MessageSenders;

import java.io.IOException;

abstract class MessageSender {
    abstract void sendMessage(final String chatClientGeneratedID, final String msg)
        throws IOException;
}