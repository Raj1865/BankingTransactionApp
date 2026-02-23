package com.bankingapp.models;

import android.database.Cursor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.bankingapp.utils.DateTimeHelper;

public class Transaction {

    // ── Type Constants (use these everywhere — avoid raw strings) ──────────
    public static final String TYPE_SENT         = "SENT";
    public static final String TYPE_RECEIVED     = "RECEIVED";
    public static final String TYPE_BILL_PAYMENT = "BILL_PAYMENT";

    // ── Category Constants ────────────────────────────────────────────────
    public static final String CAT_TRANSFER    = "Transfer";
    public static final String CAT_ELECTRICITY = "Electricity";
    public static final String CAT_WATER       = "Water";
    public static final String CAT_RECHARGE    = "Recharge";

    // ── Status Constants ──────────────────────────────────────────────────
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED  = "FAILED";

    // ── Fields ────────────────────────────────────────────────────────────
    private int    id;
    private int    userId;
    private String type;
    private String category;
    private double amount;
    private String description;
    private String toFromName;   // recipient name (SENT) or sender name (RECEIVED)
    private String toFromPhone;  // recipient/sender phone
    private String dateTime;     // 'yyyy-MM-dd HH:mm:ss'
    private double latitude;
    private double longitude;
    private String status;

    // ── Constructor for NEW transactions (auto-sets dateTime + status) ─────
    public Transaction(int userId, String type, String category,
                       double amount, String description,
                       String toFromName, double lat, double lng) {
        this.userId      = userId;
        this.type        = type;
        this.category    = category;
        this.amount      = amount;
        this.description = description;
        this.toFromName  = toFromName;
        this.latitude    = lat;
        this.longitude   = lng;
        this.status      = "SUCCESS";

        // Use DateTimeHelper for consistent formatting across the app
        this.dateTime = DateTimeHelper.nowForDb();  // "yyyy-MM-dd HH:mm:ss"
    }

    // In getFormattedDateTime() — convert to display format for UI:
    public String getFormattedDateTime() {
        return DateTimeHelper.toDisplayFormat(this.dateTime);
        // e.g. "19 Feb 2026, 10:30 AM"
    }


    // ── Cursor Constructor (maps SQLite row → Transaction object) ─────────
    public Transaction(Cursor cursor) {
        this.id          = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        this.userId      = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
        this.type        = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        this.category    = cursor.getString(cursor.getColumnIndexOrThrow("category"));
        this.amount      = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
        this.description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
        this.toFromName  = cursor.getString(cursor.getColumnIndexOrThrow("to_from_name"));
        this.toFromPhone = cursor.getString(cursor.getColumnIndexOrThrow("to_from_phone"));
        this.dateTime    = cursor.getString(cursor.getColumnIndexOrThrow("date_time"));
        this.latitude    = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
        this.longitude   = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
        this.status      = cursor.getString(cursor.getColumnIndexOrThrow("status"));
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public int    getId()          { return id; }
    public int    getUserId()      { return userId; }
    public String getType()        { return type; }
    public String getCategory()    { return category; }
    public double getAmount()      { return amount; }
    public String getDescription() { return description; }
    public String getToFromName()  { return toFromName; }
    public String getToFromPhone() { return toFromPhone; }
    public String getDateTime()    { return dateTime; }
    public double getLatitude()    { return latitude; }
    public double getLongitude()   { return longitude; }
    public String getStatus()      { return status; }

    // ── Helper: display-ready amount string ───────────────────────────────
    public String getFormattedAmount() {
        if (type.equals(TYPE_SENT) || type.equals(TYPE_BILL_PAYMENT)) {
            return String.format("- ₹ %,.2f", amount);
        } else {
            return String.format("+ ₹ %,.2f", amount);
        }
    }

    // ── Helper: display-ready date (shows only date part) ─────────────────
    public String getDisplayDate() {
        if (dateTime != null && dateTime.length() >= 10) {
            return dateTime.substring(0, 10);  // 'yyyy-MM-dd'
        }
        return dateTime;
    }
}
