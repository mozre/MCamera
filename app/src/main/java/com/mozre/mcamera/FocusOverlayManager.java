package com.mozre.mcamera;

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.Face;
import android.util.Log;
import android.util.Size;

import com.mozre.mcamera.element.FocusRegionGuideView;

import java.util.LinkedList;
import java.util.List;

import static android.support.v4.math.MathUtils.clamp;

public class FocusOverlayManager {
    private static final String TAG = Constants.getTagName(FocusOverlayManager.class.getSimpleName());
    private CameraCharacteristics mCameraCharacteristics;
    private Rect mScreenRect;
    private Rect mPreviewRect;
    private FocusRegionGuideView mFocusRegionGuideView;
    private int mDisplayOrientation;
    private int mCameraRotation;
    private Rect mSensorBoundRect;
    private OnAfAeRoiChange mOnAfAeRoiChangeCallback;

    public FocusOverlayManager(FocusRegionGuideView focusRegionGuideView, CameraCharacteristics cameraCharacteristics, OnAfAeRoiChange onAfAeRoiChange) {
        mFocusRegionGuideView = focusRegionGuideView;
        mOnAfAeRoiChangeCallback = onAfAeRoiChange;
        this.mCameraCharacteristics = cameraCharacteristics;
        mSensorBoundRect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        focusRegionGuideView.setCameraBound(mSensorBoundRect);
    }

    public void onFocusPointChanged(PointF pointF) {
        Rect afRect = calcFocusRect(pointF, Constants.AF_REGION_RATIO);
        Rect aeRect = calcFocusRect(pointF, Constants.AE_REGION_RATIO);

        mOnAfAeRoiChangeCallback.OnAfAeRoiChangeCallback(afRect, aeRect);
    }

    public Rect calcFocusRect(PointF point, float regionRatio) {
        // ref to preview range
        Size previewSize = Constants.PREVIEW_SIZE;
        Rect cameraBound = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        Log.d(TAG, "active Rect:" + cameraBound.toString());
        Rect newRect;
        int areaSize = (int) (previewSize.getHeight() / regionRatio);
        int leftPos, topPos;
        float newX = point.y;
        float newY = previewSize.getHeight() - point.x;
        leftPos = (int) ((newX / previewSize.getWidth()) * cameraBound.right);
        topPos = (int) ((newY / previewSize.getHeight()) * cameraBound.bottom);
        int left = clamp(leftPos - areaSize, 0, cameraBound.right);
        int top = clamp(topPos - areaSize, 0, cameraBound.bottom);
        int right = clamp(leftPos + areaSize, leftPos, cameraBound.right);
        int bottom = clamp(topPos + areaSize, topPos, cameraBound.bottom);
        newRect = new Rect(left, top, right, bottom);
        Log.d(TAG, newRect.toString());
        Log.d(TAG, "calcFocusRect: focus region: " + newRect.toString());
        return newRect;
    }

    private void refRoiRectToSensorSize(Rect sensorRectRegion, Size previewSize, Rect outRect) {
        float ratioX = sensorRectRegion.width() * 1.0f / previewSize.getWidth();
        Log.d(TAG, "refRoiRectToSensorSize: " + outRect.toString());
        outRect.left = (int) (outRect.top * ratioX);
        outRect.right = (int) ((previewSize.getHeight() - outRect.left) * ratioX);
        outRect.top = (int) (outRect.bottom * ratioX);
        int temp2 = previewSize.getHeight() - outRect.right;
        outRect.bottom = (int) (temp2 * ratioX);
        Log.d(TAG, "refRoiRectToSensorSize: end: " + outRect.right);
    }

    private void getRoiFromPoint(PointF point, float regionRatio, Rect outRect) {
        float halfRegionX = mPreviewRect.width() / regionRatio / 2;
        float halfRegionY = mPreviewRect.height() / regionRatio / 2;
        outRect.left = (int) (point.x - halfRegionX);
        outRect.right = (int) (point.x + halfRegionX);
        outRect.top = (int) (point.y - halfRegionY);
        outRect.bottom = (int) (point.y + halfRegionY);
    }

    private void fixRectangleRegion(Rect outRect, Rect mPreviewRect) {
        if (outRect.left < mPreviewRect.left) {
            outRect.left = mPreviewRect.left;
        }
        if (outRect.right > mPreviewRect.right) {
            outRect.right = mPreviewRect.right;
        }
        if (outRect.top < mPreviewRect.top) {
            outRect.top = mPreviewRect.top;
        }
        if (outRect.bottom > mPreviewRect.bottom) {
            outRect.bottom = mPreviewRect.bottom;
        }
    }

    public void touchPointNotify(PointF pointF) {
        Log.d(TAG, "touchPointNotify: pointF(" + pointF.x + ", " + pointF.y + ")"
                + " mPreviewRect(" + mPreviewRect.width() + ", " + mPreviewRect.height() + ")");
        if (mPreviewRect == null || pointF.x > mPreviewRect.right || pointF.y > mPreviewRect.bottom) {
            return;
        }
        onFocusPointChanged(pointF);
        mFocusRegionGuideView.touchPointNotify(pointF);
    }

    public void notifyDetectFacesChange(Face[] faces) {
        mFocusRegionGuideView.notifyFaceDetectChange(faces);
    }

    public void updateDisplayOrientation(int orientation) {
        mDisplayOrientation = orientation;
        mCameraRotation = getCameraRotation(orientation);
        mFocusRegionGuideView.updateOrientation(mCameraRotation, mDisplayOrientation);
    }

    private int getCameraRotation(int orientation) {
//        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
//        Camera.getCameraInfo(Integer.valueOf(mMainCameraId), cameraInfo);
        Integer sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Integer sensorLens = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
        if (sensorOrientation == null || sensorLens == null) {
            return -1;
        }
        if (sensorLens == CameraCharacteristics.LENS_FACING_BACK) {
//            result = (info.orientation + degrees) % 360;
//            result = (360 - result) % 360;  // compensate the mirror
            return (sensorOrientation + orientation) % 360;
        } else {
            return (sensorOrientation - orientation + 360) % 360;
        }

    }

    public int getCameraRotation() {
        return mCameraRotation;
    }

    public int getDisplayOrientation() {
        return mDisplayOrientation;
    }

    public void updateSurfaceViewLayoutChange(int width, int height) {
        mFocusRegionGuideView.updateSurfaceViewLayoutChange(width, height);
    }

    public void updateViewDestory() {
        mFocusRegionGuideView.clear();
    }

    public void updateFocusStateChanged(Integer resultAFState) {
        switch (resultAFState) {
            case CameraMetadata.CONTROL_AF_STATE_INACTIVE:
                // af set to off
                break;
            case CameraMetadata.CONTROL_AF_STATE_PASSIVE_SCAN:
            case CameraMetadata.CONTROL_AF_STATE_ACTIVE_SCAN:
                // start
                break;
            case CameraMetadata.CONTROL_AF_STATE_PASSIVE_FOCUSED:
            case CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED:
                // success
                mFocusRegionGuideView.updateAfStateChanged(true);
                break;
            case CameraMetadata.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED:
            case CameraMetadata.CONTROL_AF_STATE_PASSIVE_UNFOCUSED:
                // fail
                mFocusRegionGuideView.updateAfStateChanged(false);
                break;
        }
    }

    public void setPreviewRect(Rect refPreviewRect) {
        mPreviewRect = refPreviewRect;
    }

    interface OnAfAeRoiChange {
        void OnAfAeRoiChangeCallback(Rect afRect, Rect aeRect);
    }

}
