package com.example.arcore.chapter7.example7_3;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

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

public class MainActivity extends Activity implements SensorEventListener, LocationListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private String mTextString;
    private TextView mTextView;
    private GLSurfaceView mSurfaceView;
    private MainRenderer mRenderer;

    private boolean mUserRequestedInstall = true;

    private Session mSession;
    private Config mConfig;

    private float[] mProjMatrix = new float[16];
    private float[] mViewMatrix = new float[16];

    private float mCurrentX;
    private float mCurrentY;
    private boolean mTouched = false;

    //*********************** Azimuth ********************************
    private SensorManager sensorManager;
    private float[] accelerometerReading = new float[3];
    private float[] magnetometerReading = new float[3];

    private float[] gravityReading = new float[3]; // Test: gravity

    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];
    private float inclinationAngles = 0;

    // ***************************************************************
    // Test for a low pass filter
    private float[] accReading_LP;
    private float[] magReading_LP;
    private float[] rotationMatrix_LP = new float[9];
    private float[] orientationAngles_LP = new float[3];
    private float inclinationAngles_LP = 0;
    // ***************************************************************

    private LocationManager locationManager;
    private Location mLocation;
    private float declination;

    static final float ALPHA = 0.25f;
    // ****************************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBarAndTitleBar();
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

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

                    /*
                    Camera camera = frame.getCamera();
                    float[] projMatrix = new float[16];
                    camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100.0f);
                    float[] viewMatrix = new float[16];
                    camera.getViewMatrix(viewMatrix, 0);

                    mRenderer.setProjectionMatrix(projMatrix);
                    mRenderer.updateViewMatrix(viewMatrix);
                    */

                    Camera camera = frame.getCamera();
                    camera.getProjectionMatrix(mProjMatrix, 0, 0.1f, 100.0f);
                    camera.getViewMatrix(mViewMatrix, 0);

                    //mRenderer.setProjectionMatrix(mProjMatrix);
                    //mRenderer.updateViewMatrix(mViewMatrix);

                    // ************ Test for Visualizing Data **********
//                    mRenderer.addPoint(1, 0 , 0);
//                    mRenderer.addPoint((float)0.5, 0 , 0);
//                    mRenderer.addPoint(0, 1 , 0);
//                    mRenderer.addPoint(0, 0 , 1);
//                    mRenderer.addPoint(0, 0 , -1);
//                    mRenderer.addPoint(0, 0 , (float)-0.5);

                    mRenderer.addLine(0, 0, -1, 1, 0, -1);
                    mRenderer.addLine(0, 0, -1, 1, 1, -1);
                    mRenderer.addLine(0, 0, -1, 0, 1, -1);
                    mRenderer.addLine(0, 0, -1, -1, 0, -1);
                    mRenderer.addLine(0, 0, -1, -1, 1, -1);
                    mRenderer.addLine(0, 0, -1, 1, 1, 0);
                    // *************************************************

                    mTextString = "";
                    if (mTouched) {
                        //mTextString = "";
                        List<HitResult> results = frame.hitTest(mCurrentX, mCurrentY);
                        int i = 0;
                        for (HitResult result : results) {
                            //float distance = result.getDistance();
                            Pose pose = result.getHitPose();
                            Anchor anchor = result.createAnchor();
                            Pose anchorPose = anchor.getPose();

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
                            //mTextString += ("HitResult Pose : " + pose.toString() + "\n");
                            //mTextString += ("Anchor Pose : " + anchorPose.toString() + "\n");
                            anchor.detach();
                            i++;
                        }
                        mTouched = false;
                    }

                    //Pose camPose = camera.getDisplayOrientedPose();
                    Pose camPose = camera.getPose();

                    //**************************************************************

                    float[] xAxis = camPose.getXAxis();
                    float[] yAxis = camPose.getYAxis();
                    float[] zAxis = camPose.getZAxis();

                    //******************* Gravity Normalization *********************
                    double normOfG = Math.sqrt(gravityReading[0] * gravityReading[0]
                            + gravityReading[1] * gravityReading[1]
                            + gravityReading[2] * gravityReading[2]);

                    gravityReading[0] = (float) (gravityReading[0] / normOfG);
                    gravityReading[1] = (float) (gravityReading[1] / normOfG);
                    gravityReading[2] = (float) (gravityReading[2] / normOfG);

                    //float inclination = (float)Math.toDegrees(Math.acos(gravityReading[2]));
                    //***************************************************************

                    // Azimuth - kappa
                    float azimuth = (float)Math.toDegrees(orientationAngles[0]);
                    float azimuth_LP = (float)Math.toDegrees(orientationAngles_LP[0]);

                    // Pitch - omega
                    float pitch = (float)Math.toDegrees(orientationAngles[1]);

                    // Roll - phi
                    float roll = (float)Math.toDegrees(orientationAngles[2]);

                    mTextString += ("Camera Pose: " + camPose.toString() + "\n"
                            + "inclination: " + String.format("%.2f", inclinationAngles) + "\n"
                            + "xAxis: " + String.format("%.2f, %.2f, %.2f", xAxis[0], xAxis[1], xAxis[2]) + "\n"
                            + "yAxis: " + String.format("%.2f, %.2f, %.2f", yAxis[0], yAxis[1], yAxis[2]) + "\n"
                            + "zAxis: " + String.format("%.2f, %.2f, %.2f", zAxis[0], zAxis[1], zAxis[2]) + "\n"
                            + "Azimuth(magnetic): " + String.format("%3.3f", azimuth) + "\n"
                            + "Azimuth(true): " + String.format("%3.3f", azimuth + declination) + "\n"
                            + "Azimuth(true, LP): " + String.format("%3.3f", azimuth_LP + declination) + "\n"
                            + "declination: " + String.format("%.2f", declination) + "\n"
                            + "TrackingState: " + camera.getTrackingState());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTextView.setText(mTextString);
                        }
                    });

                    mRenderer.setProjectionMatrix(mProjMatrix);
                    mRenderer.updateViewMatrix(mViewMatrix);

                    //mRenderer.setProjectionMatrix(projMatrix);
                    //mRenderer.updateViewMatrix(viewMatrix);
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

        requestGPSPermission();

        for (final String provider : locationManager.getProviders(true)) {
            if (LocationManager.GPS_PROVIDER.equals(provider)
                    || LocationManager.PASSIVE_PROVIDER.equals(provider)
                    || LocationManager.NETWORK_PROVIDER.equals(provider)) {
                if (mLocation == null) {
                    mLocation = locationManager.getLastKnownLocation(provider);
                }
                locationManager.requestLocationUpdates(provider, 0, 100.0f, this);
            }
        }

        mSurfaceView.onResume();
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {
        /*
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
        */

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.length);
            accReading_LP = lowPass(event.values.clone(), accReading_LP);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
            magReading_LP = lowPass(event.values.clone(), magReading_LP);
        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            System.arraycopy(event.values, 0, gravityReading,
                    0, gravityReading.length);
        }

        if (accReading_LP != null && magReading_LP != null) {
            updateOrientationAngles_LP();
        }

        updateOrientationAngles();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;
        for (int i = 0; i < input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        float[] R = new float[9];

        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        inclinationAngles = (float) Math.toDegrees(Math.acos(rotationMatrix[8]));

        if (inclinationAngles < 25 || inclinationAngles > 155) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
        } else {
            //Remap to camera's point-of-view
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);
            SensorManager.getOrientation(R, orientationAngles);
        }
    }

    public void updateOrientationAngles_LP() {
        float[] R = new float[9];

        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix_LP, null,
                accReading_LP, magReading_LP);

        inclinationAngles_LP = (float) Math.toDegrees(Math.acos(rotationMatrix_LP[8]));

        if (inclinationAngles_LP < 25 || inclinationAngles_LP > 155) {
            SensorManager.getOrientation(rotationMatrix_LP, orientationAngles_LP);
        } else {
            //Remap to camera's point-of-view
            SensorManager.remapCoordinateSystem(rotationMatrix_LP, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);
            SensorManager.getOrientation(R, orientationAngles_LP);
        }
    }

    //==============================================================================================
    // LocationListener implementation
    //==============================================================================================
    // https://stackoverflow.com/questions/42200059/calculate-true-heading-correctly-in-android

    @Override
    public void onLocationChanged(Location location)
    {
        // set the new location
        this.mLocation = location;

        declination = getGeomagneticField(this.mLocation).getDeclination();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {   }

    @Override
    public void onProviderEnabled(String s) {   }

    @Override
    public void onProviderDisabled(String s) {   }

    //==============================================================================================
    // Private Utilities
    //==============================================================================================

    private GeomagneticField getGeomagneticField(Location location)
    {
        GeomagneticField geomagneticField = new GeomagneticField(
                (float)location.getLatitude(),
                (float)location.getLongitude(),
                (float)location.getAltitude(),
                System.currentTimeMillis());
        return geomagneticField;
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

    private void requestGPSPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
    }

    private void hideStatusBarAndTitleBar() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}
