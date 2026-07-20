package com.hazora.app.ui.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hazora.app.R;
import com.hazora.app.ui.login.LoginActivity;

/** Presents the branded onboarding entry point before authentication. */
public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        MaterialButton getStartedButton = findViewById(R.id.button_get_started);
        getStartedButton.setOnClickListener(view -> openLogin());

        playEntryAnimations();
    }

    private void playEntryAnimations() {
        View logoGlassCard = findViewById(R.id.layout_logo_glass);
        View titleTextView = findViewById(R.id.text_welcome_title);
        View subtitleTextView = findViewById(R.id.text_welcome_subtitle);
        View pageIndicator = findViewById(R.id.layout_page_indicator);
        MaterialButton getStartedButton = findViewById(R.id.button_get_started);

        logoGlassCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.welcome_logo_enter));
        titleTextView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.welcome_title_enter));
        subtitleTextView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.welcome_subtitle_enter));
        pageIndicator.startAnimation(AnimationUtils.loadAnimation(this, R.anim.welcome_indicator_enter));
        getStartedButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.welcome_button_enter));
    }

    private void openLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
