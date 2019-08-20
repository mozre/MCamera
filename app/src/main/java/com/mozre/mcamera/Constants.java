package com.mozre.mcamera;


import android.Manifest;
import android.util.Size;

public class Constants {
    public static String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };
    public static final int PERMISSION_REQUEST_CODE = 3;
    public static final String CAMERA_THREAD_NAME = "camera_thread_m";
    public static final Size PREVIEW_SIZE = new Size(1440, 1080);
    public static final Size CAPTURE_SIZE = new Size(4160, 3120);
    private static final String APP_TAG_NAME = "mozre_1/";
    public static final float AF_REGION_RATIO = 8.0f;
    public static final float AE_REGION_RATIO = AF_REGION_RATIO * 1.5f;
    public static final int ACTION_TOUCH_DOWN_RESET_DELAY = 300;
    private static final int FOCUS_SQUARE_LENGTH = 200;
    public static final int CANCEL_TOUCH_FOCUS_DELAY = 3000;

    public static String getTagName(String name) {
        return APP_TAG_NAME + name;
    }

}
