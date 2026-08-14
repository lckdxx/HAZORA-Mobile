package com.hazora.app.ui.incidents;

import java.util.ArrayList;
import java.util.List;

public class IncidentRepository {

    private static final List<Incident> INCIDENTS = new ArrayList<>();

    static {
        INCIDENTS.add(new Incident(
                "Missing Hard Hat Detected",
                "CAM-04",
                "09:14 AM",
                "Construction Site A — Zone B",
                "New",
                "High",
                "A worker was detected without a hard hat."));

        INCIDENTS.add(new Incident(
                "Missing Safety Vest",
                "CAM-02",
                "08:52 AM",
                "Construction Site A — Zone B",
                "Acknowledged",
                "Medium",
                "A worker was detected without a required safety vest."));

        INCIDENTS.add(new Incident(
                "Missing Safety Shoes",
                "CAM-07",
                "14:30 PM",
                "Construction Site A — Zone B",
                "Resolved",
                "Medium",
                "A worker was detected without required safety shoes."));

        INCIDENTS.add(new Incident(
                "Missing Hard Hat Detected",
                "CAM-03",
                "11:26 AM",
                "Construction Site A — Zone B",
                "Acknowledged",
                "High",
                "A worker was detected without a hard hat in the monitored area."));

        INCIDENTS.add(new Incident(
                "Missing Safety Vest",
                "CAM-05",
                "03:42 PM",
                "Construction Site A — Zone B",
                "New",
                "Medium",
                "A worker was detected without a safety vest."));
    }

    public static List<Incident> getIncidents() {
        return INCIDENTS;
    }

    public static Incident getIncident(int index) {
        return INCIDENTS.get(index);
    }

    public static void updateStatus(int index, String status) {
        INCIDENTS.get(index).setStatus(status);
    }
}
