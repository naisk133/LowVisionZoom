package cn.pinchzoom.camerafilter;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;

import java.lang.ref.WeakReference;

import static cn.pinchzoom.camerafilter.MainActivity.TAG;

/**
 * Handler for CameraThread.  Used for messages sent from the UI thread to the render thread.
 * <p>
 * The object is created on the render thread, and the various "send" methods are called
 * from the UI thread.
 */
public class CameraHandler extends Handler {
    private static final int MSG_SHUTDOWN = 1;
    private static final int MSG_INIT_ZOOM = 2;
    private static final int MSG_MOTION_EVENT = 3;

    // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
    // but no real harm in it.
    private WeakReference<CameraThread> mWeakRenderThread;

    /**
     * Call from render thread.
     */
    public CameraHandler(CameraThread rt) {
        mWeakRenderThread = new WeakReference<CameraThread>(rt);
    }

    /**
     * Sends the "shutdown" message, which tells the render thread to halt.
     * <p>
     * Call from UI thread.
     */
    public void sendShutdown() {
        sendMessage(obtainMessage(MSG_SHUTDOWN));
    }

    public void sendInitZoom(MotionEvent e) {
        sendMessage(obtainMessage(MSG_INIT_ZOOM, e));
    }

    public void sendZoom(MotionEvent e) {
        sendMessage(obtainMessage(MSG_MOTION_EVENT, e));
    }

    @Override  // runs on CameraThread
    public void handleMessage(Message msg) {
        int what = msg.what;
        //Log.d(TAG, "CameraHandler [" + this + "]: what=" + what);

        CameraThread cameraThread = mWeakRenderThread.get();
        if (cameraThread == null) {
            Log.w(TAG, "CameraHandler.handleMessage: weak ref is null");
            return;
        }

        MotionEvent e;
        switch (what) {
            case MSG_SHUTDOWN:
                cameraThread.shutdown();
                break;
            case MSG_INIT_ZOOM:
                e = (MotionEvent) msg.obj;
                cameraThread.initZoom(e);
                break;
            case MSG_MOTION_EVENT:
                e = (MotionEvent) msg.obj;
                cameraThread.handleEvent(e);
                break;
            default:
                throw new RuntimeException("unknown message " + what);
        }
    }
}
