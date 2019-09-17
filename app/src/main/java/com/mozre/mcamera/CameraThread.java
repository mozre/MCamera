package com.mozre.mcamera;

import android.os.Process;
import android.util.Log;

import com.mozre.mcamera.utils.Constants;

import java.util.concurrent.LinkedBlockingQueue;

public class CameraThread extends Thread {
    private static final String TAG = Constants.getTagName(CameraThread.class.getSimpleName());
    private volatile boolean mIsActive = true;
    private LinkedBlockingQueue<Runnable> mWorkQueue;

    public CameraThread(String name) {
        super(name);
        mWorkQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
        while (mIsActive) {
            if (mWorkQueue.isEmpty()) {
                if(mIsActive) {
                    waitWithoutInterrupt(this);
                }
            } else {
                mWorkQueue.poll().run();
            }
        }
    }

    public synchronized void post(Runnable runnable) {
        if (!mWorkQueue.offer(runnable)) {
            Log.e(TAG, "post: add job fail!");
            return;
        }
        this.notifyAll();
    }

    public synchronized void terminate() {
        mIsActive = false;
        if (!mWorkQueue.isEmpty()) {
            mWorkQueue.clear();
        }
        this.notifyAll();
    }

    private synchronized void waitWithoutInterrupt(Object object) {
        try {
            object.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
