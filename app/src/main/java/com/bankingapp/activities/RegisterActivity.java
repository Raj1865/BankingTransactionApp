package com.bankingapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bankingapp.R;
import com.bankingapp.database.DatabaseHelper;
import com.bankingapp.utils.PasswordUtils;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etRegUsername, etPhone, etRegPassword;
    private Button   btnCreateAccount;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Create Account");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = new DatabaseHelper(this);

        // Bind Views
        etFullName       = findViewById(R.id.etFullName);
        etRegUsername    = findViewById(R.id.etRegUsername);
        etPhone          = findViewById(R.id.etPhone);
        etRegPassword    = findViewById(R.id.etRegPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegister();
            }
        });
    }

    private void attemptRegister() {
        String fullName  = etFullName.getText().toString().trim();
        String username  = etRegUsername.getText().toString().trim();
        String phone     = etPhone.getText().toString().trim();
        String password  = etRegPassword.getText().toString().trim();

        // ── Validate all fields ────────────────────────────────────────────
        if (fullName.isEmpty()) {
            etFullName.setError(getString(R.string.err_name_empty));
            etFullName.requestFocus(); return;
        }

        String usernameError = PasswordUtils.validateUsername(username);
        if (usernameError != null) {
            etRegUsername.setError(usernameError);
            etRegUsername.requestFocus(); return;
        }

        if (!phone.matches("[0-9]{10}")) {
            etPhone.setError(getString(R.string.err_phone_invalid));
            etPhone.requestFocus(); return;
        }

        String passwordError = PasswordUtils.validatePassword(password);
        if (passwordError != null) {
            etRegPassword.setError(passwordError);
            etRegPassword.requestFocus(); return;
        }

        // ── Check duplicates ──────────────────────────────────────────────
        if (db.isUsernameExists(username)) {
            etRegUsername.setError(getString(R.string.err_username_taken));
            etRegUsername.requestFocus(); return;
        }

        if (db.isPhoneExists(phone)) {
            etPhone.setError(getString(R.string.err_phone_taken));
            etPhone.requestFocus(); return;
        }

        // ── Register in DB ────────────────────────────────────────────────
        boolean success = db.registerUser(username, password, fullName, phone);

        if (success) {
            // POSITIVE dialog — required by syllabus
            new AlertDialog.Builder(this)
                    .setTitle("Account Created!")
                    .setMessage(getString(R.string.msg_register_success))
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setPositiveButton("Login Now", (dialog, which) -> {
                        Intent intent = new Intent(RegisterActivity.this,
                                LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        } else {
            Toast.makeText(this, "Registration failed. Please try again.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ── Back arrow in action bar ──────────────────────────────────────────
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
