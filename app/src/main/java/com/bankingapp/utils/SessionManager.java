package com.bankingapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME      = "BankingAppSession";
    private static final String KEY_IS_LOGGED   = "isLoggedIn";
    private static final String KEY_USER_ID     = "userId";
    private static final String KEY_USERNAME    = "username";
    private static final String KEY_FULL_NAME   = "fullName";
    private static final String KEY_ACCOUNT_NO  = "accountNo";
    private static final String KEY_PHONE       = "phone";

    private final SharedPreferences         pref;
    private final SharedPreferences.Editor  editor;

    public SessionManager(Context context) {
        pref   = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // ── Save session after successful login ───────────────────────────────
    public void createSession(int userId, String username,
                              String fullName, String accountNo, String phone) {
        editor.putBoolean(KEY_IS_LOGGED,  true);
        editor.putInt   (KEY_USER_ID,     userId);
        editor.putString(KEY_USERNAME,    username);
        editor.putString(KEY_FULL_NAME,   fullName);
        editor.putString(KEY_ACCOUNT_NO,  accountNo);
        editor.putString(KEY_PHONE,       phone);
        editor.apply();  // apply() is async (faster than commit())
    }

    // ── Check if user is logged in ────────────────────────────────────────
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED, false);
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public int    getUserId()    { return pref.getInt(KEY_USER_ID, -1); }
    public String getUsername()  { return pref.getString(KEY_USERNAME, ""); }
    public String getFullName()  { return pref.getString(KEY_FULL_NAME, ""); }
    public String getAccountNo() { return pref.getString(KEY_ACCOUNT_NO, ""); }
    public String getPhone()     { return pref.getString(KEY_PHONE, ""); }

    // ── Clear session on logout ───────────────────────────────────────────
    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}
