package com.bankingapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bankingapp.models.Bill;
import com.bankingapp.models.SavingsGoal;
import com.bankingapp.models.Transaction;
import com.bankingapp.models.User;
import com.bankingapp.utils.PasswordUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper
        implements IDataRepository {

    // ── Database Info ─────────────────────────────────────────────────────
    private static final String DB_NAME    = "banking_app.db";
    private static final int    DB_VERSION = 1;

    // ── Table Names ───────────────────────────────────────────────────────
    private static final String TABLE_USERS        = "users";
    private static final String TABLE_TRANSACTIONS = "transactions";
    private static final String TABLE_BILLS        = "bills";
    private static final String COL_BALANCE  = "balance";
    private static final String TABLE_GOALS        = "savings_goals";

    // ── CREATE TABLE Statements ───────────────────────────────────────────
    private static final String CREATE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    "id           INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username     TEXT NOT NULL UNIQUE," +
                    "password     TEXT NOT NULL," +
                    "full_name    TEXT NOT NULL," +
                    "phone        TEXT NOT NULL UNIQUE," +
                    "account_no   TEXT NOT NULL UNIQUE," +
                    "balance      REAL DEFAULT 0.0," +
                    "profile_pic  TEXT," +
                    "created_at   TEXT" +
                    ")";

    private static final String CREATE_TRANSACTIONS =
            "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                    "id            INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id       INTEGER NOT NULL," +
                    "type          TEXT NOT NULL," +
                    "category      TEXT," +
                    "amount        REAL NOT NULL," +
                    "description   TEXT," +
                    "to_from_name  TEXT," +
                    "to_from_phone TEXT," +
                    "date_time     TEXT NOT NULL," +
                    "latitude      REAL DEFAULT 0.0," +
                    "longitude     REAL DEFAULT 0.0," +
                    "status        TEXT DEFAULT 'SUCCESS'," +
                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ")";

    private static final String CREATE_BILLS =
            "CREATE TABLE " + TABLE_BILLS + " (" +
                    "id        INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id   INTEGER NOT NULL," +
                    "bill_type TEXT NOT NULL," +
                    "amount    REAL NOT NULL," +
                    "paid_at   TEXT," +
                    "status    TEXT DEFAULT 'PAID'," +
                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ")";

    private static final String CREATE_GOALS =
            "CREATE TABLE " + TABLE_GOALS + " (" +
                    "id             INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id        INTEGER NOT NULL," +
                    "goal_name      TEXT NOT NULL," +
                    "target_amount  REAL NOT NULL," +
                    "current_amount REAL DEFAULT 0.0," +
                    "created_at     TEXT," +
                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ")";

    // ── Constructor ───────────────────────────────────────────────────────
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ── onCreate: runs once when DB is first created ───────────────────────
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS);
        db.execSQL(CREATE_TRANSACTIONS);
        db.execSQL(CREATE_BILLS);
        db.execSQL(CREATE_GOALS);
        insertDemoData(db);  // Pre-load demo account
    }

    // ── onUpgrade: runs when DB_VERSION is incremented ───────────────────
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GOALS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BILLS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // ── Demo Data (for testing login immediately) ─────────────────────────
    private void insertDemoData(SQLiteDatabase db) {
        ContentValues cv = new ContentValues();
        cv.put("username",   "demo");
        cv.put("password",   PasswordUtils.hash("demo123"));
        cv.put("full_name",  "Demo User");
        cv.put("phone",      "9999999999");
        cv.put("account_no", "AC0059431234");
        cv.put("balance",    25000.00);
        cv.put("created_at", getCurrentDateTime());
        db.insert(TABLE_USERS, null, cv);
    }

    // ═════════════════════════════════════════════════════════════════════
    // USER OPERATIONS
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public boolean registerUser(String username, String password,
                                String fullName, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv  = new ContentValues();
        cv.put("username",   username);
        cv.put("password",   PasswordUtils.hash(password));
        cv.put("full_name",  fullName);
        cv.put("phone",      phone);
        cv.put("account_no", generateAccountNo());
        cv.put("balance",    10000.00);   // Starting demo balance
        cv.put("created_at", getCurrentDateTime());
        long result = db.insert(TABLE_USERS, null, cv);
        db.close();
        return result != -1;
    }

    @Override
    public User loginUser(String username, String password) {
        SQLiteDatabase db     = this.getReadableDatabase();
        String hashedPassword = PasswordUtils.hash(password);
        Cursor cursor = db.query(
                TABLE_USERS,
                null,
                "username=? AND password=?",
                new String[]{ username, hashedPassword },
                null, null, null
        );
        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = new User(cursor);
        }
        if (cursor != null) cursor.close();
        db.close();
        return user;  // null if login failed
    }

    @Override
    public User getUserById(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                "id=?", new String[]{ String.valueOf(userId) },
                null, null, null);
        User user = null;
        if (cursor != null && cursor.moveToFirst()) user = new User(cursor);
        if (cursor != null) cursor.close();
        db.close();
        return user;
    }

    @Override
    public User getUserByPhone(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                "phone=?", new String[]{ phone }, null, null, null);
        User user = null;
        if (cursor != null && cursor.moveToFirst()) user = new User(cursor);
        if (cursor != null) cursor.close();
        db.close();
        return user;
    }

    @Override
    public boolean updateUserBalance(int userId, double newBalance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv  = new ContentValues();
        cv.put("balance", newBalance);
        int rows = db.update(TABLE_USERS, cv,
                "id=?", new String[]{ String.valueOf(userId) });
        db.close();
        return rows > 0;
    }

    @Override
    public boolean updateProfilePic(int userId, String picPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv  = new ContentValues();
        cv.put("profile_pic", picPath);
        int rows = db.update(TABLE_USERS, cv,
                "id=?", new String[]{ String.valueOf(userId) });
        db.close();
        return rows > 0;
    }

    @Override
    public boolean isUsernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{"id"}, "username=?",
                new String[]{ username }, null, null, null);
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        db.close();
        return exists;
    }

    @Override
    public boolean isPhoneExists(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{"id"}, "phone=?",
                new String[]{ phone }, null, null, null);
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        db.close();
        return exists;
    }

    // ═════════════════════════════════════════════════════════════════════
    // TRANSACTION OPERATIONS
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public boolean insertTransaction(Transaction t) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv  = new ContentValues();
        cv.put("user_id",       t.getUserId());
        cv.put("type",          t.getType());
        cv.put("category",      t.getCategory());
        cv.put("amount",        t.getAmount());
        cv.put("description",   t.getDescription());
        cv.put("to_from_name",  t.getToFromName());
        cv.put("to_from_phone", t.getToFromPhone());
        cv.put("date_time",     t.getDateTime());
        cv.put("latitude",      t.getLatitude());
        cv.put("longitude",     t.getLongitude());
        cv.put("status",        t.getStatus());
        long result = db.insert(TABLE_TRANSACTIONS, null, cv);
        db.close();
        return result != -1;
    }

    @Override
    public List<Transaction> getAllTransactions(int userId) {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TRANSACTIONS, null,
                "user_id=?", new String[]{ String.valueOf(userId) },
                null, null, "date_time DESC");
        while (cursor.moveToNext()) {
            list.add(new Transaction(cursor));
        }
        cursor.close(); db.close();
        return list;
    }

    @Override
    public List<Transaction> getRecentTransactions(int userId, int limit) {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TRANSACTIONS, null,
                "user_id=?", new String[]{ String.valueOf(userId) },
                null, null, "date_time DESC", String.valueOf(limit));
        while (cursor.moveToNext()) list.add(new Transaction(cursor));
        cursor.close(); db.close();
        return list;
    }

    @Override
    public List<Transaction> getTransactionsByDateRange(int userId,
                                                        String fromDate, String toDate) {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TRANSACTIONS, null,
                "user_id=? AND date_time BETWEEN ? AND ?",
                new String[]{ String.valueOf(userId), fromDate, toDate + " 23:59:59" },
                null, null, "date_time DESC");
        while (cursor.moveToNext()) list.add(new Transaction(cursor));
        cursor.close(); db.close();
        return list;
    }

    // ═════════════════════════════════════════════════════════════════════
    // BILL OPERATIONS
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public boolean insertBill(Bill bill) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv  = new ContentValues();
        cv.put("user_id",   bill.getUserId());
        cv.put("bill_type", bill.getBillType());
        cv.put("amount",    bill.getAmount());
        cv.put("paid_at",   bill.getPaidAt());
        cv.put("status",    bill.getStatus());
        long result = db.insert(TABLE_BILLS, null, cv);
        db.close();
        return result != -1;
    }

    @Override
    public List<Bill> getBillsByUser(int userId) {
        List<Bill> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BILLS, null,
                "user_id=?", new String[]{ String.valueOf(userId) },
                null, null, "paid_at DESC");
        while (cursor.moveToNext()) list.add(new Bill(cursor));
        cursor.close(); db.close();
        return list;
    }

    // ═════════════════════════════════════════════════════════════════════
    // SAVINGS GOALS
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public boolean insertGoal(SavingsGoal goal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv  = new ContentValues();
        cv.put("user_id",        goal.getUserId());
        cv.put("goal_name",      goal.getGoalName());
        cv.put("target_amount",  goal.getTargetAmount());
        cv.put("current_amount", 0.0);
        cv.put("created_at",     getCurrentDateTime());
        long result = db.insert(TABLE_GOALS, null, cv);
        db.close();
        return result != -1;
    }

    @Override
    public List<SavingsGoal> getGoalsByUser(int userId) {
        List<SavingsGoal> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_GOALS, null,
                "user_id=?", new String[]{ String.valueOf(userId) },
                null, null, "created_at DESC");
        while (cursor.moveToNext()) list.add(new SavingsGoal(cursor));
        cursor.close(); db.close();
        return list;
    }

    @Override
    public boolean updateGoalAmount(int goalId, double newAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv  = new ContentValues();
        cv.put("current_amount", newAmount);
        int rows = db.update(TABLE_GOALS, cv,
                "id=?", new String[]{ String.valueOf(goalId) });
        db.close();
        return rows > 0;
    }

    // ═════════════════════════════════════════════════════════════════════
    // INSIGHTS / AGGREGATES
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public Map<String, Double> getSpendingByCategory(int userId, String month) {
        Map<String, Double> map = new LinkedHashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql =
                "SELECT category, SUM(amount) FROM " + TABLE_TRANSACTIONS +
                        " WHERE user_id=? AND (type='SENT' OR type='BILL_PAYMENT')" +
                        " AND date_time LIKE ? GROUP BY category";
        Cursor cursor = db.rawQuery(sql,
                new String[]{ String.valueOf(userId), month + "%" });
        while (cursor.moveToNext()) {
            map.put(cursor.getString(0), cursor.getDouble(1));
        }
        cursor.close(); db.close();
        return map;
    }

    @Override
    public double getTotalSpentThisMonth(int userId, String month) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql =
                "SELECT SUM(amount) FROM " + TABLE_TRANSACTIONS +
                        " WHERE user_id=? AND (type='SENT' OR type='BILL_PAYMENT')" +
                        " AND date_time LIKE ?";
        Cursor cursor = db.rawQuery(sql,
                new String[]{ String.valueOf(userId), month + "%" });
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close(); db.close();
        return total;
    }

    @Override
    public double getTotalReceivedThisMonth(int userId, String month) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql =
                "SELECT SUM(amount) FROM " + TABLE_TRANSACTIONS +
                        " WHERE user_id=? AND type='RECEIVED' AND date_time LIKE ?";
        Cursor cursor = db.rawQuery(sql,
                new String[]{ String.valueOf(userId), month + "%" });
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close(); db.close();
        return total;
    }

    // ═════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═════════════════════════════════════════════════════════════════════

    private String getCurrentDateTime() {
        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String generateAccountNo() {
        // Format: AC + 10 random digits
        long num = (long)(Math.random() * 9_000_000_000L) + 1_000_000_000L;
        return "AC" + num;
    }

    // ── ADD this method to DatabaseHelper.java if missing ──────────────────
    // It reads the balance column directly from the users table.

    public double getBalance(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{ COL_BALANCE },
                "id = ?",
                new String[]{ String.valueOf(userId) },
                null, null, null
        );
        double balance = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            balance = cursor.getDouble(0);
            cursor.close();
        }
        db.close();
        return balance;
    }


}
