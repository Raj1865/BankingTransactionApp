package com.bankingapp.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bankingapp.R;
import com.bankingapp.database.DatabaseHelper;
import com.bankingapp.models.SavingsGoal;
import com.bankingapp.utils.SessionManager;
import com.bankingapp.views.SpendingChartView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InsightsActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────
    private Spinner spinnerMonth;
    private TextView tvTotalSpent, tvTotalReceived;
    private SpendingChartView spendingChart;
    private RatingBar ratingBarApp;
    private TextView tvRatingMessage;
    private EditText etGoalName, etGoalTarget;
    private Button btnAddGoal;
    private ListView listGoals;
    private TextView tvNoGoals;

    // ── Helpers ───────────────────────────────────────────────────────────
    private DatabaseHelper db;
    private SessionManager session;
    private GoalAdapter goalAdapter;
    private List<SavingsGoal> goalList = new ArrayList<>();

    // ── Month options shown in Spinner (last 6 months) ───────────────────
    private List<String> monthLabels = new ArrayList<>(); // display: "Feb 2026"
    private List<String> monthKeys   = new ArrayList<>(); // DB format: "2026-02"

    // ── SharedPreferences key for saving rating ───────────────────────────
    private static final String PREF_INSIGHTS = "InsightsPrefs";
    private static final String KEY_RATING    = "appRating";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insights);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Insights");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db      = new DatabaseHelper(this);
        session = new SessionManager(this);

        bindViews();
        buildMonthList();
        setupMonthSpinner();
        setupRatingBar();
        setupGoalForm();
        loadGoals();
    }

    // ── Bind all views ────────────────────────────────────────────────────
    private void bindViews() {
        spinnerMonth     = findViewById(R.id.spinnerMonth);
        tvTotalSpent     = findViewById(R.id.tvTotalSpent);
        tvTotalReceived  = findViewById(R.id.tvTotalReceived);
        spendingChart    = findViewById(R.id.spendingChart);
        ratingBarApp     = findViewById(R.id.ratingBarApp);
        tvRatingMessage  = findViewById(R.id.tvRatingMessage);
        etGoalName       = findViewById(R.id.etGoalName);
        etGoalTarget     = findViewById(R.id.etGoalTarget);
        btnAddGoal       = findViewById(R.id.btnAddGoal);
        listGoals        = findViewById(R.id.listGoals);
        tvNoGoals        = findViewById(R.id.tvNoGoals);
    }

    // ── Build a list of the last 6 months for the Spinner ────────────────
    private void buildMonthList() {
        SimpleDateFormat labelFmt = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        SimpleDateFormat keyFmt   = new SimpleDateFormat("yyyy-MM",  Locale.getDefault());

        java.util.Calendar cal = java.util.Calendar.getInstance();
        for (int i = 0; i < 6; i++) {
            monthLabels.add(labelFmt.format(cal.getTime()));
            monthKeys.add(keyFmt.format(cal.getTime()));
            cal.add(java.util.Calendar.MONTH, -1);
        }
    }

    // ── Setup month Spinner ───────────────────────────────────────────────
    private void setupMonthSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                monthLabels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(adapter);

        // Load data for the currently selected month
        spinnerMonth.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent,
                                       View view, int position, long id) {
                loadInsightsForMonth(monthKeys.get(position));
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    // ── Load spending data from DB and update chart + totals ──────────────
    private void loadInsightsForMonth(String monthKey) {
        int userId = session.getUserId();

        // 1. Total spent this month (SENT + BILL_PAYMENT)
        double totalSpent = db.getTotalSpentThisMonth(userId, monthKey);
        tvTotalSpent.setText(String.format("₹ %,.2f", totalSpent));

        // 2. Total received this month
        double totalReceived = db.getTotalReceivedThisMonth(userId, monthKey);
        tvTotalReceived.setText(String.format("₹ %,.2f", totalReceived));

        // 3. Spending breakdown by category → feed into the chart
        Map<String, Double> categoryMap = db.getSpendingByCategory(userId, monthKey);
        spendingChart.setData(categoryMap); // triggers onDraw() via invalidate()
    }

    // ── Setup RatingBar — saves rating to SharedPreferences ───────────────
    private void setupRatingBar() {
        // Restore saved rating
        SharedPreferences prefs = getSharedPreferences(PREF_INSIGHTS, Context.MODE_PRIVATE);
        float savedRating = prefs.getFloat(KEY_RATING, 0f);
        ratingBarApp.setRating(savedRating);
        updateRatingMessage(savedRating);

        // Listen for changes
        ratingBarApp.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                // Save to SharedPreferences
                prefs.edit().putFloat(KEY_RATING, rating).apply();
                updateRatingMessage(rating);
                Toast.makeText(this,
                        "Thanks for rating us " + (int) rating + " star" + (rating == 1 ? "" : "s") + "!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Update the text shown below the RatingBar ─────────────────────────
    private void updateRatingMessage(float rating) {
        String msg;
        if (rating == 0) msg = getString(R.string.lbl_rating_prompt);
        else if (rating <= 2) msg = "We'll work harder to improve!";
        else if (rating <= 3) msg = "Thanks for the feedback!";
        else if (rating <= 4) msg = "Glad you're enjoying the app!";
        else msg = "Excellent! Thank you for the 5 stars! ⭐";
        tvRatingMessage.setText(msg);
    }

    // ── Savings Goal form ─────────────────────────────────────────────────
    private void setupGoalForm() {
        btnAddGoal.setOnClickListener(v -> handleAddGoal());
    }

    private void handleAddGoal() {
        String name   = etGoalName.getText().toString().trim();
        String tgtStr = etGoalTarget.getText().toString().trim();

        if (name.isEmpty()) {
            etGoalName.setError("Enter a goal name");
            etGoalName.requestFocus();
            return;
        }
        if (tgtStr.isEmpty()) {
            etGoalTarget.setError("Enter a target amount");
            etGoalTarget.requestFocus();
            return;
        }

        double target;
        try {
            target = Double.parseDouble(tgtStr);
        } catch (NumberFormatException e) {
            etGoalTarget.setError("Invalid amount");
            return;
        }

        if (target <= 0) {
            etGoalTarget.setError("Target must be greater than 0");
            return;
        }

        // Insert into DB
        SavingsGoal goal = new SavingsGoal(session.getUserId(), name, target);
        boolean success = db.insertGoal(goal);

        if (success) {
            Toast.makeText(this, "Goal \"" + name + "\" added!", Toast.LENGTH_SHORT).show();
            etGoalName.setText("");
            etGoalTarget.setText("");
            loadGoals(); // refresh list
        } else {
            Toast.makeText(this, "Failed to add goal", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Load savings goals from DB and update ListView ────────────────────
    private void loadGoals() {
        goalList.clear();
        goalList.addAll(db.getGoalsByUser(session.getUserId()));

        if (goalList.isEmpty()) {
            tvNoGoals.setVisibility(View.VISIBLE);
            listGoals.setVisibility(View.GONE);
        } else {
            tvNoGoals.setVisibility(View.GONE);
            listGoals.setVisibility(View.VISIBLE);
        }

        if (goalAdapter == null) {
            goalAdapter = new GoalAdapter(this, goalList);
            listGoals.setAdapter(goalAdapter);
            // Make ListView non-scrollable (inside ScrollView)
            listGoals.setOnTouchListener((v, event) -> false);
        } else {
            goalAdapter.notifyDataSetChanged();
        }

        // Force ListView to show all items without internal scrolling
        adjustListViewHeight(listGoals);
    }

    // ── Make ListView expand to show all items (needed inside ScrollView) ─
    private void adjustListViewHeight(ListView lv) {
        if (lv.getAdapter() == null) return;
        int totalHeight = 0;
        for (int i = 0; i < lv.getAdapter().getCount(); i++) {
            View item = lv.getAdapter().getView(i, null, lv);
            item.measure(0, 0);
            totalHeight += item.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = lv.getLayoutParams();
        params.height = totalHeight + (lv.getDividerHeight() * (lv.getAdapter().getCount() - 1));
        lv.setLayoutParams(params);
        lv.requestLayout();
    }

    // ── Back navigation ───────────────────────────────────────────────────
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    // ─────────────────────────────────────────────────────────────────────
    // INNER CLASS: GoalAdapter — binds SavingsGoal list to item_goal.xml
    // ─────────────────────────────────────────────────────────────────────
    private static class GoalAdapter extends ArrayAdapter<SavingsGoal> {

        GoalAdapter(Context ctx, List<SavingsGoal> list) {
            super(ctx, R.layout.item_goal, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_goal, parent, false);
            }

            SavingsGoal goal = getItem(position);
            if (goal == null) return convertView;

            TextView tvName    = convertView.findViewById(R.id.tvGoalName);
            TextView tvPercent = convertView.findViewById(R.id.tvGoalPercent);
            ProgressBar pgBar  = convertView.findViewById(R.id.progressGoal);
            TextView tvSaved   = convertView.findViewById(R.id.tvGoalSaved);
            TextView tvTarget  = convertView.findViewById(R.id.tvGoalTarget);

            tvName.setText(goal.getGoalName());

            int pct = goal.getProgressPercent();
            tvPercent.setText(pct + "%");
            pgBar.setProgress(pct);

            tvSaved.setText(String.format("Saved: ₹ %,.2f", goal.getCurrentAmount()));
            tvTarget.setText(String.format("Target: ₹ %,.2f", goal.getTargetAmount()));

            return convertView;
        }
    }
}
