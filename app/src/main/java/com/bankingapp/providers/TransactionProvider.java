package com.bankingapp.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bankingapp.database.DatabaseHelper;

public class TransactionProvider extends ContentProvider {

    // ── Authority — must match AndroidManifest android:authorities ────────
    public static final String AUTHORITY =
            "com.bankingapp.provider";

    // ── Base URI ──────────────────────────────────────────────────────────
    public static final Uri CONTENT_URI = Uri.parse(
            "content://" + AUTHORITY + "/transactions");

    // ── URI codes for UriMatcher ──────────────────────────────────────────
    private static final int ALL_TRANSACTIONS   = 1;  // /transactions
    private static final int USER_TRANSACTIONS  = 2;  // /transactions/{userId}

    // ── UriMatcher: maps URIs to integer codes ────────────────────────────
    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "transactions",      ALL_TRANSACTIONS);
        uriMatcher.addURI(AUTHORITY, "transactions/#",    USER_TRANSACTIONS);
        //                                           ^
        //                             # = any integer (userId goes here)
    }

    private DatabaseHelper dbHelper;

    // ── onCreate: initialize the database helper ──────────────────────────
    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────
    // QUERY — called by getContentResolver().query(...)
    // ─────────────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;

        switch (uriMatcher.match(uri)) {

            case ALL_TRANSACTIONS:
                // content://com.bankingapp.provider/transactions
                // Returns ALL transactions (used for admin/debug only)
                cursor = db.query("transactions",
                        projection, selection, selectionArgs,
                        null, null,
                        sortOrder != null ? sortOrder : "date_time DESC");
                break;

            case USER_TRANSACTIONS:
                // content://com.bankingapp.provider/transactions/{userId}
                // Returns transactions filtered by user_id from URI path
                String userId = uri.getLastPathSegment();
                String sel    = "user_id = " + userId;
                // Append caller's extra selection if any
                if (selection != null && !selection.isEmpty()) {
                    sel = sel + " AND " + selection;
                }
                cursor = db.query("transactions",
                        projection, sel, selectionArgs,
                        null, null,
                        sortOrder != null ? sortOrder : "date_time DESC");
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        // Notify observers when data changes
        if (cursor != null && getContext() != null) {
            cursor.setNotificationUri(
                    getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    // ─────────────────────────────────────────────────────────────────────
    // INSERT — not used from UI, but required to implement ContentProvider
    // ─────────────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert("transactions", null, values);
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return Uri.withAppendedPath(CONTENT_URI, String.valueOf(id));
    }

    // ─────────────────────────────────────────────────────────────────────
    // UPDATE — not used from UI, but required
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.update("transactions", values, selection, selectionArgs);
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rows;
    }

    // ─────────────────────────────────────────────────────────────────────
    // DELETE — not used from UI, but required
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete("transactions", selection, selectionArgs);
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rows;
    }

    // ─────────────────────────────────────────────────────────────────────
    // getType — returns MIME type of the URI
    // ─────────────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case ALL_TRANSACTIONS:
                return "vnd.android.cursor.dir/vnd." + AUTHORITY + ".transactions";
            case USER_TRANSACTIONS:
                return "vnd.android.cursor.item/vnd." + AUTHORITY + ".transactions";
            default:
                return null;
        }
    }
}
