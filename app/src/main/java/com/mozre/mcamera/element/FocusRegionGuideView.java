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
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.mozre.mcamera.Constants;

import java.util.Currency;
import java.util.List;

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
        calcDrawRect(canvas);
        drawFaceRects(canvas);
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
        mPaint.setColor(Color.BLUE);
        mPaint.setStrokeWidth(5f);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    private void calcDrawRect(Canvas canvas) {
        if (mCurrentPointF.equals(0f, 0f)) {
            mCurrentDrawRect.left = mCurrentViewSize.centerX() - SQUARE_LENGTH / 2;
            mCurrentDrawRect.right = mCurrentViewSize.centerX() + SQUARE_LENGTH / 2;
            mCurrentDrawRect.top = mCurrentViewSize.centerY() - SQUARE_LENGTH / 2;
            mCurrentDrawRect.bottom = mCurrentViewSize.centerY() + SQUARE_LENGTH / 2;
        } else {
            mCurrentDrawRect.left = (int) (mCurrentPointF.x - SQUARE_LENGTH / 2);
            mCurrentDrawRect.right = (int) (mCurrentPointF.x + SQUARE_LENGTH / 2);
            mCurrentDrawRect.top = (int) (mCurrentPointF.y - SQUARE_LENGTH / 2);
            mCurrentDrawRect.bottom = (int) (mCurrentPointF.y + SQUARE_LENGTH / 2);
        }

        int rectWidth = mCurrentDrawRect.right - mCurrentDrawRect.left;
        int rectHeight = mCurrentDrawRect.bottom - mCurrentDrawRect.top;
        float diameter = rectHeight > rectWidth ? rectHeight : rectWidth;
        canvas.drawCircle(mCurrentDrawRect.centerX(), mCurrentDrawRect.centerY(), diameter / 2, mPaint);
        Log.d(TAG, "calcDrawRect mCurrentViewSize:" + mCurrentViewSize.toString() + " mCurrentDrawRect: " + mCurrentDrawRect.toString());
    }

    /*
        private void drawFaceRects(Canvas canvas) {
            if (mCurrentFaces == null || mCurrentFaces.length == 0) {
                return;
            }
            int rw, rh;
            rw = mSurfaceRect.width();
            rh = mSurfaceRect.height();
            if (((rh > rw) && ((mDisplayOrientation == 0) || (mDisplayOrientation == 180)))
                    || ((rw > rh) && ((mDisplayOrientation == 90) || (mDisplayOrientation == 270)))) {
                rw = rh + rh;
                rh = rw - rh;
                rw = rw - rh;
            }
            if (rw * mCameraBound.width() != mCameraBound.height() * rh) {
                if (rw == rh || (rh * 288 == rw * 352) || (rh * 480 == rw * 800)) {
                    rh = rw * mCameraBound.width() / mCameraBound.height();
                } else {
                    rw = rh * mCameraBound.height() / mCameraBound.width();
                }
            }
            this.initTransMatrix();
            int dx = (getWidth() - mSurfaceRect.width()) / 2;
            dx -= (rw - mSurfaceRect.width()) / 2;
            int dy = (getHeight() - mSurfaceRect.height()) / 2;
            dy -= (rh - mSurfaceRect.height()) / 2;
            canvas.save();
            mMatrix.postRotate(mOrientation); // postRotate is clockwise
            canvas.rotate(-mOrientation); // rotate is counter-clockwise (for canvas)
            Log.d(TAG, "drawFaceRects: mCurrentFaces.length:" + mCurrentFaces.length);
            RectF currentRectF = new RectF();
            for (int i = 0; i< mCurrentFaces.length; ++i) {
                if (mCurrentFaces[i].getScore() < 50) continue;
                Rect faceBound = mCurrentFaces[i].getBounds();
                android.util.Log.d(TAG, "onDraw  origin faces[i]: " + faceBound.toString());
                faceBound.offset(-mCameraBound.left, -mCameraBound.top);
                android.util.Log.d(TAG, "onDraw  after faces[i]: " + faceBound.toString());
                if (isFDRectOutOfBound(faceBound)) continue;
                currentRectF.set(faceBound);
                if (mZoom != 1.0f) {
                    currentRectF.left = currentRectF.left - mCameraBound.left;
                    currentRectF.right = currentRectF.right - mCameraBound.left;
                    currentRectF.top = currentRectF.top - mCameraBound.top;
                    currentRectF.bottom = currentRectF.bottom - mCameraBound.top;
                }
                android.util.Log.d(TAG, "onDraw  zoom fix faces[i]: " + currentRectF.toString());
                mTranslationMatrix.mapRect(currentRectF);
                android.util.Log.d(TAG, "onDraw translateMatrix mapRect fix faces[i]: " + currentRectF.toString());
                mMatrix.mapRect(currentRectF);
                android.util.Log.d(TAG, "onDraw mMatrix mapRect fix faces[i]: " + currentRectF.toString());
                currentRectF.offset(dx, dy);
                Log.d(TAG, "onDraw: mRect.offset dx: " + dx + " dy: " + dy);
                android.util.Log.d(TAG, "onDraw mRect offset faces[i]: " + currentRectF.toString());
                canvas.drawRect(currentRectF, mPaint);
            }
        }
    */
    private boolean isFDRectOutOfBound(Rect faceRect) {
        return mCameraBound.left > faceRect.left || mCameraBound.top > faceRect.top ||
                faceRect.right > mCameraBound.right || faceRect.bottom > mCameraBound.bottom;
    }

    public void touchPointNotify(PointF pointF) {
        mCurrentPointF.set(pointF);
        invalidate();
    }

    public void notifyFaceDetectChange(Face[] faces) {
        RectF rectFs = new RectF(386.51566f, 699.8125f, 730.34375f, 1043.9922f);
//        rects = new ArrayList<>();
//        rects.add(rectFs);
        this.mCurrentFaces = faces;
        invalidate();
//        requestLayout();
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
}
