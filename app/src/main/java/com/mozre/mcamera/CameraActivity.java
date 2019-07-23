package com.mozre.mcamera;

import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.widget.RelativeLayout;


public class CameraActivity extends AppCompatActivity {
    private static final String TAG = Constants.getTagName(CameraActivity.class.getSimpleName());
    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    private CameraManager mCameraManager;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private RelativeLayout mRelativeContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindowManager().getDefaultDisplay().getRotation();
        mRelativeContainer = (RelativeLayout)findViewById(R.id.main_container);
        Log.d(TAG, "onCreate: -----------");
        mCameraManager = CameraManager.getInstance();
        mCameraManager.setMainHandler(mMainHandler);
/*        CircleImageView circleImageView = (CircleImageView) findViewById(R.id.circle_image_view);
        circleImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.light));*/
//        WaManagerWrapper waManagerWrapper = WaManagerWrapper.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ----------------");
        mCameraManager.init(this, mRelativeContainer);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermissionManager.checkAndRequestPermission(this, Constants.PERMISSIONS);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: -----------");
        super.onResume();
        mCameraManager.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraManager.stopPreview();
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
}
