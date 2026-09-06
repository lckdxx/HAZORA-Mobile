package com.hazora.app.ui.messages;

public class Message {
    private String id;
    private String sender;
    private String role;
    private String subject;
    private String preview;
    private String body;
    private String time;
    private boolean unread;

    public Message(String id, String sender, String role, String subject, String preview,
                   String body, String time, boolean unread) {
        this.id = id;
        this.sender = sender;
        this.role = role;
        this.subject = subject;
        this.preview = preview;
        this.body = body;
        this.time = time;
        this.unread = unread;
    }

    public String getId() { return id; }
    public String getSender() { return sender; }
    public String getRole() { return role; }
    public String getSubject() { return subject; }
    public String getPreview() { return preview; }
    public String getBody() { return body; }
    public String getTime() { return time; }
    public boolean isUnread() { return unread; }

    public void setId(String id) { this.id = id; }
    public void setSender(String sender) { this.sender = sender; }
    public void setRole(String role) { this.role = role; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setPreview(String preview) { this.preview = preview; }
    public void setBody(String body) { this.body = body; }
    public void setTime(String time) { this.time = time; }
    public void setUnread(boolean unread) { this.unread = unread; }
}