package com.mozre.mcamera;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class TapCaptureView extends View {
    private final Paint mBorderPaint = new Paint();
    private final Paint mContentPaint = new Paint();
    private int mBorderColor = Color.GRAY;
    private int mContentColor = Color.GRAY;
    private int mBorderWidth = 0;
    private int mBorderRadius = 0;


    public TapCaptureView(Context context) {
        super(context);
    }

    public TapCaptureView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TapCaptureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.TapCaptureView);
        mBorderColor = array.getColor(R.styleable.TapCaptureView_tap_border_color, Color.GRAY);
        mContentColor = array.getColor(R.styleable.TapCaptureView_tap_content_color, Color.GRAY);
        mBorderWidth = array.getDimensionPixelSize(R.styleable.TapCaptureView_tap_border_width, 0);
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
        mBorderRadius = Math.min(w/2, h/2);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(getWidth()/2,getHeight()/2,mBorderRadius, mBorderPaint);
    }

    private void init() {
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(mBorderWidth);
        mBorderPaint.setColor(mBorderColor);
    }

}
