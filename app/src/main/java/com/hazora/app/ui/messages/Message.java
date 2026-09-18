package com.hazora.app.ui.messages;

public class Message {
    private String id;
    private String sender;
    private String senderEmail;
    private String senderId;
    private String preview;
    private String body;
    private String time;
    private String imageUrl;
    private boolean unread;
    private boolean isMine;

    public Message(String id, String sender, String senderEmail, String senderId, String preview,
                   String body, String time, boolean unread, boolean isMine, String imageUrl) {
        this.id = id;
        this.sender = sender;
        this.senderEmail = senderEmail;
        this.senderId = senderId;
        this.preview = preview;
        this.body = body;
        this.time = time;
        this.unread = unread;
        this.isMine = isMine;
        this.imageUrl = imageUrl;
    }

    // Getters
    public String getId() { return id; }
    public String getSender() { return sender; }
    public String getSenderEmail() { return senderEmail; }
    public String getSenderId() { return senderId; }
    public String getPreview() { return preview; }
    public String getBody() { return body; }
    public String getTime() { return time; }
    public boolean isUnread() { return unread; }
    public boolean isMine() { return isMine; }
    public String getImageUrl() { return imageUrl; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setSender(String sender) { this.sender = sender; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setPreview(String preview) { this.preview = preview; }
    public void setBody(String body) { this.body = body; }
    public void setTime(String time) { this.time = time; }
    public void setUnread(boolean unread) { this.unread = unread; }
    public void setMine(boolean mine) { isMine = mine; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
