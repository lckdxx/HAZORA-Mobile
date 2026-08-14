package com.hazora.app.ui.incidents;

import java.io.Serializable;

public class Incident implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private String cameraId;
    private String time;
    private String site;
    private String status;
    private String severity;
    private String description;

    public Incident(String title, String cameraId, String time, String site, String status, String severity, String description) {
        this.title = title;
        this.cameraId = cameraId;
        this.time = time;
        this.site = site;
        this.status = status;
        this.severity = severity;
        this.description = description;
    }

    public String getTitle() { return title; }
    public String getCameraId() { return cameraId; }
    public String getTime() { return time; }
    public String getSite() { return site; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSeverity() { return severity; }
    public String getDescription() { return description; }
}
