package com.mozre.mcamera.gl;

import android.content.Context;
import android.opengl.GLES20;


import com.mozre.mcamera.utils.Constants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by gaoyunfeng on 08/12/2017.
 */

public abstract class GLMatrix {
    private static final String TAG =  Constants.getTagName(GLMatrix.class.getSimpleName());
    private final String mVertexShader;
    private final String mFragmentShader;
    private final float vertexPoint[] = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, 1.0f,
            1.0f, -1.0f,
    };
    protected float[] mTextureTransformMatrix;
    protected int mTextureTransformMatrixLocation;
    protected int mGLProgId;
    protected int mGLAttribPosition;
    protected int mGLUniformTexture;
    protected int mGLAttribTextureCoordinate;
    protected boolean mIsInitialized;
    protected FloatBuffer mGLCubeBuffer;
    protected FloatBuffer mGLTextureBuffer;
    private float texturePoint[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            /*0.0f,0.0f,
             0.0f,1.0f,
             1.0f,0.0f,
             1.0f,1.0f*/
    };


    protected GLMatrix(Context context, int vertex, int fragment) {
        mVertexShader = OpenglUtil.loadFromRawFile(context, vertex);
        mFragmentShader = OpenglUtil.loadFromRawFile(context, fragment);

        mGLCubeBuffer = ByteBuffer.allocateDirect(vertexPoint.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(vertexPoint).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(texturePoint.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(texturePoint).position(0);
        init();
    }

    protected GLMatrix(Context context, int vertex, int fragment, float texturePoints[]) {
        for (int i = 0; i < this.texturePoint.length && i < texturePoints.length; i++) {
            this.texturePoint[i] = texturePoints[i];
        }
        mVertexShader = OpenglUtil.loadFromRawFile(context, vertex);
        mFragmentShader = OpenglUtil.loadFromRawFile(context, fragment);

        mGLCubeBuffer = ByteBuffer.allocateDirect(vertexPoint.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(vertexPoint).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(texturePoint.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(texturePoint).position(0);
        init();
    }

    protected void init() {
        mGLProgId = OpenglUtil.loadProgram(mVertexShader, mFragmentShader);

        mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgId, "position");
        mGLUniformTexture = GLES20.glGetUniformLocation(mGLProgId, "inputImageTexture");
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgId,
                "inputTextureCoordinate");
        mTextureTransformMatrixLocation = GLES20.glGetUniformLocation(mGLProgId, "textureTransform");
        mIsInitialized = true;
    }

    public void setTextureTransformMatrix(float[] mtx) {
        mTextureTransformMatrix = mtx;
    }

    public void destroy() {
        mIsInitialized = false;
    }

    public int onDrawFrame(int textureId) {

        GLES20.glUseProgram(mGLProgId);
        if (!mIsInitialized) {
            return OpenglUtil.NOT_INIT;
        }
//        Log.e(TAG, "onDrawFrame: ");
        mGLCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);

        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glUniformMatrix4fv(mTextureTransformMatrixLocation, 1, false, mTextureTransformMatrix, 0);

        if (textureId != OpenglUtil.NO_TEXTURE) {
            bindTexture(textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        } else {
            GLES20.glUniform1i(mGLUniformTexture, 0);
            bindTexture(0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return OpenglUtil.ON_DRAWN;
    }


    protected abstract void bindTexture(int textureId);
}
