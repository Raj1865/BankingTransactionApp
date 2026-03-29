package com.bankingapp.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.bankingapp.R;
import com.bankingapp.models.Transaction;

public class TransactionCardView extends View {

    // Custom attribute values
    private float   accentWidth;
    private float   cornerRadius;
    private boolean showShadow;

    // Paint objects — created ONCE, never inside onDraw()
    private Paint cardPaint;
    private Paint accentPaint;
    private Paint badgePaint;
    private Paint badgeTextPaint;
    private Paint descPaint;
    private Paint datePaint;
    private Paint amountPaint;
    private Paint ripplePaint;

    // Geometry — calculated once in onSizeChanged(), reused in onDraw()
    private final RectF cardRect   = new RectF();
    private final RectF accentRect = new RectF();
    private final RectF badgeRect  = new RectF();
    private final Path  clipPath   = new Path();

    private float textX, descY, dateY, amountX, amountY;
    private float pressAlpha = 0f;

    // Data
    private Transaction transaction;

    // Colors
    private static final int COLOR_SENT = Color.parseColor("#E53935");
    private static final int COLOR_RECV = Color.parseColor("#43A047");
    private static final int COLOR_BILL = Color.parseColor("#FB8C00");

    // ── All 3 constructors required for XML inflation ──────────────────────
    public TransactionCardView(Context context) {
        this(context, null);
    }
    public TransactionCardView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public TransactionCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readAttributes(context, attrs);
        initPaints();
        // Required for setShadowLayer() to work
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    // ── Read custom XML attributes ─────────────────────────────────────────
    private void readAttributes(Context context, AttributeSet attrs) {
        float dp     = getResources().getDisplayMetrics().density;
        accentWidth  = 6f  * dp;
        cornerRadius = 12f * dp;
        showShadow   = true;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TransactionCardView);
            try {
                accentWidth  = a.getDimension(R.styleable.TransactionCardView_accentWidth,  accentWidth);
                cornerRadius = a.getDimension(R.styleable.TransactionCardView_cornerRadius, cornerRadius);
                showShadow   = a.getBoolean(  R.styleable.TransactionCardView_showShadow,   showShadow);
            } finally {
                a.recycle(); // always recycle to avoid memory leak
            }
        }
    }

    // ── Set up all paints once ─────────────────────────────────────────────
    private void initPaints() {
        float sp = getResources().getDisplayMetrics().scaledDensity;

        cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardPaint.setColor(Color.WHITE);
        cardPaint.setStyle(Paint.Style.FILL);
        if (showShadow) cardPaint.setShadowLayer(10f, 0f, 4f, Color.argb(40, 0, 0, 0));

        accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        accentPaint.setStyle(Paint.Style.FILL);

        badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgePaint.setStyle(Paint.Style.FILL);

        badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeTextPaint.setColor(Color.WHITE);
        badgeTextPaint.setTextAlign(Paint.Align.CENTER);
        badgeTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        badgeTextPaint.setTextSize(13f * sp);

        descPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        descPaint.setColor(Color.parseColor("#212121"));
        descPaint.setTypeface(Typeface.DEFAULT_BOLD);
        descPaint.setTextSize(14f * sp);

        datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        datePaint.setColor(Color.parseColor("#757575"));
        datePaint.setTextSize(11f * sp);

        amountPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        amountPaint.setTypeface(Typeface.DEFAULT_BOLD);
        amountPaint.setTextSize(15f * sp);
        amountPaint.setTextAlign(Paint.Align.RIGHT);

        ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ripplePaint.setColor(Color.argb(30, 0, 0, 0));
        ripplePaint.setStyle(Paint.Style.FILL);
    }

    // ── Tell the layout system our preferred size ──────────────────────────
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float dp     = getResources().getDisplayMetrics().density;
        int desiredH = (int)(80f * dp);
        int w = resolveSize(MeasureSpec.getSize(widthMeasureSpec), widthMeasureSpec);
        int h = resolveSize(desiredH, heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    // ── Pre-compute all geometry when size is known ────────────────────────
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        float dp  = getResources().getDisplayMetrics().density;
        float pad = 8f * dp;

        cardRect.set(pad, pad, w - pad, h - pad);
        accentRect.set(pad, pad, pad + accentWidth, h - pad);

        float badgeDiam = 40f * dp;
        float bx = pad + accentWidth + 10f * dp;
        float by = (h - badgeDiam) / 2f;
        badgeRect.set(bx, by, bx + badgeDiam, by + badgeDiam);

        clipPath.reset();
        clipPath.addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW);

        textX   = badgeRect.right + 10f * dp;
        float midY = h / 2f;
        descY   = midY - 4f * dp;
        dateY   = midY + 14f * dp;
        amountX = w - 12f * dp;
        amountY = midY + 6f * dp;
    }

    // ── All drawing logic — zero allocations allowed here ─────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (transaction == null) return;

        int color = colorForType(transaction.getType());

        // 1. Card background
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardPaint);

        // 2. Clip to rounded card shape
        canvas.save();
        canvas.clipPath(clipPath);

        // 3. Left accent stripe
        accentPaint.setColor(color);
        canvas.drawRect(accentRect, accentPaint);

        // 4. Badge circle
        badgePaint.setColor(color);
        float cx = badgeRect.centerX(), cy = badgeRect.centerY();
        canvas.drawCircle(cx, cy, badgeRect.width() / 2f, badgePaint);

        // 5. Badge letter — vertically centred
        Paint.FontMetrics fm = badgeTextPaint.getFontMetrics();
        canvas.drawText(letterForType(transaction.getType()), cx,
                cy - (fm.ascent + fm.descent) / 2f, badgeTextPaint);

        // 6. Press ripple
        if (pressAlpha > 0f) {
            ripplePaint.setAlpha((int)(pressAlpha * 60));
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, ripplePaint);
        }
        canvas.restore();

        // 7. Description text
        canvas.drawText(truncate(transaction.getDescription(), 24), textX, descY, descPaint);

        // 8. Date text
        canvas.drawText(transaction.getDisplayDate(), textX, dateY, datePaint);

        // 9. Amount — right-aligned, colour by type
        boolean positive = Transaction.TYPE_RECEIVED.equals(transaction.getType());
        amountPaint.setColor(positive ? COLOR_RECV : COLOR_SENT);
        canvas.drawText(transaction.getFormattedAmount(), amountX, amountY, amountPaint);
    }

    // ── Animated press ripple ─────────────────────────────────────────────
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                animate(pressAlpha, 1f, 100); return true;
            case MotionEvent.ACTION_UP:
                animate(pressAlpha, 0f, 200); performClick(); return true;
            case MotionEvent.ACTION_CANCEL:
                animate(pressAlpha, 0f, 200); return true;
        }
        return super.onTouchEvent(event);
    }

    @Override public boolean performClick() { return super.performClick(); }

    private void animate(float from, float to, int ms) {
        ValueAnimator a = ValueAnimator.ofFloat(from, to);
        a.setDuration(ms);
        a.addUpdateListener(anim -> {
            pressAlpha = (float) anim.getAnimatedValue();
            invalidate();
        });
        a.start();
    }

    // ── Public API ────────────────────────────────────────────────────────
    public void setTransaction(Transaction txn) {
        this.transaction = txn;
        invalidate();
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private int colorForType(String type) {
        switch (type) {
            case Transaction.TYPE_RECEIVED:     return COLOR_RECV;
            case Transaction.TYPE_BILL_PAYMENT: return COLOR_BILL;
            default:                            return COLOR_SENT;
        }
    }
    private String letterForType(String type) {
        switch (type) {
            case Transaction.TYPE_RECEIVED:     return "R";
            case Transaction.TYPE_BILL_PAYMENT: return "B";
            default:                            return "S";
        }
    }
    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}