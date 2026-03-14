package com.ritesh.hoppeconnect.models;

public class ChatMessage {
    public String senderId;
    public String text;
    public long timestamp;
    public boolean isMine; // Used to decide which layout to show (sent vs received)

    public ChatMessage(String senderId, String text, long timestamp, boolean isMine) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
        this.isMine = isMine;
    }
}