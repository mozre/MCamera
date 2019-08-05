package com.mozre.mcamera;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.Face;
import android.util.Log;
import android.util.Size;

import com.mozre.mcamera.element.FocusRegionGuideView;

import java.util.LinkedList;
import java.util.List;

public class FocusOverlayManager {
    private static final String TAG = Constants.getTagName(FocusOverlayManager.class.getSimpleName());
    private CameraCharacteristics mCameraCharacteristics;
    private Rect mScreenRect;
    private Rect mPreviewRect;
    private FocusRegionGuideView mFocusRegionGuideView;
    private int mDisplayOrientation;
    private int mCameraRotation;

    public FocusOverlayManager(FocusRegionGuideView focusRegionGuideView, CameraCharacteristics cameraCharacteristics) {
        mFocusRegionGuideView = focusRegionGuideView;
        this.mCameraCharacteristics = cameraCharacteristics;
        focusRegionGuideView.setCameraBound(mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE));
    }

    public void onFocusPointChanged(PointF pointF) {
        RectF afRect = calcFocusRect(pointF, Constants.AF_REGION_RATIO);
        RectF aeRect = calcFocusRect(pointF, Constants.AE_REGION_RATIO);

    }

    public RectF calcFocusRect(PointF point, float regionRatio) {
        Rect sensorRectRegion = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        // ref to preview range
        RectF outRectF = new RectF();
        getRoiFromPoint(point, regionRatio, outRectF);
        fixRectangleRegion(outRectF, mPreviewRect);
        refRoiRectToSensorSize(sensorRectRegion, mPreviewRect, outRectF);
        return outRectF;
    }

    private void refRoiRectToSensorSize(Rect sensorRectRegion, Rect mPreviewRect, RectF outRectF) {
        float ratioX = sensorRectRegion.width() / mPreviewRect.width();
        float ratioY = sensorRectRegion.height() / mPreviewRect.height();
        outRectF.left *= ratioX;
        outRectF.right *= ratioX;
        outRectF.top *= ratioY;
        outRectF.bottom *= ratioY;
    }

    private void getRoiFromPoint(PointF point, float regionRatio, RectF outRectF) {
        float halfRegionX = mPreviewRect.width() / regionRatio / 2;
        float halfRegionY = mPreviewRect.height() / regionRatio / 2;
        outRectF.left = point.x - halfRegionX;
        outRectF.right = point.x + halfRegionX;
        outRectF.top = point.y - halfRegionY;
        outRectF.bottom = point.y + halfRegionY;
    }

    private void fixRectangleRegion(RectF outRectF, Rect mPreviewRect) {
        if (outRectF.left < mPreviewRect.left) {
            outRectF.left = mPreviewRect.left;
        }
        if (outRectF.right > mPreviewRect.right) {
            outRectF.right = mPreviewRect.right;
        }
        if (outRectF.top < mPreviewRect.top) {
            outRectF.top = mPreviewRect.top;
        }
        if (outRectF.bottom > mPreviewRect.bottom) {
            outRectF.bottom = mPreviewRect.bottom;
        }
    }

    public void touchPointNotify(PointF pointF) {
        mFocusRegionGuideView.touchPointNotify(pointF);
    }

    public void notifyDetectFacesChange(Face[] faces) {
        // 4608, 3456
        Rect sensorRectRegion = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        Log.d(TAG, "notifyDetectFacesChange: " + " sensorRectRegion: " + sensorRectRegion.toString());
        if (faces == null || faces.length == 0 || sensorRectRegion == null) {
            return;
        }
        List<RectF> faceRects = new LinkedList<>();
        Rect rect;
        for (int i = 0; i < faces.length; ++i) {
            RectF tmpRect = new RectF();
            rect = faces[i].getBounds();
            Log.d(TAG, "notifyDetectFacesChange rect:" + rect.toString());
            tmpRect.left = Constants.PREVIEW_SIZE.getHeight() - rect.top * (Constants.PREVIEW_SIZE.getHeight() * 1.0f / sensorRectRegion.height());
            tmpRect.top = Constants.PREVIEW_SIZE.getWidth() - rect.left * (Constants.PREVIEW_SIZE.getWidth() * 1.0f / sensorRectRegion.width());
            tmpRect.right = Constants.PREVIEW_SIZE.getHeight() - rect.bottom * (Constants.PREVIEW_SIZE.getHeight() * 1.0f / sensorRectRegion.height()) + tmpRect.top;
            tmpRect.bottom = Constants.PREVIEW_SIZE.getWidth() -rect.right * (Constants.PREVIEW_SIZE.getWidth() * 1.0f / sensorRectRegion.width()) + tmpRect.left;
            Log.d(TAG, "notifyDetectFacesChange tmpRect: " + tmpRect.toString());
            faceRects.add(tmpRect);
        }
//        Matrix matrix = new Matrix();
//        matrix.setRectToRect()
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
}
