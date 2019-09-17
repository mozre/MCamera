package com.mozre.mcamera.gl;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.Log;
import android.view.Display;

import com.mozre.mcamera.utils.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CustomerRender implements GLSurfaceView.Renderer {
    private static final String TAG = Constants.getTagName(CustomerRender.class.getSimpleName());
    private Context mContext;
    private ImageMatrix mImageMatrix;
    private CameraMatrix mCameraMatrix;
    private int mInTextureId = -1;
    private int mOutTextureId = -1;
    private SurfaceTexture mSurfaceTexture;
    private int mOrientation;
    private CustomerRenderCallback mCustomerCallback;
    private ByteBuffer mImageData;
    private int mWidth;
    private int mHeight;
    private boolean isReady = false;
    private int mFboId = -1;
    private Rect mViewportRect;

    public CustomerRender(Context context, int width, int height) {
        this.mContext = context;
        this.mWidth = height;
        this.mHeight = width;
        Activity activity = (Activity) context;
        Display display = activity.getWindowManager().getDefaultDisplay();
        mViewportRect = new Rect();
        display.getRectSize(mViewportRect);
        float scale = mViewportRect.width() / (float) this.mWidth;
        int margin = (int) (mViewportRect.height() - scale * mHeight);
        Log.d(TAG, "CustomerRender screen display: " + mViewportRect.toString() + " mWidth: " + mWidth + " mHeight: " + mHeight + " scale: " + scale);
        mViewportRect.left = 0;
        mViewportRect.top = margin;
        mViewportRect.right = mViewportRect.width();
        mViewportRect.bottom = mViewportRect.height();
        Log.d(TAG, "CustomerRender screen width: " + mViewportRect.width() + " height: " + mViewportRect.height() + " width: " + width + " Height: " + height);
//        Log.d(TAG, "CustomerRender screen mViewportRect: " + mViewportRect.toString());
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated: ");
        mImageMatrix = new ImageMatrix(mContext);
        mCameraMatrix = new CameraMatrix(mContext);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged: ");
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        if (mOutTextureId != -1) {
            OpenglUtil.deleteTextureID(mOutTextureId);
        }
        mOutTextureId = OpenglUtil.initTextureID();
        if (mInTextureId != -1) {
            OpenglUtil.deleteTextureID(mInTextureId);
        }
        mInTextureId = OpenglUtil.initCameraTextureID();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mInTextureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mViewportRect.width(), mViewportRect.height(), 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        mSurfaceTexture = new SurfaceTexture(mInTextureId);
//        mFboId = initFBO();
        mCustomerCallback.textureCreated(mSurfaceTexture);
    }

    private int initFBO() {
        int[] fob = new int[1];
        GLES20.glGenFramebuffers(1, fob, 0);
        return fob[0];
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!isReady || mSurfaceTexture == null || mCameraMatrix == null || mImageMatrix == null) {
            Log.i(TAG, "onDrawFrame: skip  isReady: " + isReady + " mSurfaceTexture: " + mSurfaceTexture + " mCameraMatrix: " + mCameraMatrix + " mImageMatrix: " + mImageMatrix);
            return;
        }
/*        if (null == mImageData) {
            mCameraMatrix.onDrawFrame(mInTextureId);
        }
*/
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mSurfaceTexture.updateTexImage();
        float[] mtx = new float[16];
        Log.d(TAG, "onDrawFrame: " + mViewportRect.toString());
        GLES20.glViewport(mViewportRect.left, mViewportRect.top, mViewportRect.right, mViewportRect.bottom);
        mSurfaceTexture.getTransformMatrix(mtx);
        if (/*res*/false) {
            mImageMatrix.setTextureTransformMatrix(mtx);
            mImageMatrix.onDrawFrame(mOutTextureId);
        } else {
//            dumpCameraImage(mInTextureId);
            mCameraMatrix.setTextureTransformMatrix(mtx);
            mCameraMatrix.onDrawFrame(mInTextureId);
        }
    }

    public void updateOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    public void updateFrame(byte[] data, int width, int height) {
        if (mImageData == null) {
            mImageData = ByteBuffer.allocateDirect(data.length);
        }
        Log.i(TAG, "updateFrame: " + data.length);
        mImageData.clear();
        this.mImageData.put(data);
        this.mWidth = width;
        this.mHeight = height;
//        this.isReady = true;
    }

    public void release() {
        if (mInTextureId != -1) {
            OpenglUtil.deleteTextureID(mInTextureId);
            mInTextureId = -1;
        }

        if (mOutTextureId != -1) {
            OpenglUtil.deleteTextureID(mOutTextureId);
            mOutTextureId = -1;
        }

        if (null != mSurfaceTexture) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
    }

    private void dumpCameraImage(int textureId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureId, 0);
        GLES20.glFinish();
        ByteBuffer buffer = ByteBuffer.allocateDirect(mWidth * mHeight * 4);
        GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        File file = new File(Environment.getExternalStorageDirectory(),
                "dump" + File.separator + System.currentTimeMillis() + ".jpg");
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setRelightingRenderCallback(CustomerRenderCallback customerRenderCallback) {
        this.mCustomerCallback = customerRenderCallback;
    }

    public void previewReady() {
        this.isReady = true;
    }

    public Rect getRefPreviewRect() {
        return mViewportRect;
    }

    public interface CustomerRenderCallback {
        void textureCreated(SurfaceTexture surfaceTexture);
    }
}
