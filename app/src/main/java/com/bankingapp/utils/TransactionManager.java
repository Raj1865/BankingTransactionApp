package com.bankingapp.utils;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import com.bankingapp.database.DatabaseHelper;
import com.bankingapp.models.Bill;
import com.bankingapp.models.Transaction;
import com.bankingapp.models.User;

public class TransactionManager {

    public static final double SUSPICIOUS_FLAT_AMOUNT = 5000.0;
    public static final double SUSPICIOUS_BALANCE_PCT = 0.50;

    // ── Result wrapper returned to Fragments ──────────────────────────────
    public static class Result {
        public final boolean success;
        public final boolean suspicious;
        public final String  message;
        public final double  newBalance;

        public Result(boolean success, boolean suspicious,
                      String message, double newBalance) {
            this.success    = success;
            this.suspicious = suspicious;
            this.message    = message;
            this.newBalance = newBalance;
        }
    }

    // ── Use DatabaseHelper directly (not IDataRepository) so getBalance()
    //    and other DB-specific methods are accessible ──────────────────────
    private final DatabaseHelper db;
    private final SessionManager session;
    private final Context        context;

    public TransactionManager(Context context) {
        this.context = context;
        this.db      = new DatabaseHelper(context);
        this.session = new SessionManager(context);
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND MONEY
    // ─────────────────────────────────────────────────────────────────────
    public Result sendMoney(String recipientPhone, double amount,
                            double lat, double lng) {

        if (amount <= 0)
            return new Result(false, false, "Amount must be greater than 0", 0);

        if (amount > 100000)
            return new Result(false, false,
                    "Amount exceeds daily limit of ₹1,00,000", 0);

        if (recipientPhone == null || !recipientPhone.matches("[0-9]{10}"))
            return new Result(false, false,
                    "Enter a valid 10-digit phone number", 0);

        int    userId  = session.getUserId();
        double balance = db.getBalance(userId);

        if (balance < amount)
            return new Result(false, false,
                    "Insufficient balance. Available: ₹" +
                            String.format("%.2f", balance), balance);

        boolean suspicious = isSuspicious(amount, balance);

        User   recipient     = db.getUserByPhone(recipientPhone);
        String recipientName = (recipient != null) ?
                recipient.getFullName() : recipientPhone;

        double newBalance = balance - amount;
        db.updateUserBalance(userId, newBalance);

        // ── Record SENT transaction (8-param constructor) ─────────────────
        Transaction sentTxn = new Transaction(
                userId,
                Transaction.TYPE_SENT,
                Transaction.CAT_TRANSFER,
                amount,
                "Sent to " + recipientName,
                recipientName,
                lat, lng
        );
        db.insertTransaction(sentTxn);

        // ── Credit recipient if they are in our app ───────────────────────
        if (recipient != null) {
            double recipientBalance = db.getBalance(recipient.getId());
            db.updateUserBalance(recipient.getId(), recipientBalance + amount);

            String senderName = session.getFullName();
            Transaction receivedTxn = new Transaction(
                    recipient.getId(),
                    Transaction.TYPE_RECEIVED,
                    Transaction.CAT_TRANSFER,
                    amount,
                    "Received from " + senderName,
                    senderName,
                    lat, lng
            );
            db.insertTransaction(receivedTxn);
        }

        return new Result(true, suspicious,
                "₹" + String.format("%.2f", amount) + " sent to " + recipientName,
                newBalance);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PAY BILL
    // ─────────────────────────────────────────────────────────────────────
    public Result payBill(String billType, double amount) {

        if (amount <= 0)
            return new Result(false, false, "Amount must be greater than 0", 0);

        int    userId  = session.getUserId();
        double balance = db.getBalance(userId);

        if (balance < amount)
            return new Result(false, false,
                    "Insufficient balance. Available: ₹" +
                            String.format("%.2f", balance), balance);

        double newBalance = balance - amount;
        db.updateUserBalance(userId, newBalance);

        double[] latLng = getLastKnownLocation();

        Transaction txn = new Transaction(
                userId,
                Transaction.TYPE_BILL_PAYMENT,
                billType,
                amount,
                billType + " bill payment",
                billType + " Provider",
                latLng[0], latLng[1]
        );
        db.insertTransaction(txn);

        Bill bill = new Bill(userId, billType, amount);
        db.insertBill(bill);

        return new Result(true, false,
                billType + " bill of ₹" +
                        String.format("%.2f", amount) + " paid!", newBalance);
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────
    private boolean isSuspicious(double amount, double balance) {
        return amount >= SUSPICIOUS_FLAT_AMOUNT ||
                amount >= (balance * SUSPICIOUS_BALANCE_PCT);
    }

    private double[] getLastKnownLocation() {
        try {
            LocationManager lm = (LocationManager)
                    context.getSystemService(Context.LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null)
                loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null)
                return new double[]{ loc.getLatitude(), loc.getLongitude() };
        } catch (SecurityException e) {
            // permission not granted
        }
        return new double[]{ 0.0, 0.0 };
    }
}