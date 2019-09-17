package com.mozre.mcamera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.TextureView;
import android.widget.RelativeLayout;

import com.mozre.mcamera.utils.Constants;


public class CameraActivity extends AppCompatActivity{
    private static final String TAG = Constants.getTagName(CameraActivity.class.getSimpleName());
    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    private CameraManager mCameraManager;
    private FocusOverlayManager mFocusOverlayManager;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private RelativeLayout mRelativeContainer;
    private OrientationEventListener mOrientationEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindowManager().getDefaultDisplay().getRotation();
        mRelativeContainer = (RelativeLayout)findViewById(R.id.root_container);
        mCameraManager = CameraManager.getInstance();
        mCameraManager.setMainHandler(mMainHandler);
        mOrientationEventListener = new CustomOrientationEventListener(this);

/*        CircleImageView circleImageView = (CircleImageView) findViewById(R.id.circle_image_view);
        circleImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.light));*/
//        WaManagerWrapper waManagerWrapper = WaManagerWrapper.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCameraManager.init(this, mRelativeContainer);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermissionManager.checkAndRequestPermission(this, Constants.PERMISSIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraManager.startPreview();
        mOrientationEventListener.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraManager.stopPreview();
        mOrientationEventListener.disable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraManager.stopPreview();
        mCameraManager.uninit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.PERMISSION_REQUEST_CODE) {
            Log.d(TAG, "onRequestPermissionsResult: ");
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    //TODO
//                    System.exit(0);
                    Log.e(TAG, "onRequestPermissionsResult: error");
                }
            }
        }
    }

    private class CustomOrientationEventListener extends OrientationEventListener {
        public CustomOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            mCameraManager.onOrientationChanged(orientation);
        }
    }

}
