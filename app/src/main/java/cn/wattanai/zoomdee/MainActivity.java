package cn.wattanai.zoomdee;

import android.Manifest;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Locale;

import cn.wattanai.zoomdee.databinding.ActivityMainBinding;
import cn.wattanai.zoomdee.filter.CameraFilter;
import me.toptas.fancyshowcase.FancyShowCaseQueue;
import me.toptas.fancyshowcase.FancyShowCaseView;
import me.toptas.fancyshowcase.OnViewInflateListener;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, View.OnTouchListener {
    public static final String TAG = "MainActivity";

    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private CameraRenderer renderer;
    private TextureView textureView;

    private ImageView captureButton;
    private boolean captureFlag = false;
    private ImageView captureImageView;

    private ImageView constrastButton;
    private int contrastIndex = 0;

    private ImageView flashButton;
    private boolean flashFlag = false;
    private TextToSpeech tts;

    private ImageView helpButton;

    private CameraThread mCameraThread;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setCaptureButtonIcon(R.drawable.ic_capture);
        binding.setVolumeButtonIcon(R.drawable.ic_volume_on);

        tts = new TextToSpeech(this, this);


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);

        } else {
            setupCameraPreviewView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mCameraThread = new CameraThread(renderer);
        mCameraThread.start();
        mCameraThread.waitUntilReady();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraThread == null) {
            return;
        }
        CameraHandler rh = mCameraThread.getHandler();

        rh.sendShutdown();
        try {
            mCameraThread.join();
        } catch (InterruptedException ie) {
            // not expected
            throw new RuntimeException("join was interrupted", ie);
        }
        mCameraThread = null;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Do something here
            int result = tts.setLanguage(new Locale("th"));
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(getApplicationContext(), "This language is not supported", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupCameraPreviewView();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA_PERMISSION);
                }
            }
        }
    }

    private FancyShowCaseView.Builder captureShowCaseBuilder() {
        return new FancyShowCaseView.Builder(this)
                .focusOn(captureButton)
                .title("กดปุ่มนี้เพื่อหยุดภาพ")
                .titleSize(48, TypedValue.COMPLEX_UNIT_SP);
    }

    private FancyShowCaseView.Builder flashShowCaseBuilder() {
        return new FancyShowCaseView.Builder(this)
                .focusOn(flashButton)
                .title("กดปุ่มนี้เพื่อเปิด/ปิดไฟ")
                .titleSize(48, TypedValue.COMPLEX_UNIT_SP);
    }

    private FancyShowCaseView.Builder contrastShowCaseBuilder() {
        return new FancyShowCaseView.Builder(this)
                .focusOn(constrastButton)
                .title("กดปุ่มนี้เพื่อเปลี่ยนสี")
                .titleSize(48, TypedValue.COMPLEX_UNIT_SP);
    }

    private FancyShowCaseView.Builder pinchZoomShowCaseBuilder() {
        return new FancyShowCaseView.Builder(this)
                .focusOn(textureView)
                .customView(R.layout.pinch_tutorial, new OnViewInflateListener() {
                    @Override
                    public void onViewInflated(@NonNull View view) {

                    }
                });
    }

    private void displayManual() {
        FancyShowCaseView captureShowCase = captureShowCaseBuilder().build();
        FancyShowCaseView contrastShowCase = contrastShowCaseBuilder().build();
        FancyShowCaseView flashShowCase = flashShowCaseBuilder().build();
        FancyShowCaseView pinchZoomShowCase = pinchZoomShowCaseBuilder().build();

        final FancyShowCaseQueue queue = new FancyShowCaseQueue()
                .add(captureShowCase)
                .add(contrastShowCase)
                .add(flashShowCase)
                .add(pinchZoomShowCase);

        queue.show();
    }

    private void displayManualOnce() {
        FancyShowCaseView captureShowCase = captureShowCaseBuilder().showOnce("capture").build();
        FancyShowCaseView contrastShowCase = contrastShowCaseBuilder().showOnce("contrast").build();
        FancyShowCaseView flashShowCase = flashShowCaseBuilder().showOnce("flash").build();
        FancyShowCaseView pinchZoomShowCase = pinchZoomShowCaseBuilder().showOnce("pinchZoom").build();

        final FancyShowCaseQueue queue = new FancyShowCaseQueue()
                .add(captureShowCase)
                .add(contrastShowCase)
                .add(flashShowCase)
                .add(pinchZoomShowCase);

        queue.show();
    }

    private void setupCameraPreviewView() {
        renderer = new CameraRenderer(this);
        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(renderer);

        if (textureView.isAvailable()) {
            renderer.onSurfaceTextureAvailable(textureView.getSurfaceTexture(), textureView.getWidth(), textureView.getHeight());
        }

        textureView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                renderer.onSurfaceTextureSizeChanged(null, v.getWidth(), v.getHeight());
            }
        });

        textureView.setOnTouchListener(this);

        captureImageView = findViewById(R.id.captureImage);

        captureButton = findViewById(R.id.capture);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!captureFlag) {
                    capture();
                } else {
                    uncapture();
                }
            }
        });

        constrastButton = findViewById(R.id.contrast);
        constrastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SparseArray<CameraFilter> filters = renderer.getCameraFilterMap();
                contrastIndex++;
                contrastIndex %= filters.size();
                renderer.setSelectedFilter(filters.keyAt(contrastIndex));

                String filterText = renderer.getSelectedFilterName();
                tts.speak(String.format(Locale.ENGLISH, filterText, contrastIndex + 1), TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        flashButton = findViewById(R.id.flash);
        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!flashFlag) {
                    tts.speak("เปิดไฟ", TextToSpeech.QUEUE_FLUSH, null);
                    renderer.flash();
                    flashFlag = true;
                } else {
                    tts.speak("ปิดไฟ", TextToSpeech.QUEUE_FLUSH, null);
                    renderer.unflash();
                    flashFlag = false;
                }
            }
        });

        helpButton = findViewById(R.id.help);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayManual();
            }
        });

        displayManualOnce();
    }

    private void capture() {
        tts.speak("ภาพนิ่ง", TextToSpeech.QUEUE_FLUSH, null);
        Bitmap bm = textureView.getBitmap();
        captureImageView.setImageBitmap(bm);
        captureImageView.setVisibility(View.VISIBLE);
        captureFlag = true;
        binding.setCaptureButtonIcon(R.drawable.ic_play_arrow);
    }

    private void uncapture() {
        tts.speak("ภาพเคลื่อนไหว", TextToSpeech.QUEUE_FLUSH, null);
        captureImageView.setVisibility(View.INVISIBLE);
        captureFlag = false;
        binding.setCaptureButtonIcon(R.drawable.ic_capture);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // Get the pointer ID
        int action = event.getAction();


        if (event.getPointerCount() > 1) {
            // handle multi-touch events
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                mCameraThread.getHandler().sendInitZoom(event);
            } else if (action == MotionEvent.ACTION_MOVE) {
                mCameraThread.getHandler().sendZoom(event);
            }
        } else {
            // handle single touch events
            if (action == MotionEvent.ACTION_UP) {
                renderer.focus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean b, Camera camera) {
                    }
                });
            }
        }
        view.performClick();
        return true;
    }
}