package com.hazora.app.ui.incidents;

import java.util.ArrayList;
import java.util.List;

public class IncidentRepository {

    private static final List<Incident> INCIDENTS = new ArrayList<>();

    public static List<Incident> getIncidents() {
        return INCIDENTS;
    }

    public static Incident getIncident(int index) {
        if (index >= 0 && index < INCIDENTS.size()) {
            return INCIDENTS.get(index);
        }
        return null;
    }

    public static void updateStatus(int index, String status) {
        if (index >= 0 && index < INCIDENTS.size()) {
            INCIDENTS.get(index).setStatus(status);
        }
    }

    public static void removeIncident(Incident incident) {
        INCIDENTS.remove(incident);
    }
}
