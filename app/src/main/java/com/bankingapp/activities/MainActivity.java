package com.bankingapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.bankingapp.R;
import com.bankingapp.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    // Splash display time = 2 seconds
    private static final int SPLASH_DURATION_MS = 2000;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide the action bar on splash screen
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        sessionManager = new SessionManager(this);

        // ── Use Handler to delay navigation by SPLASH_DURATION_MS ──────────
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                navigateToNext();
            }
        }, SPLASH_DURATION_MS);
    }

    private void navigateToNext() {
        Intent intent;

        if (sessionManager.isLoggedIn()) {
            // User is already logged in → go to Dashboard
            intent = new Intent(MainActivity.this, DashboardActivity.class);
        } else {
            // User not logged in → go to Login
            intent = new Intent(MainActivity.this, LoginActivity.class);
        }

        startActivity(intent);

        // Finish MainActivity so user cannot press Back to come to Splash
        finish();
    }

    // ── Prevent back press on Splash screen ────────────────────────────────
    @Override
    public void onBackPressed() {
        // Do nothing — block back during splash
    }
}
