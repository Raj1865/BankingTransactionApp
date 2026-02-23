package com.bankingapp.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bankingapp.R;
import com.bankingapp.activities.DashboardActivity;
import com.bankingapp.activities.SettingsActivity;
import com.bankingapp.database.DatabaseHelper;
import com.bankingapp.models.Bill;
import com.bankingapp.utils.NotificationHelper;
import com.bankingapp.utils.SessionManager;
import com.bankingapp.utils.TransactionManager;

public class PayBillFragment extends Fragment {

    // ── Views ─────────────────────────────────────────────────────────────
    private Spinner    spinnerBillType;
    private EditText   etBillAmount;
    private RadioGroup rgPaymentMethod;
    private TextView   tvBillAvailableBalance;
    private Button     btnPayNow, btnBillCancel;

    // ── Helpers ───────────────────────────────────────────────────────────
    private TransactionManager txnManager;
    private SessionManager     session;
    private DatabaseHelper     db;

    // Bill types shown in Spinner
    private final String[] BILL_TYPES = {
            Bill.TYPE_ELECTRICITY,
            Bill.TYPE_WATER,
            Bill.TYPE_RECHARGE
    };

    // ═════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═════════════════════════════════════════════════════════════════════

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_pay_bill, container, false);

        txnManager = new TransactionManager(requireContext());
        session    = new SessionManager(requireContext());
        db         = new DatabaseHelper(requireContext());

        // Bind views
        spinnerBillType        = view.findViewById(R.id.spinnerBillType);
        etBillAmount           = view.findViewById(R.id.etBillAmount);
        rgPaymentMethod        = view.findViewById(R.id.rgPaymentMethod);
        tvBillAvailableBalance = view.findViewById(R.id.tvBillAvailableBalance);
        btnPayNow              = view.findViewById(R.id.btnPayNow);
        btnBillCancel          = view.findViewById(R.id.btnBillCancel);

        // Setup Spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                BILL_TYPES);
        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerBillType.setAdapter(spinnerAdapter);

        // Show available balance
        double balance = db.getBalance(session.getUserId());
        tvBillAvailableBalance.setText(String.format(
                "Available Balance: ₹ %,.2f", balance));

        // Button listeners
        btnPayNow.setOnClickListener(v -> handlePay());

        btnBillCancel.setOnClickListener(v -> {
            if (getActivity() instanceof DashboardActivity) {
                ((DashboardActivity) getActivity()).onTransactionFlowCancelled();
            } else if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

    // ═════════════════════════════════════════════════════════════════════
    // PAY BILL FLOW
    // ═════════════════════════════════════════════════════════════════════

    private void handlePay() {
        String billType = spinnerBillType.getSelectedItem().toString();
        String amtStr   = etBillAmount.getText().toString().trim();

        int selectedId = rgPaymentMethod.getCheckedRadioButtonId();
        RadioButton selectedRb = rgPaymentMethod.findViewById(selectedId);
        String paymentMethod = (selectedRb != null) ?
                selectedRb.getText().toString() : "Wallet";

        if (amtStr.isEmpty()) {
            etBillAmount.setError("Enter bill amount");
            etBillAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
        } catch (NumberFormatException e) {
            etBillAmount.setError("Invalid amount");
            return;
        }

        // Confirm dialog
        String message = String.format(
                "Pay ₹%,.2f for %s?\nPayment via: %s",
                amount, billType, paymentMethod);

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Bill Payment")
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("Pay Now",
                        (dialog, which) -> processBillPayment(billType, amount))
                .setNegativeButton(getString(R.string.dlg_cancel),
                        (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void processBillPayment(String billType, double amount) {
        TransactionManager.Result result = txnManager.payBill(billType, amount);

        if (result.success) {
            // ── Fire bill-paid notification if user enabled it ────────────
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);

            if (prefs.getBoolean(SettingsActivity.KEY_NOTIF_BILLS, true)) {
                new NotificationHelper(requireContext()).notifyBillPaid(billType, amount);
            }

            Toast.makeText(requireContext(),
                    result.message, Toast.LENGTH_LONG).show();

            // Notify Dashboard to refresh balance and transaction list
            if (getActivity() instanceof DashboardActivity) {
                ((DashboardActivity) getActivity()).onTransactionComplete();
            }

        } else {
            Toast.makeText(requireContext(),
                    result.message, Toast.LENGTH_LONG).show();
        }
    }
}