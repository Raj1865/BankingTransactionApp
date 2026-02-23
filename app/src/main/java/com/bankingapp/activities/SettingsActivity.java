package com.bankingapp.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bankingapp.R;
import com.bankingapp.utils.NotificationHelper;
import com.bankingapp.utils.SessionManager;

/**
 * SettingsActivity — lets the user toggle notification preferences
 * and view account info.
 *
 * Syllabus coverage: SharedPreferences (read + write), Switch widget,
 *                    AppCompatActivity lifecycle.
 */
public class SettingsActivity extends AppCompatActivity {

    // ── SharedPreferences key constants ──────────────────────────────────────
    public static final String PREFS_NAME              = "BankingAppPrefs";
    public static final String KEY_NOTIF_TRANSACTIONS  = "notif_transactions";
    public static final String KEY_NOTIF_ALERTS        = "notif_alerts";
    public static final String KEY_NOTIF_BILLS         = "notif_bills";

    private SharedPreferences prefs;
    private Switch switchTransactions, switchAlerts, switchBills;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Back arrow in ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        switchTransactions = findViewById(R.id.switchTransactions);
        switchAlerts       = findViewById(R.id.switchAlerts);
        switchBills        = findViewById(R.id.switchBills);

        // ── Load saved preferences ────────────────────────────────────────────
        switchTransactions.setChecked(prefs.getBoolean(KEY_NOTIF_TRANSACTIONS, true));
        switchAlerts      .setChecked(prefs.getBoolean(KEY_NOTIF_ALERTS,       true));
        switchBills       .setChecked(prefs.getBoolean(KEY_NOTIF_BILLS,        true));

        // ── Save on toggle ────────────────────────────────────────────────────
        switchTransactions.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean(KEY_NOTIF_TRANSACTIONS, checked).apply();
            showToast("Transaction notifications " + (checked ? "ON" : "OFF"));
        });

        switchAlerts.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean(KEY_NOTIF_ALERTS, checked).apply();
            showToast("Security alerts " + (checked ? "ON" : "OFF"));
        });

        switchBills.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean(KEY_NOTIF_BILLS, checked).apply();
            showToast("Bill notifications " + (checked ? "ON" : "OFF"));
        });
    }

    // ── Handle the back arrow press ──────────────────────────────────────────
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
