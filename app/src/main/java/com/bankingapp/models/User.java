package com.bankingapp.models;

import android.database.Cursor;

public class User {

    // ── Fields ────────────────────────────────────────────────────────────
    private int    id;
    private String username;
    private String fullName;
    private String phone;
    private String accountNo;
    private double balance;
    private String profilePicPath;   // local file path or null
    private String createdAt;

    // ── Empty Constructor ─────────────────────────────────────────────────
    public User() {}

    // ── Full Constructor (for registering new users) ──────────────────────
    public User(String username, String fullName, String phone,
                String accountNo, double balance) {
        this.username   = username;
        this.fullName   = fullName;
        this.phone      = phone;
        this.accountNo  = accountNo;
        this.balance    = balance;
    }

    // ── Cursor Constructor (maps SQLite row → User object) ────────────────
    public User(Cursor cursor) {
        this.id             = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        this.username       = cursor.getString(cursor.getColumnIndexOrThrow("username"));
        this.fullName       = cursor.getString(cursor.getColumnIndexOrThrow("full_name"));
        this.phone          = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
        this.accountNo      = cursor.getString(cursor.getColumnIndexOrThrow("account_no"));
        this.balance        = cursor.getDouble(cursor.getColumnIndexOrThrow("balance"));
        this.profilePicPath = cursor.getString(cursor.getColumnIndexOrThrow("profile_pic"));
        this.createdAt      = cursor.getString(cursor.getColumnIndexOrThrow("created_at"));
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public int    getId()             { return id; }
    public String getUsername()       { return username; }
    public String getFullName()       { return fullName; }
    public String getPhone()          { return phone; }
    public String getAccountNo()      { return accountNo; }
    public double getBalance()        { return balance; }
    public String getProfilePicPath() { return profilePicPath; }
    public String getCreatedAt()      { return createdAt; }

    // ── Setters ───────────────────────────────────────────────────────────
    public void setId(int id)                       { this.id = id; }
    public void setUsername(String username)         { this.username = username; }
    public void setFullName(String fullName)         { this.fullName = fullName; }
    public void setPhone(String phone)               { this.phone = phone; }
    public void setAccountNo(String accountNo)       { this.accountNo = accountNo; }
    public void setBalance(double balance)           { this.balance = balance; }
    public void setProfilePicPath(String path)       { this.profilePicPath = path; }
    public void setCreatedAt(String createdAt)       { this.createdAt = createdAt; }

    // ── Helper: display-ready balance string ──────────────────────────────
    public String getFormattedBalance() {
        return String.format("₹ %,.2f", balance);
    }
}
