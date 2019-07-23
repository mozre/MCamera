package com.mozre.mcamera.gl;

import android.content.Context;
import android.opengl.GLES20;

import com.mozre.mcamera.R;


/**
 * 绘制原始camera数据
 */
public class ImageMatrix extends GLMatrix {
    public ImageMatrix(Context context) {
        super(context, R.raw.vertex /*R.raw.vertex*/, R.raw.image_fragment/*R.raw.image_fragment*/);
    }

    @Override
    protected void bindTexture(int textureId) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
    }
}
