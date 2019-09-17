package com.mozre.mcamera.element;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.mozre.mcamera.R;
import com.mozre.mcamera.utils.Constants;

public class TapCaptureView extends View {
    private static final String TAG = Constants.getTagName(TapCaptureView.class.getSimpleName());
    private final Paint mBorderPaint = new Paint();
    private final Paint mContentPaint = new Paint();
    private int mBorderNormalColor = Color.GRAY;
    private int mBorderActiveColor = Color.GRAY;
    private int mContentNormalColor = Color.WHITE;
    private int mContentActiveColor = Color.WHITE;
    private int mBorderWidth = 0;
    private int mBorderRadius = 0;
    private int mContentRadius = 0;
    private float mDpScale = 0f;


    public TapCaptureView(Context context) {
        super(context);
    }

    public TapCaptureView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TapCaptureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.TapCaptureView);
        mBorderNormalColor = array.getColor(R.styleable.TapCaptureView_tap_border_normal_color, Color.GRAY);
        mBorderActiveColor = array.getColor(R.styleable.TapCaptureView_tap_border_active_color, Color.WHITE);
        mContentNormalColor = array.getColor(R.styleable.TapCaptureView_tap_border_normal_color, Color.WHITE);
        mContentActiveColor = array.getColor(R.styleable.TapCaptureView_tap_content_active_color, Color.GRAY);
        mBorderWidth = array.getDimensionPixelSize(R.styleable.TapCaptureView_tap_border_width, 0);
        mDpScale = context.getResources().getDisplayMetrics().density;
        array.recycle();
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBorderRadius = Math.min(w / 2, h / 2);
        mContentRadius = (int) (mBorderRadius - mDpScale*3);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, mBorderRadius, mBorderPaint);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, mContentRadius, mContentPaint);
    }

    public void touchShutter() {
        Log.d(TAG, "touchShutter: mmmmmmmmmmmmmmmmmmmm");
        mBorderPaint.setColor(mBorderActiveColor);
        mContentPaint.setColor(mContentActiveColor);
        invalidate();
    }

    public void cancelShutter() {
        Log.d(TAG, "cancelShutter: mmmmmmmmmmmmmmmmmmmm");
        mBorderPaint.setColor(mBorderNormalColor);
        mContentPaint.setColor(mContentNormalColor);
        invalidate();
    }

    private void init() {
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setStyle(Paint.Style.FILL);
        mBorderPaint.setStrokeWidth(mBorderWidth);
        mBorderPaint.setColor(mBorderNormalColor);
        mContentPaint.setAntiAlias(true);
        mContentPaint.setStyle(Paint.Style.FILL);
        mContentPaint.setColor(mContentNormalColor);
    }

}
