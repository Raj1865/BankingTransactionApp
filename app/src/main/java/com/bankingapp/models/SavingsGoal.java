package com.bankingapp.models;

import android.database.Cursor;

public class SavingsGoal {

    private int    id;
    private int    userId;
    private String goalName;
    private double targetAmount;
    private double currentAmount;
    private String createdAt;

    // Constructor for new goal
    public SavingsGoal(int userId, String goalName, double targetAmount) {
        this.userId        = userId;
        this.goalName      = goalName;
        this.targetAmount  = targetAmount;
        this.currentAmount = 0.0;
    }

    // Cursor constructor
    public SavingsGoal(Cursor cursor) {
        this.id            = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        this.userId        = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
        this.goalName      = cursor.getString(cursor.getColumnIndexOrThrow("goal_name"));
        this.targetAmount  = cursor.getDouble(cursor.getColumnIndexOrThrow("target_amount"));
        this.currentAmount = cursor.getDouble(cursor.getColumnIndexOrThrow("current_amount"));
        this.createdAt     = cursor.getString(cursor.getColumnIndexOrThrow("created_at"));
    }

    // Getters
    public int    getId()            { return id; }
    public int    getUserId()        { return userId; }
    public String getGoalName()      { return goalName; }
    public double getTargetAmount()  { return targetAmount; }
    public double getCurrentAmount() { return currentAmount; }
    public String getCreatedAt()     { return createdAt; }

    // Setters
    public void setCurrentAmount(double currentAmount) {
        this.currentAmount = currentAmount;
    }

    // Progress percentage (0–100)
    public int getProgressPercent() {
        if (targetAmount <= 0) return 0;
        return (int) Math.min(100, (currentAmount / targetAmount) * 100);
    }
}
