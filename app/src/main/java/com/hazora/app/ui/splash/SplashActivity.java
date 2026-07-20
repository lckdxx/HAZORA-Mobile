package com.hazora.app.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hazora.app.R;
import com.hazora.app.ui.login.LoginActivity;

/** Displays the branded launch experience before handing off to authentication. */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DISPLAY_DURATION_MS = 2500L;

    private final Handler navigationHandler = new Handler(Looper.getMainLooper());
    private final Runnable navigateToLoginRunnable = this::navigateToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        startFadeInAnimation();
        navigationHandler.postDelayed(navigateToLoginRunnable, SPLASH_DISPLAY_DURATION_MS);
    }

    private void startFadeInAnimation() {
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        ImageView logoImageView = findViewById(R.id.image_hazora_logo);
        TextView appNameTextView = findViewById(R.id.text_app_name);
        TextView subtitleTextView = findViewById(R.id.text_app_subtitle);

        logoImageView.startAnimation(fadeInAnimation);
        appNameTextView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        subtitleTextView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        navigationHandler.removeCallbacks(navigateToLoginRunnable);
        super.onDestroy();
    }
}
