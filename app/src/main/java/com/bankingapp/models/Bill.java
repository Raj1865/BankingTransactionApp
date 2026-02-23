package com.bankingapp.models;

import android.database.Cursor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Bill {

    public static final String TYPE_ELECTRICITY = "Electricity";
    public static final String TYPE_WATER       = "Water";
    public static final String TYPE_RECHARGE    = "Mobile Recharge";

    public static final String STATUS_PAID    = "PAID";
    public static final String STATUS_PENDING = "PENDING";

    private int    id;
    private int    userId;
    private String billType;
    private double amount;
    private String paidAt;
    private String status;

    // Constructor for new bill payment
    public Bill(int userId, String billType, double amount) {
        this.userId   = userId;
        this.billType = billType;
        this.amount   = amount;
        this.paidAt   = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        this.status   = STATUS_PAID;
    }

    // Cursor constructor
    public Bill(Cursor cursor) {
        this.id       = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        this.userId   = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
        this.billType = cursor.getString(cursor.getColumnIndexOrThrow("bill_type"));
        this.amount   = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
        this.paidAt   = cursor.getString(cursor.getColumnIndexOrThrow("paid_at"));
        this.status   = cursor.getString(cursor.getColumnIndexOrThrow("status"));
    }

    // Getters
    public int    getId()       { return id; }
    public int    getUserId()   { return userId; }
    public String getBillType() { return billType; }
    public double getAmount()   { return amount; }
    public String getPaidAt()   { return paidAt; }
    public String getStatus()   { return status; }
}
