package com.mozre.mcamera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;
import android.widget.RelativeLayout;

import com.mozre.mcamera.gl.CustomerGLSurfaceView;
import com.mozre.mcamera.gl.CustomerRender;

import java.util.Arrays;
import java.util.List;

public class CameraManager implements CustomerRender.CustomerRenderCallback {
    private static final String TAG = Constants.getTagName(CameraManager.class.getSimpleName());
    private CameraThread mCameraThread;
    private CameraDevice mCameraDevice;
    private static CameraManager sCameraManager;
    private android.hardware.camera2.CameraManager mCameraService;
    private CameraCharacteristics mCameraCharacteristics;
    private CameraCaptureSession mCameraCaptureSession;
    private Surface mGLPreviewSurface;
    private Surface mFrameStreamingSurface;
    private CaptureRequest mPreviewCaptureRequest;
    private Handler mMainHandler;
    private String mMainCameraId = "-1";
    private Context mContext;
    private SurfaceTexture mSurfaceTexture;
    private boolean mIsSurfaceTextureReady = false;
    private boolean mIsCameraReady = false;
    private boolean mIsStartPreview = false;
    private CustomerGLSurfaceView mCustomerSurfaceView;
    private RelativeLayout mContainer;

    private CameraDevice.StateCallback mCameraOpenStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            Log.d(TAG, "onOpened: " + camera.getId());
            if (mIsSurfaceTextureReady && !mIsStartPreview) {
                mIsStartPreview = true;
                createPreviewSession(mSurfaceTexture, mMainHandler);
            }
            mIsCameraReady = true;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            Log.w(TAG, "camera Disconnected!");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            Log.e(TAG, "onError: " + error);
        }
    };

    private CameraCaptureSession.StateCallback mCameraSessionCreateCallBack = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.i(TAG, "CameraCaptureSession.StateCallback onConfigured: ");
            mCameraCaptureSession = session;
            sendPreviewRequest(mMainHandler);
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            session.close();
            Log.e(TAG, "onConfigureFailed: ");
        }
    };

    private ImageReader.OnImageAvailableListener mImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.i(TAG, "onImageAvailable: +++++");
            Image image = reader.acquireNextImage();
            image.close();
        }
    };

    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            Log.i(TAG, "onCaptureStarted: ");
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            mPreviewCaptureRequest = request;
            Log.i(TAG, "onCaptureCompleted: ");
            if (mCustomerSurfaceView != null) {
                mCustomerSurfaceView.previewReady();
                mCustomerSurfaceView.requestRender();
            }
        }
    };

    private CameraCaptureSession.CaptureCallback mAeAfModeCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            Log.i(TAG, "onCaptureProgressed: ");
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Log.i(TAG, "onCaptureCompleted: ");
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.i(TAG, "onCaptureFailed: ");
        }
    };

    private CameraManager() {
    }

    public static CameraManager getInstance() {
        if (sCameraManager == null) {
            sCameraManager = new CameraManager();
        }
        return sCameraManager;
    }

    public void init(Context context, RelativeLayout mRelativeContainer) {
        Log.d(TAG, "init: ");
        this.mContext = context;
        this.mContainer = mRelativeContainer;
        mCameraThread = new CameraThread(Constants.CAMERA_THREAD_NAME);
        mCameraThread.start();
        mCameraService = (android.hardware.camera2.CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (mCameraService == null) {
            Log.e(TAG, "init get Camera Service Failed!");
            return;
        }
        String[] id = null;
        try {
            id = mCameraService.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (id != null && id.length > 0) {
            mMainCameraId = id[0];
            Log.e(TAG, "init: get Camera id list  " + id.length);
        } else {
            Log.e(TAG, "init: get Camera id list is empty " + id.length);
        }

    }


    @SuppressLint("MissingPermission")
    public void openCamera(@NonNull Context context, @NonNull final Handler handler, @NonNull final String cameraId) {
        if (mCameraService == null) {
            mCameraService = (android.hardware.camera2.CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        }
        mCameraThread.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "openCamera: start");
                    mCameraService.openCamera(cameraId, mCameraOpenStateCallback, handler);
                } catch (CameraAccessException e) {
                    Log.e(TAG, "run: open camera fail!");
                    e.printStackTrace();
                }
            }
        });
    }

    public void releaseCamera() {
        mCameraThread.post(new Runnable() {
            @Override
            public void run() {
                if (mCameraDevice != null) {
                    Log.d(TAG, "run: release camera");
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
            }
        });
    }

    public CameraCharacteristics getCameraCharacteristics(@NonNull String cameraId) {
        try {
            mCameraCharacteristics = mCameraService.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            Log.e(TAG, "getCameraCharacteristics: get camera characteristics faill! camera id is " + cameraId);
            e.printStackTrace();
        }
        return mCameraCharacteristics;
    }

    public void createPreviewSession(SurfaceTexture surfaceTexture, Handler handler) {
        Log.d(TAG, "createPreviewSession: ");
        try {
            mCameraDevice.createCaptureSession(getOutputSurfaceList(surfaceTexture, handler), mCameraSessionCreateCallBack, handler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "createPreviewSession: create capture session fail!");
            e.printStackTrace();
        }
    }

    private List<Surface> getOutputSurfaceList(SurfaceTexture surfaceTexture, Handler handler) {
        surfaceTexture.setDefaultBufferSize(Constants.PREVIEW_SIZE.getWidth(), Constants.PREVIEW_SIZE.getHeight());
        mGLPreviewSurface = new Surface(surfaceTexture);
        ImageReader imageReader = ImageReader.newInstance(Constants.PREVIEW_SIZE.getWidth(),
                Constants.PREVIEW_SIZE.getHeight(), ImageFormat.YUV_420_888, 1);
        imageReader.setOnImageAvailableListener(mImageAvailableListener, handler);
        mFrameStreamingSurface = imageReader.getSurface();
        return Arrays.asList(mFrameStreamingSurface, mGLPreviewSurface);
    }

    private int checkModeIsSupport(int mode, CameraCharacteristics.Key<int[]> key) {
        if (mCameraCharacteristics == null) {
            getCameraCharacteristics(mMainCameraId);
        }
        int[] supportModeList = mCameraCharacteristics.get(key);
        if (supportModeList == null) {
            return -1;
        }
        for (int sMode : supportModeList) {
            if (sMode == mode) {
                return mode;
            }
        }
        return supportModeList[0];
    }

    private void sendPreviewRequest(Handler handler) {
        Log.d(TAG, "sendPreviewRequest: ");
        int afMode = checkModeIsSupport(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE, CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        int aeAntibandingMode = checkModeIsSupport(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO, CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES);
        try {
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(mGLPreviewSurface);
            builder.addTarget(mFrameStreamingSurface);
            if (afMode != -1) {
                builder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
            }
            if (aeAntibandingMode != -1) {
                builder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, aeAntibandingMode);
            }
            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            mCameraCaptureSession.setRepeatingRequest(builder.build(), mPreviewCaptureCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void sendCaptureRequest(int orientation, Handler handler) {
        int rotation = getCameraRotation(orientation);
        try {
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.set(CaptureRequest.JPEG_ORIENTATION, rotation);
            builder.set(CaptureRequest.CONTROL_AF_MODE, mPreviewCaptureRequest.get(CaptureRequest.CONTROL_AF_MODE));
            builder.set(CaptureRequest.CONTROL_AE_MODE, mPreviewCaptureRequest.get(CaptureRequest.CONTROL_AE_MODE));
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, mPreviewCaptureRequest.get(CaptureRequest.LENS_FOCUS_DISTANCE));
            mCameraCaptureSession.capture(builder.build(), null, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private int getCameraRotation(int orientation) {
        Integer sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Integer sensorLens = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
        if (sensorOrientation == null || sensorLens == null) {
            return -1;
        }
        if (sensorLens == CameraCharacteristics.LENS_FACING_BACK) {
            return (sensorOrientation + orientation) % 360;
        } else {
            return (sensorOrientation - orientation + 360) % 360;
        }
    }

    private boolean isAeAfRegionSupport(CameraCharacteristics.Key<Integer> key) {
        Integer regionNum = mCameraCharacteristics.get(key);
        return (regionNum != null && regionNum > 0);
    }

    public void sendAfAeRequest(MeteringRectangle focusArea, MeteringRectangle aeArea, Handler handler) {
        try {
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            if (isAeAfRegionSupport(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)) {
                builder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{focusArea});
            }
            if (isAeAfRegionSupport(CameraCharacteristics.CONTROL_MAX_REGIONS_AE)) {
                builder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{aeArea});
            }
            mCameraCaptureSession.setRepeatingRequest(builder.build(), mAeAfModeCaptureCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void notifyTextureIsAvailable(SurfaceTexture surface) {
        this.mSurfaceTexture = surface;
        if (mIsCameraReady && !mIsStartPreview) {
            this.createPreviewSession(surface, mMainHandler);
        }
    }

    public void notifyTextureIsDistoryed() {

    }

    public void setMainHandler(Handler mMainHandler) {
        this.mMainHandler = mMainHandler;
    }

    public void startPreview() {
        Log.d(TAG, "startPreview: ++++++");
        if (mCustomerSurfaceView == null) {
            mCustomerSurfaceView = new CustomerGLSurfaceView(mContext,Constants.PREVIEW_SIZE.getWidth(), Constants.PREVIEW_SIZE.getHeight());
            mCustomerSurfaceView.setRenderCallBack(this);
        }
        mContainer.addView(mCustomerSurfaceView, 0);
        this.openCamera(mContext, mMainHandler, mMainCameraId);
    }

    public void stopPreview() {
        Log.d(TAG, "stopPreview: ");
        try {
            if (mCameraCaptureSession != null) {
                mCameraCaptureSession.stopRepeating();
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        this.releaseCamera();
        mSurfaceTexture = null;
        this.mIsStartPreview = false;
        this.mIsSurfaceTextureReady = false;
        this.mIsCameraReady = false;
        if (mCustomerSurfaceView!= null) {
            mCustomerSurfaceView.release();
            mContainer.removeView(mCustomerSurfaceView);
            mCustomerSurfaceView = null;
        }
    }

    public void uninit() {
        if (mCameraThread.isAlive()) {
            mCameraThread.terminate();
            try {
                mCameraThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void textureCreated(final SurfaceTexture surfaceTexture) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "textureCreated: ");
                mSurfaceTexture = surfaceTexture;
                if (mIsCameraReady && !mIsStartPreview) {
                    createPreviewSession(surfaceTexture, mMainHandler);
                }
                mIsSurfaceTextureReady = true;
            }
        });

    }
}
