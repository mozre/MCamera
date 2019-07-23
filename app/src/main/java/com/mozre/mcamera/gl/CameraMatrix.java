package com.mozre.mcamera.gl;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.mozre.mcamera.R;


/**
 * Created by gaoyunfeng on 08/12/2017.
 */

public class CameraMatrix extends GLMatrix {
    public CameraMatrix(Context context) {
        super(context, R.raw.vertex/*R.raw.vertex*/, R.raw.camera_fragment/*R.raw.camera_fragment*/);
    }

    @Override
    protected void bindTexture(int textureId) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
    }
}
