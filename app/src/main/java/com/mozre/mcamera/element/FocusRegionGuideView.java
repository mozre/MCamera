package com.mozre.mcamera.element;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.camera2.params.Face;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.mozre.mcamera.Constants;

public class FocusRegionGuideView extends View {
    private static final String TAG = Constants.getTagName(FocusRegionGuideView.class.getSimpleName());
    private static final int SQUARE_LENGTH = 200;
    private Paint mPaint = new Paint();
    private PointF mCurrentPointF = new PointF(0f, 0f);
    private Face[] mCurrentFaces;
    private Rect mCurrentDrawRect = new Rect();
    private Rect mCurrentViewSize = new Rect();
    private Matrix mMatrix = new Matrix();
    private Matrix mTranslationMatrix = new Matrix();
    private int mDisplayOrientation;
    private int mOrientation;
    private Rect mCameraBound;
    private Rect mSurfaceRect = new Rect();
    private float mZoom = 1.0f;
    private int mColorAf = Color.BLUE;
    private Handler mHandler = new Handler();

    public FocusRegionGuideView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FocusRegionGuideView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FocusRegionGuideView(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mCurrentViewSize.set(left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        initState();
        drawFaceRects(canvas);
        drawFocusRect(canvas);
    }

    private void drawFaceRects(Canvas canvas) {
        if (mCurrentFaces == null || mCurrentFaces.length == 0) {
            return;
        }
        RectF fixRectF;
        float rateRef = (mCameraBound.width() * 1.0f) / Constants.PREVIEW_SIZE.getWidth();
        for (Face face : mCurrentFaces) {
            Rect faceRect = face.getBounds();
            fixRectF = new RectF();
            fixRectF.left = (mCameraBound.height() - faceRect.top) / rateRef;
            fixRectF.top = faceRect.left / rateRef;
            fixRectF.right = (mCameraBound.height() - faceRect.bottom) / rateRef;
            fixRectF.bottom = faceRect.right / rateRef;
            canvas.drawRect(fixRectF, mPaint);
        }
    }

    private void initState() {
        mPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(5f);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    private void drawFocusRect(Canvas canvas) {
        if (mCurrentPointF.equals(0f, 0f)) {
            return;
        }
        mCurrentDrawRect.left = (int) (mCurrentPointF.x - SQUARE_LENGTH / 2);
        mCurrentDrawRect.right = (int) (mCurrentPointF.x + SQUARE_LENGTH / 2);
        mCurrentDrawRect.top = (int) (mCurrentPointF.y - SQUARE_LENGTH / 2);
        mCurrentDrawRect.bottom = (int) (mCurrentPointF.y + SQUARE_LENGTH / 2);
        int rectWidth = mCurrentDrawRect.right - mCurrentDrawRect.left;
        int rectHeight = mCurrentDrawRect.bottom - mCurrentDrawRect.top;
        float diameter = rectHeight > rectWidth ? rectHeight : rectWidth;
        mPaint.setColor(mColorAf);
        canvas.drawCircle(mCurrentDrawRect.centerX(), mCurrentDrawRect.centerY(), diameter / 2, mPaint);
        Log.d(TAG, "drawFocusRect mCurrentViewSize:" + mCurrentViewSize.toString() + " mCurrentDrawRect: " + mCurrentDrawRect.toString());
    }

    private boolean isFDRectOutOfBound(Rect faceRect) {
        return mCameraBound.left > faceRect.left || mCameraBound.top > faceRect.top ||
                faceRect.right > mCameraBound.right || faceRect.bottom > mCameraBound.bottom;
    }

    public void touchPointNotify(PointF pointF) {
        mCurrentPointF.set(pointF);
        mColorAf = Color.RED;
        invalidate();
    }

    public void notifyFaceDetectChange(Face[] faces) {
        this.mCurrentFaces = faces;
        invalidate();
    }

    public void initTransMatrix() {
        Log.d(TAG, "initTransMatrix: mCameraBound: " + mCameraBound.toString() + " mSurfaceRect: " + mSurfaceRect);
        mTranslationMatrix.preTranslate(-mCameraBound.width() / 2f, -mCameraBound.height() / 2f);
        mTranslationMatrix.postScale(2000f / mCameraBound.width(), 2000f / mCameraBound.height());
        mMatrix.setScale(1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        mMatrix.postRotate(mDisplayOrientation);
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        mMatrix.postScale(mSurfaceRect.width() / 2000f, mSurfaceRect.height() / 2000f);
        mMatrix.postTranslate(mSurfaceRect.width() / 2f, mSurfaceRect.height() / 2f);
        Log.d(TAG, "initTransMatrix: mTranslationMatrix: " + mTranslationMatrix.toString() + " mMatrix: " + mMatrix.toString());
    }

    public void updateOrientation(int cameraRotation, int displayOrientation) {
        this.mDisplayOrientation = displayOrientation;
        this.mOrientation = cameraRotation;
    }

    public void setCameraBound(Rect rect) {
        this.mCameraBound = rect;
    }

    public void updateSurfaceViewLayoutChange(int width, int height) {
        mSurfaceRect.left = 0;
        mSurfaceRect.right = width;
        mSurfaceRect.top = 0;
        mSurfaceRect.bottom = height;
    }

    public void updateAfStateChanged(boolean state) {
        if (state) {
            mColorAf = Color.GREEN;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mColorAf = Color.RED;
                    mCurrentDrawRect.setEmpty();
                }
            }, Constants.CANCEL_TOUCH_FOCUS_DELAY);

        } else {
            mColorAf = Color.RED;
        }
    }

    public void clear() {
        mCurrentFaces = null;
        mCurrentDrawRect.setEmpty();
        mCurrentPointF.set(0f, 0f);
        invalidate();
    }
}
