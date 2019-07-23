package com.mozre.mcamera.gl;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class CustomerGLSurfaceView extends GLSurfaceView {
    private CustomerRender customerRender;

    public CustomerGLSurfaceView(Context context) {
        super(context);
    }

    public CustomerGLSurfaceView(Context context, int width, int height) {
        super(context);
        customerRender = new CustomerRender(context, width, height);
        setEGLContextClientVersion(2);
        setRenderer(customerRender);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void updateFrame(byte[] data, int width, int height) {
        customerRender.updateFrame(data, width, height);
    }

    public void previewReady() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                customerRender.previewReady();
            }
        });
    }

    public CustomerRender getRender() {
        return customerRender;
    }
    public void setRenderCallBack(final CustomerRender.CustomerRenderCallback callBack) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                customerRender.setRelightingRenderCallback(callBack);
            }
        });
    }

    public void release() {
        if (customerRender != null) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    customerRender.release();
                }
            });
        }
    }

}
