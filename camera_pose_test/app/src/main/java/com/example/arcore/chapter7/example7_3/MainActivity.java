package com.example.arcore.chapter7.example7_3;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import java.io.IOException;

import java.util.List;

public class MainActivity extends Activity implements SensorEventListener {
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

    //*********************** Test: Azimuth **************************
    private SensorManager sensorManager;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private final float[] gravityReading = new float[3]; // Test: gravity

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    // ****************************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBarAndTitleBar();
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

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

                try {
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

                    //******************* Test: Android Sensor Pose *****************
                    Pose androidPose = frame.getAndroidSensorPose();

                    float[] androidXAxis = androidPose.getXAxis();
                    float[] androidYAxis = androidPose.getYAxis();
                    float[] androidZAxis = androidPose.getZAxis();
                    //***************************************************************

                    // Azimuth - kappa
                    float azimuth = -(float)Math.toDegrees(orientationAngles[0]);
                    if (azimuth < 0) azimuth += 360.0f;

                    // Pitch - omega
                    float pitch = (float)Math.toDegrees(orientationAngles[1]);

                    // Roll - phi
                    float roll = (float)Math.toDegrees(orientationAngles[2]);
                    if (roll < 0) roll += 360.0f;

                    mTextString += ("Camera Pose: " + camPose.toString() + "\n"
                            + "xAxis: " + String.format("%.2f, %.2f, %.2f", xAxis[0], xAxis[1], xAxis[2]) + "\n"
                            + "yAxis: " + String.format("%.2f, %.2f, %.2f", yAxis[0], yAxis[1], yAxis[2]) + "\n"
                            + "zAxis: " + String.format("%.2f, %.2f, %.2f", zAxis[0], zAxis[1], zAxis[2]) + "\n"
                            + "Azimuth(Z): " + String.format("%3.3f", azimuth) + "\n"
                            + "Pitch(X): " + String.format("%3.3f", pitch) + "\n"
                            + "Roll(Y): " + String.format("%3.3f", roll) + "\n"
                            + "gravityX: " + String.format("%.2f", gravityReading[0]) + "\n"
                            + "gravityY: " + String.format("%.2f", gravityReading[1]) + "\n"
                            + "gravityZ: " + String.format("%.2f", gravityReading[2]) + "\n");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTextView.setText(mTextString);
                        }
                    });

                    mRenderer.setProjectionMatrix(projMatrix);
                    mRenderer.updateViewMatrix(viewMatrix);
                }
                catch (Throwable t) {
                    // Avoid crashing the application due to unhandled exceptions.
                    Log.e(TAG, "Exception on the OpenGL thread", t);
                }
            }
        });
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setRenderer(mRenderer);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSurfaceView.onPause();
        mSession.pause();

        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this);
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
            return;
        }
        catch (UnavailableArcoreNotInstalledException
                | UnavailableUserDeclinedInstallationException e) {
            Log.e(TAG, "Please install ARCore", e);
            return;
        } catch (UnavailableApkTooOldException e) {
            Log.e(TAG, "Please update ARCore", e);
            return;
        } catch (UnavailableSdkTooOldException e) {
            Log.e(TAG, "Please update this app", e);
            return;
        } catch (UnavailableDeviceNotCompatibleException e) {
            Log.e(TAG, "This device does not support AR", e);
            return;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create AR session", e);
            return;
        }

        /*
        mConfig = new Config(mSession);
        if (!mSession.isSupported(mConfig)) {
            Log.d(TAG, "This device is not support ARCore.");
        }
        mSession.configure(mConfig);
        mSession.resume();
        */

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            mSession.resume();
        } catch (CameraNotAvailableException e) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            Log.e(TAG, "Camera not available. Please restart the app.", e);
            mSession = null;
            return;
        }

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (gravity != null) {
            sensorManager.registerListener(this, gravity,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }

        mSurfaceView.onResume();
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            System.arraycopy(event.values, 0, gravityReading,
                    0, gravityReading.length);
        }

        updateOrientationAngles();
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles);

        // "mOrientationAngles" now has up-to-date information.
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
