package com.bankingapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bankingapp.R;
import com.bankingapp.adapters.TransactionAdapter;
import com.bankingapp.database.DatabaseHelper;
import com.bankingapp.fragments.PayBillFragment;
import com.bankingapp.fragments.SendMoneyFragment;
import com.bankingapp.models.Transaction;
import com.bankingapp.utils.DateTimeHelper;
import com.bankingapp.utils.LocationHelper;
import com.bankingapp.utils.NotificationHelper;
import com.bankingapp.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private static final int REQ_LOCATION = 101;
    private static final int REQ_NOTIF    = 100;

    // ── Views ─────────────────────────────────────────────────────────────
    private TextView tvUserName, tvAccountNo, tvBalance;
    private TextView tvLocation, tvDateTime;
    private Button   btnSendMoney, btnPayBill, btnHistory, btnInsights;
    private Button   btnToggleBalance;
    private ListView listRecentTransactions;
    private View     layoutDefaultContent;

    // ── State ─────────────────────────────────────────────────────────────
    private boolean isBalanceVisible = false;
    private double  currentBalance   = 0.0;

    // ── Helpers ───────────────────────────────────────────────────────────
    private DatabaseHelper     db;
    private SessionManager     session;
    private LocationHelper     locationHelper;
    private TransactionAdapter adapter;
    private NotificationHelper notifHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Dashboard");

        db      = new DatabaseHelper(this);
        session = new SessionManager(this);

        bindViews();
        loadUserInfo();
        refreshBalance();
        loadRecentTransactions();
        setButtonListeners();

        notifHelper    = new NotificationHelper(this);
        locationHelper = new LocationHelper(this);

        requestPermissionsIfNeeded();
        updateDateTimeDisplay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBalance();
        loadRecentTransactions();
        showDefaultContent();
        updateDateTimeDisplay();
        if (locationHelper != null) locationHelper.startUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationHelper != null) locationHelper.stopUpdates();
    }

    // ═════════════════════════════════════════════════════════════════════
    // PERMISSIONS
    // ═════════════════════════════════════════════════════════════════════

    private void requestPermissionsIfNeeded() {
        List<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    needed.toArray(new String[0]), REQ_LOCATION);
        } else {
            if (locationHelper != null) locationHelper.startUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (requestCode == REQ_LOCATION) {
            if (granted) {
                locationHelper = new LocationHelper(this);
                locationHelper.startUpdates();
                updateDateTimeDisplay();
            } else {
                Toast.makeText(this,
                        "Location permission denied — transactions saved without GPS.",
                        Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQ_NOTIF && !granted) {
            Toast.makeText(this,
                    "Notification permission denied — alerts will not appear.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // VIEW SETUP
    // ═════════════════════════════════════════════════════════════════════

    private void bindViews() {
        tvUserName             = findViewById(R.id.tvUserName);
        tvAccountNo            = findViewById(R.id.tvAccountNo);
        tvBalance              = findViewById(R.id.tvBalance);
        tvLocation             = findViewById(R.id.tvLocation);
        tvDateTime             = findViewById(R.id.tvDateTime);
        btnSendMoney           = findViewById(R.id.btnSendMoney);
        btnPayBill             = findViewById(R.id.btnPayBill);
        btnHistory             = findViewById(R.id.btnHistory);
        btnInsights            = findViewById(R.id.btnInsights);
        btnToggleBalance       = findViewById(R.id.btnToggleBalance);
        listRecentTransactions = findViewById(R.id.listRecentTransactions);
        layoutDefaultContent   = findViewById(R.id.layoutDefaultContent);
    }

    private void updateDateTimeDisplay() {
        if (tvDateTime != null)
            tvDateTime.setText(DateTimeHelper.getDashboardHeader());
        new android.os.Handler().postDelayed(() -> {
            if (tvLocation == null) return;
            if (locationHelper != null && locationHelper.hasValidLocation())
                tvLocation.setText(getString(R.string.location_prefix)
                        + " " + locationHelper.getFormattedLocation());
            else
                tvLocation.setText(getString(R.string.location_unavailable));
        }, 3000);
    }

    private void loadUserInfo() {
        tvUserName.setText(session.getFullName());
        tvAccountNo.setText("A/C No: " + session.getAccountNo());
        tvBalance.setText("Balance: ₹ ----");
    }

    private void refreshBalance() {
        currentBalance = db.getBalance(session.getUserId());
        if (isBalanceVisible)
            tvBalance.setText("Balance: ₹ " + String.format("%,.2f", currentBalance));
    }

    private void loadRecentTransactions() {
        List<Transaction> recent = db.getRecentTransactions(session.getUserId(), 5);
        if (adapter == null) {
            adapter = new TransactionAdapter(this, recent);
            listRecentTransactions.setAdapter(adapter);
        } else {
            adapter.clear();
            adapter.addAll(recent);
            adapter.notifyDataSetChanged();
        }
    }

    private void showDefaultContent() {
        if (layoutDefaultContent != null)
            layoutDefaultContent.setVisibility(View.VISIBLE);
    }

    // ═════════════════════════════════════════════════════════════════════
    // BUTTON LISTENERS
    // ═════════════════════════════════════════════════════════════════════

    private void setButtonListeners() {
        btnToggleBalance.setOnClickListener(v -> {
            isBalanceVisible = !isBalanceVisible;
            if (isBalanceVisible) {
                tvBalance.setText("Balance: ₹ " + String.format("%,.2f", currentBalance));
                btnToggleBalance.setText("Hide");
            } else {
                tvBalance.setText("Balance: ₹ ----");
                btnToggleBalance.setText("Show");
            }
        });
        btnSendMoney.setOnClickListener(v -> loadFragment(new SendMoneyFragment()));
        btnPayBill.setOnClickListener(v -> loadFragment(new PayBillFragment()));
        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, TransactionActivity.class)));
        btnInsights.setOnClickListener(v ->
                startActivity(new Intent(this, InsightsActivity.class)));
    }

    // ═════════════════════════════════════════════════════════════════════
    // FRAGMENT MANAGEMENT
    // ═════════════════════════════════════════════════════════════════════

    public void loadFragment(Fragment fragment) {
        layoutDefaultContent.setVisibility(View.GONE);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragmentContainer, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    public void onTransactionComplete() {
        getSupportFragmentManager().popBackStack();
        layoutDefaultContent.setVisibility(View.VISIBLE);
        refreshBalance();
        loadRecentTransactions();
    }

    /**
     * Called when a Send/Pay flow is cancelled by the user (e.g. pressing a
     * "Cancel" button in the fragment) and no new transaction was created.
     * Restores the default dashboard content without reloading data.
     */
    public void onTransactionFlowCancelled() {
        getSupportFragmentManager().popBackStack();
        if (layoutDefaultContent != null) {
            layoutDefaultContent.setVisibility(View.VISIBLE);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // OPTIONS MENU
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_history) {
            startActivity(new Intent(this, TransactionActivity.class));
            return true;
        }
        if (id == R.id.action_insights) {
            startActivity(new Intent(this, InsightsActivity.class));
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_logout) {
            showLogoutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ═════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dlg_logout_title))
                .setMessage(getString(R.string.dlg_logout_msg))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(getString(R.string.dlg_yes), (d, w) -> {
                    session.clearSession();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .setNegativeButton(getString(R.string.dlg_no), (d, w) -> d.dismiss())
                .setNeutralButton("Lock", (d, w) -> {
                    Intent i = new Intent(this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        // Check backstack BEFORE calling super — calling super first would
        // auto-pop the stack, making getBackStackEntryCount() always return 0
        // and causing the "Exit App" dialog to appear unexpectedly mid-flow.
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            layoutDefaultContent.setVisibility(View.VISIBLE);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Exit App")
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("Exit",   (d, w) -> finishAffinity())
                    .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                    .show();
        }
        // Do NOT call super.onBackPressed() here — it double-pops the stack.
    }
}