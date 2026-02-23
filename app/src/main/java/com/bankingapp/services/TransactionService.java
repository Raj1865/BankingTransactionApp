package com.bankingapp.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

public class TransactionService extends Service {

    // ── Action constants (called from SendMoneyFragment) ──────────────────
    public static final String ACTION_PROCESS_TRANSACTION = "com.bankingapp.PROCESS_TXN";
    public static final String ACTION_SUSPICIOUS_CHECK    = "com.bankingapp.SUSPICIOUS_CHECK";

    // ── Intent extras ─────────────────────────────────────────────────────
    public static final String EXTRA_AMOUNT  = "extra_amount";
    public static final String EXTRA_TYPE    = "extra_type";
    public static final String EXTRA_USER_ID = "extra_user_id";

    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String action  = intent.getAction();
        double amount  = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0);
        String type    = intent.getStringExtra(EXTRA_TYPE);
        int    userId  = intent.getIntExtra(EXTRA_USER_ID, -1);

        // Run work on a background thread — never block the main thread
        new Thread(() -> {
            String msg;

            if (ACTION_PROCESS_TRANSACTION.equals(action)) {
                msg = processTransaction(amount, type, userId);
            } else if (ACTION_SUSPICIOUS_CHECK.equals(action)) {
                msg = checkSuspicious(amount);
            } else {
                msg = null;
            }

            if (msg != null) {
                final String finalMsg = msg;
                mainHandler.post(() ->
                        Toast.makeText(getApplicationContext(),
                                finalMsg, Toast.LENGTH_SHORT).show());
            }
            stopSelf();

        }).start();

        return START_NOT_STICKY;
    }

    // ── Process a completed transaction asynchronously ────────────────────
    private String processTransaction(double amount, String type, int userId) {
        if (amount <= 0)
            return "Invalid transaction amount.";
        if (amount > 100000)
            return "Transaction limit exceeded.";

        // Simulate background processing delay (e.g. server sync)
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        return type + " of ₹" + String.format("%.2f", amount) + " processed.";
    }

    // ── Check if a transaction amount is suspicious ───────────────────────
    private String checkSuspicious(double amount) {
        if (amount > 5000) {
            return "⚠ Suspicious Activity: Large transfer of ₹" +
                    String.format("%.2f", amount) + " detected. Tap to review.";
        }
        return null;  // not suspicious — no toast needed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;  // started service, not bound
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}