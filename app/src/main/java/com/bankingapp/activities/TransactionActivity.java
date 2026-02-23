package com.bankingapp.activities;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bankingapp.R;
import com.bankingapp.models.Transaction;
import com.bankingapp.providers.TransactionProvider;
import com.bankingapp.utils.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TransactionActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────
    private Button   btnFromDate, btnToDate, btnApplyFilter, btnClearFilter;
    private Spinner  spinnerFilter;
    private ListView listTransactions;
    private TextView tvTotalSent, tvTotalReceived, tvTxnCount, tvEmptyList;

    // ── State ─────────────────────────────────────────────────────────────
    private String selectedFromDate = "";   // 'yyyy-MM-dd'
    private String selectedToDate   = "";   // 'yyyy-MM-dd'
    private List<Transaction> transactionList = new ArrayList<>();
    private TransactionDetailAdapter adapter;
    private int selectedContextPosition = -1;  // for context menu

    private SessionManager session;

    // ── Filter type options ────────────────────────────────────────────────
    private final String[] FILTER_TYPES = {
            "All", "Sent", "Received", "Bill Payment"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Transaction History");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        session = new SessionManager(this);

        bindViews();
        setupSpinner();
        setupDateButtons();
        setupFilterButtons();
        loadTransactions("", "", "All");
    }

    // ── Bind views ────────────────────────────────────────────────────────
    private void bindViews() {
        btnFromDate      = findViewById(R.id.btnFromDate);
        btnToDate        = findViewById(R.id.btnToDate);
        btnApplyFilter   = findViewById(R.id.btnApplyFilter);
        btnClearFilter   = findViewById(R.id.btnClearFilter);
        spinnerFilter    = findViewById(R.id.spinnerFilter);
        listTransactions = findViewById(R.id.listTransactions);
        tvTotalSent      = findViewById(R.id.tvTotalSent);
        tvTotalReceived  = findViewById(R.id.tvTotalReceived);
        tvTxnCount       = findViewById(R.id.tvTxnCount);
        tvEmptyList      = findViewById(R.id.tvEmptyList);
    }

    // ── Setup Spinner with filter types ───────────────────────────────────
    private void setupSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, FILTER_TYPES);
        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(spinnerAdapter);
    }

    // ── Date picker buttons ───────────────────────────────────────────────
    private void setupDateButtons() {
        btnFromDate.setOnClickListener(v -> showDatePicker(true));
        btnToDate.setOnClickListener(v   -> showDatePicker(false));
    }

    // ── DatePickerDialog — syllabus requirement ───────────────────────────
    private void showDatePicker(boolean isFromDate) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    // month is 0-indexed, so +1
                    String date = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    if (isFromDate) {
                        selectedFromDate = date;
                        btnFromDate.setText(date);
                    } else {
                        selectedToDate = date;
                        btnToDate.setText(date);
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    // ── Apply / Clear filter buttons ──────────────────────────────────────
    private void setupFilterButtons() {
        btnApplyFilter.setOnClickListener(v -> {
            String typeFilter = spinnerFilter.getSelectedItem().toString();
            loadTransactions(selectedFromDate, selectedToDate, typeFilter);
        });

        btnClearFilter.setOnClickListener(v -> {
            selectedFromDate = "";
            selectedToDate   = "";
            btnFromDate.setText("Select Date");
            btnToDate.setText("Select Date");
            spinnerFilter.setSelection(0);
            loadTransactions("", "", "All");
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // CORE: Load transactions via ContentProvider
    // ─────────────────────────────────────────────────────────────────────
    private void loadTransactions(String fromDate, String toDate,
                                  String typeFilter) {
        transactionList.clear();

        int    userId = session.getUserId();

        // Build URI: content://com.bankingapp.provider/transactions/{userId}
        Uri uri = Uri.withAppendedPath(
                TransactionProvider.CONTENT_URI,
                String.valueOf(userId));

        // Build WHERE clause based on filters
        StringBuilder selection = new StringBuilder();
        List<String>  selArgs   = new ArrayList<>();

        // Date range filter
        if (!fromDate.isEmpty() && !toDate.isEmpty()) {
            selection.append("date_time BETWEEN ? AND ?");
            selArgs.add(fromDate + " 00:00:00");
            selArgs.add(toDate   + " 23:59:59");
        } else if (!fromDate.isEmpty()) {
            selection.append("date_time >= ?");
            selArgs.add(fromDate + " 00:00:00");
        } else if (!toDate.isEmpty()) {
            selection.append("date_time <= ?");
            selArgs.add(toDate + " 23:59:59");
        }

        // Type filter
        if (!typeFilter.equals("All")) {
            if (selection.length() > 0) selection.append(" AND ");
            String dbType = typeFilterToDbValue(typeFilter);
            selection.append("type = ?");
            selArgs.add(dbType);
        }

        String selString = selection.length() > 0 ? selection.toString() : null;
        String[] selArgsArr = selArgs.isEmpty() ? null :
                selArgs.toArray(new String[0]);

        // ── Query via ContentProvider (not DatabaseHelper directly) ───────
        Cursor cursor = getContentResolver().query(
                uri,
                null,           // all columns
                selString,
                selArgsArr,
                "date_time DESC"
        );

        double totalSent     = 0;
        double totalReceived = 0;

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Transaction txn = new Transaction(cursor);
                transactionList.add(txn);

                // Accumulate totals
                if (txn.getType().equals(Transaction.TYPE_SENT) ||
                        txn.getType().equals(Transaction.TYPE_BILL_PAYMENT)) {
                    totalSent += txn.getAmount();
                } else if (txn.getType().equals(Transaction.TYPE_RECEIVED)) {
                    totalReceived += txn.getAmount();
                }
            }
            cursor.close();
        }

        // Update summary strip
        tvTotalSent.setText(String.format("Sent: ₹ %,.2f", totalSent));
        tvTotalReceived.setText(String.format("Received: ₹ %,.2f", totalReceived));
        tvTxnCount.setText("Total: " + transactionList.size());

        // Show/hide empty state
        if (transactionList.isEmpty()) {
            tvEmptyList.setVisibility(View.VISIBLE);
            listTransactions.setVisibility(View.GONE);
        } else {
            tvEmptyList.setVisibility(View.GONE);
            listTransactions.setVisibility(View.VISIBLE);
        }

        // Set or refresh adapter
        if (adapter == null) {
            adapter = new TransactionDetailAdapter(transactionList);
            listTransactions.setAdapter(adapter);
            // Register ListView for Context Menu
            registerForContextMenu(listTransactions);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    // ── Map Spinner display text to DB column value ────────────────────────
    private String typeFilterToDbValue(String displayText) {
        switch (displayText) {
            case "Sent":         return Transaction.TYPE_SENT;
            case "Received":     return Transaction.TYPE_RECEIVED;
            case "Bill Payment": return Transaction.TYPE_BILL_PAYMENT;
            default:             return "";
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONTEXT MENU — long press on a transaction row
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.listTransactions) {
            AdapterView.AdapterContextMenuInfo info =
                    (AdapterView.AdapterContextMenuInfo) menuInfo;
            selectedContextPosition = info.position;
            Transaction txn = transactionList.get(info.position);
            menu.setHeaderTitle(txn.getDescription());
            menu.add(Menu.NONE, 1, Menu.NONE, "View Full Details");
            menu.add(Menu.NONE, 2, Menu.NONE, "View Location");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (selectedContextPosition < 0) return false;
        Transaction txn = transactionList.get(selectedContextPosition);

        switch (item.getItemId()) {
            case 1:
                showTransactionDetailDialog(txn);
                return true;
            case 2:
                showLocationDialog(txn);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    // ── Full detail dialog ────────────────────────────────────────────────
    private void showTransactionDetailDialog(Transaction txn) {
        String details =
                "Type:        " + txn.getType()        + "\n" +
                        "Category:    " + txn.getCategory()    + "\n" +
                        "Amount:      " + txn.getFormattedAmount() + "\n" +
                        "To/From:     " + txn.getToFromName()  + "\n" +
                        "Phone:       " + txn.getToFromPhone() + "\n" +
                        "Date & Time: " + txn.getDateTime()    + "\n" +
                        "Status:      " + txn.getStatus();

        new AlertDialog.Builder(this)
                .setTitle("Transaction Details")
                .setMessage(details)
                .setPositiveButton("Close", (d, w) -> d.dismiss())
                .show();
    }

    // ── Location dialog ───────────────────────────────────────────────────
    private void showLocationDialog(Transaction txn) {
        String locInfo;
        if (txn.getLatitude() == 0.0 && txn.getLongitude() == 0.0) {
            locInfo = "Location was not recorded for this transaction.\n" +
                    "(Permission may have been denied or GPS unavailable)";
        } else {
            locInfo = String.format(Locale.getDefault(),
                    "Latitude:  %.6f\nLongitude: %.6f\n\n" +
                            "Transaction recorded at this location on:\n%s",
                    txn.getLatitude(), txn.getLongitude(), txn.getDateTime());
        }

        new AlertDialog.Builder(this)
                .setTitle("Transaction Location")
                .setMessage(locInfo)
                .setIcon(android.R.drawable.ic_menu_mylocation)
                .setPositiveButton("OK", (d, w) -> d.dismiss())
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────
    // INNER ADAPTER — uses item_transaction_detail.xml
    // ─────────────────────────────────────────────────────────────────────
    private class TransactionDetailAdapter extends ArrayAdapter<Transaction> {

        TransactionDetailAdapter(List<Transaction> list) {
            super(TransactionActivity.this,
                    R.layout.item_transaction_detail, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(
                        R.layout.item_transaction_detail, parent, false);
            }

            Transaction txn = transactionList.get(position);

            TextView tvIcon    = convertView.findViewById(R.id.tvDetailTypeIcon);
            TextView tvDesc    = convertView.findViewById(R.id.tvDetailDescription);
            TextView tvToFrom  = convertView.findViewById(R.id.tvDetailToFrom);
            TextView tvDate    = convertView.findViewById(R.id.tvDetailDate);
            TextView tvCat    = convertView.findViewById(R.id.tvDetailCategory);
            TextView tvAmount  = convertView.findViewById(R.id.tvDetailAmount);

            tvDesc.setText(txn.getDescription());
            tvToFrom.setText(txn.getToFromName());
            tvDate.setText(txn.getDateTime());
            tvCat.setText(txn.getCategory());
            tvAmount.setText(txn.getFormattedAmount());

            switch (txn.getType()) {
                case Transaction.TYPE_RECEIVED:
                    tvIcon.setText("R");
                    tvIcon.setBackgroundColor(Color.parseColor("#43A047"));
                    tvAmount.setTextColor(Color.parseColor("#43A047"));
                    break;
                case Transaction.TYPE_BILL_PAYMENT:
                    tvIcon.setText("B");
                    tvIcon.setBackgroundColor(Color.parseColor("#FB8C00"));
                    tvAmount.setTextColor(Color.parseColor("#E53935"));
                    break;
                default:
                    tvIcon.setText("S");
                    tvIcon.setBackgroundColor(Color.parseColor("#E53935"));
                    tvAmount.setTextColor(Color.parseColor("#E53935"));
                    break;
            }

            return convertView;
        }
    }

    // ── Back arrow in action bar ──────────────────────────────────────────
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
