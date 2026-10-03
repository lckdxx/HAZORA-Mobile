package com.hazora.app.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hazora.app.R;
import com.hazora.app.auth.AuthRepository;
import com.hazora.app.ui.dashboard.DashboardActivity;
import com.hazora.app.ui.welcome.WelcomeActivity;

/** Displays the branded launch experience before handing off to authentication. */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DISPLAY_DURATION_MS = 2000L;

    private final Handler navigationHandler = new Handler(Looper.getMainLooper());
    private final Runnable navigateToWelcomeRunnable = this::navigateToWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        startLogoFadeInAnimation();
        navigationHandler.postDelayed(navigateToWelcomeRunnable, SPLASH_DISPLAY_DURATION_MS);
    }

    private void startLogoFadeInAnimation() {
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_logo_enter);
        ImageView logoImageView = findViewById(R.id.image_hazora_logo);
        logoImageView.startAnimation(fadeInAnimation);
    }

    private void navigateToWelcome() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            openWelcome();
            return;
        }
        if (!user.isEmailVerified()) {
            FirebaseAuth.getInstance().signOut();
            openWelcome();
            return;
        }

        new AuthRepository().ensureMobileAccess(user, new AuthRepository.AuthenticationCallback() {
            @Override
            public void onSuccess(FirebaseUser verifiedUser) {
                if (isFinishing()) return;
                startActivity(new Intent(SplashActivity.this, DashboardActivity.class));
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing()) return;
                Toast.makeText(SplashActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                openWelcome();
            }
        });
    }

    private void openWelcome() {
        startActivity(new Intent(this, WelcomeActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        navigationHandler.removeCallbacks(navigateToWelcomeRunnable);
        super.onDestroy();
    }
}
