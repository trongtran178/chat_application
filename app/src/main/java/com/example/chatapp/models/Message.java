package com.example.chatapp.models;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class Message {


    private String content;
    private String sender; // sender is email
    private Date createdAt;

    public Message() {
    }

    public Message(String content, String sender, Date createdAt) {
        this.content = content;
        this.sender = sender;
        this.createdAt = createdAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
