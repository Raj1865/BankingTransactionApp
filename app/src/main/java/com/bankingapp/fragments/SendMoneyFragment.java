package com.bankingapp.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bankingapp.R;
import com.bankingapp.activities.DashboardActivity;
import com.bankingapp.activities.SettingsActivity;
import com.bankingapp.database.DatabaseHelper;
import com.bankingapp.services.TransactionService;
import com.bankingapp.utils.LocationHelper;
import com.bankingapp.utils.NotificationHelper;
import com.bankingapp.utils.SessionManager;
import com.bankingapp.utils.TransactionManager;

import java.util.ArrayList;
import java.util.Locale;

public class SendMoneyFragment extends Fragment {

    private static final int REQ_CAMERA           = 200;
    private static final int REQ_CAM_PERM         = 201;
    private static final int REQ_SPEECH_RECIPIENT = 300;
    private static final int REQ_MIC_PERM         = 301;

    // ── Views ─────────────────────────────────────────────────────────────
    private EditText etRecipientPhone;
    private EditText etAmount;
    private TextView   tvAvailableBalance;
    private Button     btnSend;
    private Button     btnSendCancel;
    private Button     btnScanToPay;
    private ImageButton btnMicRecipient;

    // ── Helpers ───────────────────────────────────────────────────────────
    private TransactionManager txnManager;
    private SessionManager     session;
    private DatabaseHelper     db;
    private LocationHelper     locationHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_send_money, container, false);

        txnManager     = new TransactionManager(requireContext());
        session        = new SessionManager(requireContext());
        db             = new DatabaseHelper(requireContext());
        locationHelper = new LocationHelper(requireContext());
        locationHelper.startUpdates();

        etRecipientPhone   = view.findViewById(R.id.etRecipientPhone);
        etAmount           = view.findViewById(R.id.etAmount);
        tvAvailableBalance = view.findViewById(R.id.tvAvailableBalance);
        btnSend            = view.findViewById(R.id.btnSend);
        btnSendCancel      = view.findViewById(R.id.btnSendCancel);
        btnScanToPay       = view.findViewById(R.id.btnScanToPay);
        btnMicRecipient    = view.findViewById(R.id.btnMicRecipient);

        // ── Scan-to-Pay with Camera ────────────────────────────────────────
        if (btnScanToPay != null) {
            btnScanToPay.setOnClickListener(v -> openCamera());
        }

        // ── Mic: speech-to-text for recipient phone ───────────────────────
        if (btnMicRecipient != null) {
            btnMicRecipient.setOnClickListener(v -> startSpeechForRecipient());
        }

        double balance = db.getBalance(session.getUserId());
        tvAvailableBalance.setText(String.format(
                "Available Balance: ₹ %,.2f", balance));

        btnSend.setOnClickListener(v -> handleSend());

        btnSendCancel.setOnClickListener(v -> {
            if (getActivity() instanceof DashboardActivity) {
                ((DashboardActivity) getActivity()).onTransactionFlowCancelled();
            } else if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationHelper != null) locationHelper.stopUpdates();
    }

    // ═════════════════════════════════════════════════════════════════════
    // SEND MONEY FLOW
    // ═════════════════════════════════════════════════════════════════════

    private void handleSend() {
        String phone  = etRecipientPhone.getText().toString().trim();
        String amtStr = etAmount.getText().toString().trim();

        if (phone.isEmpty()) {
            etRecipientPhone.setError("Enter recipient phone number");
            etRecipientPhone.requestFocus();
            return;
        }
        if (amtStr.isEmpty()) {
            etAmount.setError("Enter amount");
            etAmount.requestFocus();
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amtStr);
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount");
            return;
        }
        showConfirmDialog(phone, amount);
    }

    private void showConfirmDialog(String phone, double amount) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dlg_confirm_title))
                .setMessage(String.format(
                        "Send ₹%,.2f to %s?\n\nThis cannot be undone.", amount, phone))
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("Confirm", (d, w) -> processSend(phone, amount))
                .setNegativeButton(getString(R.string.dlg_cancel),
                        (d, w) -> d.dismiss())
                .show();
    }

    private void processSend(String phone, double amount) {
        double lat = locationHelper.getLatitude();
        double lng = locationHelper.getLongitude();

        TransactionManager.Result result = txnManager.sendMoney(phone, amount, lat, lng);

        if (result.success) {

            // ── Notifications (respect user preferences) ──────────────────
            SharedPreferences prefs = requireContext().getSharedPreferences(
                    SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);

            if (prefs.getBoolean(SettingsActivity.KEY_NOTIF_TRANSACTIONS, true))
                new NotificationHelper(requireContext()).notifyDebit(phone, amount);

            if (result.suspicious &&
                    prefs.getBoolean(SettingsActivity.KEY_NOTIF_ALERTS, true))
                new NotificationHelper(requireContext()).notifySuspicious(amount);

            // ── Background service for async processing ───────────────────
            Intent serviceIntent = new Intent(getContext(), TransactionService.class);
            serviceIntent.setAction(TransactionService.ACTION_PROCESS_TRANSACTION);
            serviceIntent.putExtra(TransactionService.EXTRA_AMOUNT, amount);
            serviceIntent.putExtra(TransactionService.EXTRA_TYPE, "SENT");
            serviceIntent.putExtra(TransactionService.EXTRA_USER_ID, session.getUserId());
            requireContext().startService(serviceIntent);

            if (result.suspicious) {
                showSuspiciousDialog(result.message);
            } else {
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show();
                notifyDashboardAndExit();
            }

        } else {
            Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show();
        }
    }

    private void showSuspiciousDialog(String successMessage) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dlg_suspicious_title))
                .setMessage(successMessage + "\n\n" +
                        getString(R.string.dlg_suspicious_msg))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setNeutralButton("OK, Noted", (d, w) -> notifyDashboardAndExit())
                .setCancelable(false)
                .show();
    }

    private void notifyDashboardAndExit() {
        if (getActivity() instanceof DashboardActivity)
            ((DashboardActivity) getActivity()).onTransactionComplete();
    }

    // ═════════════════════════════════════════════════════════════════════
    // SCAN-TO-PAY (Camera Intent) + Mic (Speech-to-text)
    // ═════════════════════════════════════════════════════════════════════

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAM_PERM);
            return;
        }
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(cameraIntent, REQ_CAMERA);
        } else {
            Toast.makeText(requireContext(),
                    "No camera app found on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startSpeechForRecipient() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC_PERM);
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Speak recipient phone number");

        try {
            startActivityForResult(intent, REQ_SPEECH_RECIPIENT);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Speech recognition not available on this device.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CAMERA && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getExtras() != null) {
                Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                // Simulate QR decode — pre-fill recipient field
                etRecipientPhone.setText("9876543210");
                Toast.makeText(requireContext(),
                        "QR scanned — recipient pre-filled.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_SPEECH_RECIPIENT && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                ArrayList<String> results =
                        data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty()) {
                    String spoken = results.get(0);
                    etRecipientPhone.setText(spoken.replaceAll("\\s+", ""));
                    Toast.makeText(requireContext(),
                            "Filled from mic input.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAM_PERM) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(requireContext(),
                        "Camera permission denied.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_MIC_PERM) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechForRecipient();
            } else {
                Toast.makeText(requireContext(),
                        "Microphone permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}