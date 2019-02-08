package com.example.arcore.chapter7.example7_3;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private String mTextString;
    private TextView mTextView;
    private GLSurfaceView mSurfaceView;
    private MainRenderer mRenderer;

    private boolean mUserRequestedInstall = true;

    private Session mSession;
    private Config mConfig;

    private float mCurrentX;
    private float mCurrentY;
    private boolean mTouched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBarAndTitleBar();
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.ar_core_text);
        mSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface_view);

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    synchronized (this) {
                        mRenderer.onDisplayChanged();
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }
            }, null);
        }

        mRenderer = new MainRenderer(new MainRenderer.RenderCallback() {
            @Override
            public void preRender() {
                if (mRenderer.isViewportChanged()) {
                    Display display = getWindowManager().getDefaultDisplay();
                    int displayRotation = display.getRotation();
                    mRenderer.updateSession(mSession, displayRotation);
                }

                mSession.setCameraTextureName(mRenderer.getTextureId());

                Frame frame = mSession.update();
                if (frame.hasDisplayGeometryChanged()) {
                    mRenderer.transformDisplayGeometry(frame);
                }

                PointCloud pointCloud = frame.acquirePointCloud();
                mRenderer.updatePointCloud(pointCloud);
                pointCloud.release();

                mTextString = "";
                if (mTouched) {
                    //mTextString = "";
                    List<HitResult> results = frame.hitTest(mCurrentX, mCurrentY);
                    int i = 0;
                    for (HitResult result : results) {
                        //float distance = result.getDistance();
                        //Pose pose = result.getHitPose();
                        Anchor anchor = result.createAnchor();
                        Pose pose = anchor.getPose();

                        float[] xAxis = pose.getXAxis();
                        float[] yAxis = pose.getYAxis();
                        float[] zAxis = pose.getZAxis();
                        mRenderer.addPoint(pose.tx(), pose.ty(), pose.tz());
                        mRenderer.addLineX(pose.tx(), pose.ty(), pose.tz(),
                                             xAxis[0], xAxis[1], xAxis[2]);
                        mRenderer.addLineY(pose.tx(), pose.ty(), pose.tz(),
                                             yAxis[0], yAxis[1], yAxis[2]);
                        mRenderer.addLineZ(pose.tx(), pose.ty(), pose.tz(),
                                             zAxis[0], zAxis[1], zAxis[2]);
                        //mTextString += ("[" + i + "] distance : " + distance
                        //        + ", Pose : " + pose.toString() + "\n");
                        //mTextString += ("Anchor Pose : " + pose.toString() + "\n");
                        //anchor.detach();
                        i++;
                    }
                    mTouched = false;
                }

                Camera camera = frame.getCamera();
                float[] projMatrix = new float[16];
                camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100.0f);
                float[] viewMatrix = new float[16];
                camera.getViewMatrix(viewMatrix, 0);

                Pose camPose = camera.getDisplayOrientedPose();

                float[] xAxis = camPose.getXAxis();
                float[] yAxis = camPose.getYAxis();
                float[] zAxis = camPose.getZAxis();

                mTextString += ("Camera Pose: " + camPose.toString() + "\n"
                        + "xAxis: " + String.format("%.2f, %.2f, %.2f", xAxis[0], xAxis[1], xAxis[2]) + "\n"
                        + "yAxis: " + String.format("%.2f, %.2f, %.2f", yAxis[0], yAxis[1], yAxis[2]) + "\n"
                        + "zAxis: " + String.format("%.2f, %.2f, %.2f", zAxis[0], zAxis[1], zAxis[2]) + "\n");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextView.setText(mTextString);
                    }
                });

                mRenderer.setProjectionMatrix(projMatrix);
                mRenderer.updateViewMatrix(viewMatrix);
            }
        });
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setRenderer(mRenderer);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSurfaceView.onPause();
        mSession.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        requestCameraPermission();

        try {
            if (mSession == null) {
                switch (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
                    case INSTALLED:
                        mSession = new Session(this);
                        Log.d(TAG, "ARCore Session created.");
                        break;
                    case INSTALL_REQUESTED:
                        mUserRequestedInstall = false;
                        Log.d(TAG, "ARCore should be installed.");
                        break;
                }
            }
        }
        catch (UnsupportedOperationException e) {
            Log.e(TAG, e.getMessage());
        }

        mConfig = new Config(mSession);
        if (!mSession.isSupported(mConfig)) {
            Log.d(TAG, "This device is not support ARCore.");
        }
        mSession.configure(mConfig);
        mSession.resume();

        mSurfaceView.onResume();
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mCurrentX = event.getX();
                mCurrentY = event.getY();
                mTouched = true;
                break;
        }
        return true;
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        }
    }

    private void hideStatusBarAndTitleBar() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}
