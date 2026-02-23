package com.bankingapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bankingapp.R;
import com.bankingapp.database.DatabaseHelper;
import com.bankingapp.models.User;
import com.bankingapp.utils.PasswordUtils;
import com.bankingapp.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────
    private EditText    etUsername;
    private EditText    etPassword;
    private CheckBox    chkRememberMe;
    private Button      btnLogin;
    private TextView    tvRegisterLink;

    // ── Helpers ───────────────────────────────────────────────────────────
    private DatabaseHelper db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set title in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Login");
        }

        // ── Initialize helpers ─────────────────────────────────────────────
        db             = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        // ── Bind Views ─────────────────────────────────────────────────────
        etUsername     = findViewById(R.id.etUsername);
        etPassword     = findViewById(R.id.etPassword);
        chkRememberMe  = findViewById(R.id.chkRememberMe);
        btnLogin       = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);

        // ── Login Button Click ─────────────────────────────────────────────
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        // ── Register Link Click ────────────────────────────────────────────
        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this,
                        RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    // ── Core Login Logic ──────────────────────────────────────────────────
    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // ── Step 1: Validate inputs ────────────────────────────────────────
        String usernameError = PasswordUtils.validateUsername(username);
        if (usernameError != null) {
            etUsername.setError(usernameError);
            etUsername.requestFocus();
            return;
        }

        String passwordError = PasswordUtils.validatePassword(password);
        if (passwordError != null) {
            etPassword.setError(passwordError);
            etPassword.requestFocus();
            return;
        }

        // ── Step 2: Query database ─────────────────────────────────────────
        User user = db.loginUser(username, password);

        // ── Step 3: Handle result ─────────────────────────────────────────
        if (user != null) {
            // SUCCESS: create session and go to Dashboard
            sessionManager.createSession(
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getAccountNo(),
                    user.getPhone()
            );

            Toast.makeText(this,
                    "Welcome, " + user.getFullName() + "!",
                    Toast.LENGTH_SHORT).show();

            // Navigate to Dashboard
            Intent intent = new Intent(LoginActivity.this,
                    DashboardActivity.class);
            // Clear back stack so back button doesn't return to login
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        } else {
            // FAILURE: show error dialog
            showLoginErrorDialog();
        }
    }

    // ── Error Dialog (NEGATIVE dialog — required by syllabus) ─────────────
    private void showLoginErrorDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Login Failed")
                .setMessage(getString(R.string.err_login_failed))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton("Try Again", (dialog, which) -> {
                    // Clear password field and refocus
                    etPassword.setText("");
                    etPassword.requestFocus();
                    dialog.dismiss();
                })
                .show();
    }

    // ── Back Press: exit app with confirmation ────────────────────────────
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Exit",   (d, w) -> finishAffinity())
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }
}
