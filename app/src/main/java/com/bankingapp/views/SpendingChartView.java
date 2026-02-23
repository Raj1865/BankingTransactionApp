package com.bankingapp.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SpendingChartView — Custom View that draws a bar chart of spending by category.
 * Syllabus coverage: Canvas, onDraw(), Paint, custom View.
 */
public class SpendingChartView extends View {

    // ── Data ──────────────────────────────────────────────────────────────
    private List<String> labels = new ArrayList<>();
    private List<Double> values = new ArrayList<>();
    private double maxValue = 1.0; // avoid divide-by-zero

    // ── Paint objects (created once in init, NOT inside onDraw) ──────────
    private Paint barPaint;       // fills the bar
    private Paint labelPaint;     // category name below bar
    private Paint valuePaint;     // amount text above bar
    private Paint axisPaint;      // X-axis baseline
    private Paint titlePaint;     // chart title
    private Paint emptyPaint;     // 'No data' message

    // ── Bar colours (cycles through these for each category) ─────────────
    private static final int[] BAR_COLORS = {
            Color.parseColor("#1A237E"), // Transfer — dark blue
            Color.parseColor("#FB8C00"), // Electricity — orange
            Color.parseColor("#00ACC1"), // Water — teal
            Color.parseColor("#43A047"), // Recharge — green
            Color.parseColor("#E53935"), // Other — red
    };

    // ── Dimensions (in pixels, set during onSizeChanged) ────────────────
    private float chartLeft;
    private float chartTop;
    private float chartRight;
    private float chartBottom;

    // ── Constructors (all 3 needed for XML inflation) ─────────────────────
    public SpendingChartView(Context context) {
        super(context);
        init();
    }

    public SpendingChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpendingChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    // ── Initialise all Paint objects ─────────────────────────────────────
    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.parseColor("#212121"));
        labelPaint.setTextSize(spToPx(11));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.DEFAULT);

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(Color.parseColor("#FFFFFF"));
        valuePaint.setTextSize(spToPx(10));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTypeface(Typeface.DEFAULT_BOLD);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.parseColor("#BDBDBD"));
        axisPaint.setStrokeWidth(dpToPx(1.5f));
        axisPaint.setStyle(Paint.Style.STROKE);

        titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#1A237E"));
        titlePaint.setTextSize(spToPx(13));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);

        emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emptyPaint.setColor(Color.parseColor("#9E9E9E"));
        emptyPaint.setTextSize(spToPx(13));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
    }

    // ── Public API: set data and trigger redraw ───────────────────────────
    public void setData(Map<String, Double> spendingMap) {
        labels.clear();
        values.clear();
        maxValue = 1.0;

        if (spendingMap != null) {
            for (Map.Entry<String, Double> entry : spendingMap.entrySet()) {
                labels.add(entry.getKey());
                values.add(entry.getValue());
                if (entry.getValue() > maxValue) {
                    maxValue = entry.getValue();
                }
            }
        }

        invalidate(); // triggers onDraw()
    }

    // ── Calculate chart area on size change ──────────────────────────────
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        float paddingH = dpToPx(16);
        float paddingTop = dpToPx(40); // space for title
        float paddingBottom = dpToPx(48); // space for labels
        chartLeft   = paddingH;
        chartTop    = paddingTop;
        chartRight  = w - paddingH;
        chartBottom = h - paddingBottom;
    }

    // ── CORE: draw the chart ──────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;

        // Draw chart title
        canvas.drawText("Spending by Category", centerX, chartTop - dpToPx(8), titlePaint);

        // Draw X-axis baseline
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint);

        // Show 'No data' if empty
        if (labels.isEmpty()) {
            canvas.drawText("No spending data for selected month",
                    centerX, (chartTop + chartBottom) / 2f, emptyPaint);
            return;
        }

        int count = labels.size();
        float totalWidth = chartRight - chartLeft;
        float barWidth = (totalWidth / count) * 0.6f;
        float gap = (totalWidth / count) * 0.4f;
        float slotWidth = barWidth + gap;
        float chartHeight = chartBottom - chartTop;

        for (int i = 0; i < count; i++) {
            double value = values.get(i);
            float ratio = (float)(value / maxValue);

            // Bar rectangle
            float left   = chartLeft + i * slotWidth + gap / 2f;
            float right  = left + barWidth;
            float top    = chartBottom - (ratio * chartHeight * 0.85f);
            float bottom = chartBottom;

            // Draw bar
            barPaint.setColor(BAR_COLORS[i % BAR_COLORS.length]);
            canvas.drawRoundRect(new RectF(left, top, right, bottom), dpToPx(4), dpToPx(4), barPaint);

            // Amount above bar (inside bar if tall enough)
            String amtLabel = "\u20B9" + formatAmount(value);
            float textY = top - dpToPx(4);
            if (textY < chartTop + dpToPx(16)) textY = top + spToPx(12);
            valuePaint.setColor(BAR_COLORS[i % BAR_COLORS.length]);
            canvas.drawText(amtLabel, left + barWidth / 2f, textY, valuePaint);

            // Category label below X-axis
            canvas.drawText(labels.get(i), left + barWidth / 2f,
                    chartBottom + dpToPx(18), labelPaint);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private String formatAmount(double value) {
        if (value >= 1000) {
            return String.format("%.1fk", value / 1000);
        }
        return String.format("%.0f", value);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    // ── Minimum measured size so the view has space ───────────────────────
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth  = MeasureSpec.getSize(widthMeasureSpec);
        int desiredHeight = (int)(desiredWidth * 0.55f); // 55% aspect ratio
        setMeasuredDimension(desiredWidth, desiredHeight);
    }
}
