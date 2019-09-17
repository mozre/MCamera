package com.mozre.mcamera.utils;

import android.app.Activity;
import android.hardware.camera2.CameraCharacteristics;
import android.view.OrientationEventListener;
import android.view.Surface;

public class CameraUtil {
    // Orientation hysteresis amount used in rounding, in degrees
    private static final int ORIENTATION_HYSTERESIS = 5;

    public static int getDisplayOrientation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    public static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation = false;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min( dist, 360 - dist );
            changeOrientation = ( dist >= 45 + ORIENTATION_HYSTERESIS );
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }


    public static int getJpegRotation(int orientation, CameraCharacteristics info) {
        // See android.hardware.Camera.Parameters.setRotation for
        // documentation.
        int rotation = 0;
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            orientation = 0;
        }
        if (info.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT) {
            rotation = (info.get(CameraCharacteristics.SENSOR_ORIENTATION)
                    - orientation + 360) % 360;
        } else {  // back-facing camera
            rotation = (info.get(CameraCharacteristics.SENSOR_ORIENTATION) + orientation) % 360;
        }
        return rotation;
    }

}
