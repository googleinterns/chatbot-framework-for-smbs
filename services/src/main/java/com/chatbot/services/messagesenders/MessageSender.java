package com.chatbot.services.messagesenders;

import java.io.IOException;

// class to simplify message dispatch for a given chat service

abstract class MessageSender {
    abstract void sendMessage(final String chatClientGeneratedID, final String msg)
        throws IOException;
}