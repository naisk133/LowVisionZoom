package cn.pinchzoom.camerafilter;

import android.databinding.BindingAdapter;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import static cn.pinchzoom.camerafilter.MainActivity.TAG;


/**
 * Thread that handles all rendering and camera operations.
 */
public class CameraThread extends Thread {
    // Object must be created on render thread to get correct Looper, but is used from
    // UI thread, so we need to declare it volatile to ensure the UI thread sees a fully
    // constructed object.
    private volatile CameraHandler mHandler;

    // Used to wait for the thread to start.
    private Object mStartLock = new Object();
    private boolean mReady = false;

    private CameraRenderer renderer;

    private float mDist = 0;

    private static final int THROTTLE_RATE = 4;
    private int throttleCount = 0;

    CameraThread(CameraRenderer renderer) {
        this.renderer = renderer;
    }


    /**
     * Thread entry point.
     */
    @Override
    public void run() {
        Looper.prepare();

        // We need to create the Handler before reporting ready.
        mHandler = new CameraHandler(this);
        synchronized (mStartLock) {
            mReady = true;
            mStartLock.notify();    // signal waitUntilReady()
        }

        Looper.loop();

        Log.d(TAG, "looper quit");

        synchronized (mStartLock) {
            mReady = false;
        }
    }

    /**
     * Waits until the render thread is ready to receive messages.
     * <p>
     * Call from the UI thread.
     */
    public void waitUntilReady() {
        synchronized (mStartLock) {
            while (!mReady) {
                try {
                    mStartLock.wait();
                } catch (InterruptedException ie) { /* not expected */ }
            }
        }
    }

    /**
     * Shuts everything down.
     */
    void shutdown() {
        Log.d(TAG, "shutdown");
        Looper.myLooper().quit();
    }

    /**
     * Returns the render thread's Handler.  This may be called from any thread.
     */
    public CameraHandler getHandler() {
        return mHandler;
    }


    public void handleEvent(MotionEvent event) {
        try {

            throttleCount++;
            if (throttleCount < THROTTLE_RATE) {
                return;
            }
            throttleCount = 0;

            float newDist = getFingerSpacing(event);
            int zoom = renderer.getZoomLevel();

            int mMaxZoom = renderer.getMaxZoom();
            int mZoomStep = Math.max(mMaxZoom * 5 / 100, 1);

            if (newDist > mDist) {
                //zoom in
                if (zoom + mZoomStep < mMaxZoom)
                    zoom += mZoomStep;
            } else if (newDist < mDist) {
                //zoom out
                if (zoom - mZoomStep > 0)
                    zoom -= mZoomStep;
            }
            mDist = newDist;
            renderer.zoom(zoom);
        } catch (IllegalArgumentException e) {

        }
    }


    public void initZoom(MotionEvent event) {
        mDist = getFingerSpacing(event);
    }

    /**
     * Determine the space between the first two fingers
     */
    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    public static class DataBindingAdapter {
        @BindingAdapter("imageResource")
        public static void setImageResource(ImageView imageView, int resource) {
            imageView.setImageResource(resource);
        }
    }
}

