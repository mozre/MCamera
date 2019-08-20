package com.mozre.mcamera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.RelativeLayout;

import com.mozre.mcamera.element.FocusRegionGuideView;
import com.mozre.mcamera.gl.CustomerGLSurfaceView;
import com.mozre.mcamera.gl.CustomerRender;

import java.util.Arrays;
import java.util.List;

public class CameraManager implements CustomerRender.CustomerRenderCallback, View.OnTouchListener, FocusOverlayManager.OnAfAeRoiChange {
    private static final String TAG = Constants.getTagName(CameraManager.class.getSimpleName());
    private CameraThread mCameraThread;
    private CameraDevice mCameraDevice;
    private static CameraManager sCameraManager;
    private FocusOverlayManager mFocusOverlayManager;
    private android.hardware.camera2.CameraManager mCameraService;
    private CameraCharacteristics mCameraCharacteristics;
    private CameraCaptureSession mCameraCaptureSession;
    private Surface mGLPreviewSurface;
    private ImageReader mFrameStreamingImageReader;
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
    private boolean mIsTapDown = false;
    private DisplayManager.DisplayListener mDisplayListener;
    private int mLastAfState = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
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
            String id = session.getDevice().getId();
            sendPreviewRequest(mMainHandler, id);
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
//            Log.i(TAG, "onCaptureStarted: ");
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            updateAfState(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            mPreviewCaptureRequest = request;
            Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
            String id = (String) result.getRequest().getTag();
            mFocusOverlayManager.notifyDetectFacesChange(faces);
            Log.i(TAG, "onCaptureCompleted id: " + id + " face count: " + faces.length);
            if (mMainCameraId.equals(id) && mCustomerSurfaceView != null) {
                mCustomerSurfaceView.previewReady();
                mCustomerSurfaceView.requestRender();
            }
            updateAfState(result);
        }
    };

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            updateAfState(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            updateAfState(result);
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

    public void init(final Activity context, RelativeLayout mRelativeContainer) {
        Log.d(TAG, "init: ");
        mContext = context;
        mContainer = (RelativeLayout) mRelativeContainer.findViewById(R.id.main_container);
        mContainer.setOnTouchListener(this);
        FocusRegionGuideView focusRegionGuideView = mRelativeContainer.findViewById(R.id.main_focus_guide);
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
            mFocusOverlayManager = new FocusOverlayManager(focusRegionGuideView, getCameraCharacteristics(mMainCameraId), this);
            Log.e(TAG, "init: get Camera id list  " + id.length);
        } else {
            Log.e(TAG, "init: get Camera id list is empty " + id.length);
        }

        mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {

            }

            @Override
            public void onDisplayRemoved(int displayId) {

            }

            @Override
            public void onDisplayChanged(int displayId) {
                if (mDisplayListener == null) return;
                mFocusOverlayManager.updateDisplayOrientation(CameraUtil.getDisplayOrientation(context));
            }
        };
        mFocusOverlayManager.updateDisplayOrientation(CameraUtil.getDisplayOrientation(context));
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
        mFrameStreamingImageReader = imageReader;
        return Arrays.asList(mGLPreviewSurface, mFrameStreamingImageReader.getSurface());
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

    private void sendPreviewRequest(Handler handler, String id) {

        int afMode = checkModeIsSupport(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE, CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        int aeAntibandingMode = checkModeIsSupport(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO, CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES);

        try {
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(mGLPreviewSurface);
            builder.addTarget(mFrameStreamingImageReader.getSurface());
            if (afMode != -1) {
                builder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
            }
//            if (aeAntibandingMode != -1) {
//                builder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, aeAntibandingMode);
//            }
            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
//            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            // face detection
            builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE);
//            builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL);
            // white balance
//            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            // Expose
//            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH);
            builder.setTag(id);
            mCameraCaptureSession.setRepeatingRequest(builder.build(), mPreviewCaptureCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void sendCaptureRequest(int orientation, Handler handler) {
        int rotation = mFocusOverlayManager.getCameraRotation();
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

    private boolean isAeAfRegionSupport(CameraCharacteristics.Key<Integer> key) {
        Integer regionNum = mCameraCharacteristics.get(key);
        return (regionNum != null && regionNum > 0);
    }

    private void sendAfAeRequest(MeteringRectangle focusArea, MeteringRectangle aeArea, int afMode, Handler handler) {
        if (mCameraDevice == null) {
            Log.e(TAG, "sendAfAeRequest: mCameraDevice is null!");
            return;
        }
        try {
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{focusArea});
            if (isAeAfRegionSupport(CameraCharacteristics.CONTROL_MAX_REGIONS_AE)) {
                captureBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{aeArea});
            }
            captureBuilder.addTarget(mFrameStreamingImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
            captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
//            captureBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            mCameraCaptureSession.capture(captureBuilder.build(), mCaptureCallback, handler);
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
            builder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{focusArea});
            if (isAeAfRegionSupport(CameraCharacteristics.CONTROL_MAX_REGIONS_AE)) {
                builder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{aeArea});
            }
            builder.addTarget(mGLPreviewSurface);
            builder.addTarget(mFrameStreamingImageReader.getSurface());
            // face detection
            builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE);
            builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL);

//            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH);
            builder.setTag(mMainCameraId);
            mCameraCaptureSession.setRepeatingRequest(builder.build(), mPreviewCaptureCallback, handler);
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
        if (mCustomerSurfaceView == null) {
            mCustomerSurfaceView = new CustomerGLSurfaceView(mContext, Constants.PREVIEW_SIZE.getWidth(), Constants.PREVIEW_SIZE.getHeight());
            mCustomerSurfaceView.setRenderCallBack(this);
        }
        mFocusOverlayManager.setPreviewRect(mCustomerSurfaceView.getRender().getRefPreviewRect());
        mContainer.addView(mCustomerSurfaceView, 0);
        this.openCamera(mContext, mMainHandler, mMainCameraId);
        mCustomerSurfaceView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (mCustomerSurfaceView == null) {
                    return;
                }
                mFocusOverlayManager.updateSurfaceViewLayoutChange(right - left, bottom - top);
            }
        });
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
        mFocusOverlayManager.updateViewDestory();
        this.releaseCamera();
        mSurfaceTexture = null;
        this.mIsStartPreview = false;
        this.mIsSurfaceTextureReady = false;
        this.mIsCameraReady = false;
        if (mCustomerSurfaceView != null) {
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
        mDisplayListener = null;
        sCameraManager = null;
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsTapDown = true;
                Log.d(TAG, "onTouch: DOWN");
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mIsTapDown = false;
                    }
                }, Constants.ACTION_TOUCH_DOWN_RESET_DELAY);
                return true;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "onTouch: UP");
                if (!mIsTapDown) {
                    break;
                }
                mIsTapDown = false;
                if (event.getY() < Constants.PREVIEW_SIZE.getWidth()) {
                    mFocusOverlayManager.touchPointNotify(new PointF(event.getX(), event.getY()));
                }
                break;
        }
        return false;
    }

    private void updateAfState(CaptureResult result) {
        final Integer resultAFState = result.get(CaptureResult.CONTROL_AF_STATE);
        if (resultAFState != null && mLastAfState != resultAFState) {
            Log.d(TAG, "onCaptureCompleted: " + resultAFState + " mLastAfState: " + mLastAfState);
            mFocusOverlayManager.updateFocusStateChanged(resultAFState);
            mLastAfState = resultAFState;
        }
    }
    @Override
    public void OnAfAeRoiChangeCallback(Rect afRect, Rect aeRect) {

        final MeteringRectangle afMeteringRectangle = new MeteringRectangle(afRect, 1000);
        final MeteringRectangle aeMeteringRectangle = new MeteringRectangle(aeRect, 1);
        sendAfAeRequest(afMeteringRectangle, aeMeteringRectangle, CaptureRequest.CONTROL_AF_MODE_AUTO, mMainHandler);
        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendAfAeRequest(afMeteringRectangle, aeMeteringRectangle, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE, mMainHandler);
            }
        }, Constants.CANCEL_TOUCH_FOCUS_DELAY);
    }
}
