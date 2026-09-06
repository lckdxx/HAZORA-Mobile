package com.hazora.app.ui.navigation;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hazora.app.R;
import com.hazora.app.ui.dashboard.DashboardActivity;
import com.hazora.app.ui.hazardscan.HazardScanActivity;
import com.hazora.app.ui.incidents.IncidentsActivity;
import com.hazora.app.ui.messages.MessagesActivity;
import com.hazora.app.ui.profile.ProfileActivity;

public final class BottomNavigationHelper {

    private BottomNavigationHelper() {
    }

    public static void bind(Activity activity, int selectedItemId) {
        BottomNavigationView navigation = activity.findViewById(R.id.shared_bottom_nav);
        View camera = activity.findViewById(R.id.shared_camera_fab);
        if (navigation == null || camera == null) {
            return;
        }

        navigation.setSelectedItemId(selectedItemId);
        navigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                open(activity, DashboardActivity.class, selectedItemId != R.id.nav_home);
            } else if (itemId == R.id.nav_incidents) {
                open(activity, IncidentsActivity.class, selectedItemId != R.id.nav_incidents);
            } else if (itemId == R.id.nav_messages) {
                open(activity, MessagesActivity.class, selectedItemId != R.id.nav_messages);
            } else if (itemId == R.id.nav_profile) {
                open(activity, ProfileActivity.class, selectedItemId != R.id.nav_profile);
            }
            return true;
        });

        camera.setOnClickListener(view -> open(activity, HazardScanActivity.class,
                !(activity instanceof HazardScanActivity)));
    }

    private static void open(Activity activity, Class<? extends Activity> target, boolean needed) {
        if (needed) {
            activity.startActivity(new Intent(activity, target));
        }
    }
}
