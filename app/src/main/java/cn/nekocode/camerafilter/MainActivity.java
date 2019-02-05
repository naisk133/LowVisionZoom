/*
 * Copyright 2016 nekocode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.nekocode.camerafilter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

import cn.nekocode.camerafilter.filter.CameraFilter;

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private CameraRenderer renderer;
    private TextureView textureView;

    private ImageView zoomButton;
    private TextView zoomLvl;
    private final float[] zoomPercents = new float[]{0.0f, 0.33f, 0.67f, 1.0f};
    private int zoomIndex = 0;

    private ImageView captureButton;
    private boolean captureFlag = false;
    private ImageView captureImageView;

    private ImageView constrastButton;
    private int contrastIndex = 0;

    private ImageView flashButton;
    private boolean flashFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Original");

        //TODO: handle the app
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Toast.makeText(this, "Camera access is required.", Toast.LENGTH_SHORT).show();

            }
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);

        } else {
            setupCameraPreviewView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupCameraPreviewView();
                }
            }
        }
    }

    void setupCameraPreviewView() {
        textureView = findViewById(R.id.textureView);
        renderer = new CameraRenderer(this);
        textureView.setSurfaceTextureListener(renderer);

        textureView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                renderer.onSurfaceTextureSizeChanged(null, v.getWidth(), v.getHeight());
            }
        });

        textureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                renderer.focus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean b, Camera camera) {
                    }
                });
            }
        });

        zoomLvl = findViewById(R.id.zoomLvl);
        zoomButton = findViewById(R.id.zoom);
        zoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zoomIndex++;
                zoomIndex %= zoomPercents.length;
                int magnifyValue = renderer.zoom(zoomPercents[zoomIndex]);
                String text = String.format(Locale.ENGLISH, "%.1fX", (float) magnifyValue / 100);
                zoomLvl.setText(text);
            }
        });

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
            }
        });

        flashButton = findViewById(R.id.flash);
        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!flashFlag) {
                    renderer.flash();
                    flashFlag = true;
                } else {
                    renderer.unflash();
                    flashFlag = false;
                }
            }
        });
    }

    private void capture() {
        Bitmap bm = textureView.getBitmap();
        captureImageView.setImageBitmap(bm);
        captureImageView.setVisibility(View.VISIBLE);
        captureFlag = true;
    }

    private void uncapture() {
        captureImageView.setVisibility(View.INVISIBLE);
        captureFlag = false;
    }
}
