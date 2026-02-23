package com.bankingapp.database;

import com.bankingapp.models.Bill;
import com.bankingapp.models.SavingsGoal;
import com.bankingapp.models.Transaction;
import com.bankingapp.models.User;

import java.util.List;
import java.util.Map;

public interface IDataRepository {

    // ── USER OPERATIONS ──────────────────────────────────────────────────
    boolean registerUser(String username, String password,
                         String fullName, String phone);

    User    loginUser(String username, String password);
    User    getUserById(int userId);
    User    getUserByPhone(String phone);

    boolean updateUserBalance(int userId, double newBalance);
    boolean updateProfilePic(int userId, String picPath);
    boolean isUsernameExists(String username);
    boolean isPhoneExists(String phone);

    // ── TRANSACTION OPERATIONS ────────────────────────────────────────────
    boolean         insertTransaction(Transaction transaction);
    List<Transaction> getAllTransactions(int userId);
    List<Transaction> getRecentTransactions(int userId, int limit);
    List<Transaction> getTransactionsByDateRange(int userId,
                                                 String fromDate,
                                                 String toDate);

    // ── BILL OPERATIONS ───────────────────────────────────────────────────
    boolean     insertBill(Bill bill);
    List<Bill>  getBillsByUser(int userId);

    // ── SAVINGS GOAL OPERATIONS ───────────────────────────────────────────
    boolean          insertGoal(SavingsGoal goal);
    List<SavingsGoal> getGoalsByUser(int userId);
    boolean          updateGoalAmount(int goalId, double newAmount);

    // ── INSIGHTS / AGGREGATES ─────────────────────────────────────────────
    // month format: 'yyyy-MM'  e.g. '2026-02'
    Map<String, Double> getSpendingByCategory(int userId, String month);
    double getTotalSpentThisMonth(int userId, String month);
    double getTotalReceivedThisMonth(int userId, String month);
}