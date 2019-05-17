package com.example.arcore.chapter7.example7_3;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroupOverlay;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;

import com.example.arcore.chapter7.example7_3.ScreenshotHandler;

import static com.example.arcore.chapter7.example7_3.ScreenshotHandler.init;

public class MainActivity extends Activity implements SensorEventListener, LocationListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private String mTextString;
    private TextView mTextView;
    private GLSurfaceView mSurfaceView;
    private MainRenderer mRenderer;
    private TextView mAziTextView;
    private String mTextAzimuth;

    private boolean mUserRequestedInstall = true;

    private Session mSession;
    private Config mConfig;

    private float mCurrentX;
    private float mCurrentY;
    private boolean mTouched = false;

    //*********************** Azimuth ********************************
    private SensorManager sensorManager;
    private float[] accelerometerReading = new float[3];
    private float[] magnetometerReading = new float[3];

    private final float[] gravityReading = new float[3]; // Test: gravity

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private float inclinationAngles = 0;

    // ***************************************************************
    // Test for a low pass filter
    private float[] accReading_LP;
    private float[] magReading_LP;
    private final float[] rotationMatrix_LP = new float[9];
    private final float[] orientationAngles_LP = new float[3];
    private float inclinationAngles_LP = 0;
    // ***************************************************************

    private LocationManager locationManager;
    private Location mLocation;
    private float declination;

    static final float ALPHA = 0.25f;
    // ****************************************************************

    private Button ArScreenshotBtn;
    private Button ArCameraBtn;
    private Double lon;
    private Double lat;
    private Double tm_x;
    private Double tm_y;
    private Double tm_z;
    private String cLon = "";
    private String cLat = "";
    float[] xAxis;
    float[] yAxis;
    float[] zAxis;
    final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE_ = 1001;
    float azimuth;
    float azimuth_LP;
    float azimuth_true;

    final static String foldername1 = Environment.getExternalStorageDirectory()+File.separator+"DCIM/TEXT_Screenshot";
    final static String foldername2 = Environment.getExternalStorageDirectory()+File.separator+"DCIM/TEXT_Capture";
    final static String foldername3 = Environment.getExternalStorageDirectory()+File.separator+"DCIM/TEXT_frame";
//    final static String filename = "log.txt";

    private String filename;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBarAndTitleBar();
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mTextView = (TextView) findViewById(R.id.ar_core_text);
        mSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
        ArScreenshotBtn = (Button) findViewById(R.id.ar_core_screenshot_btn);
        ArCameraBtn = (Button) findViewById(R.id.ar_core_camera_btn);
        mAziTextView = (TextView) findViewById(R.id.azimuth_text);

        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        String now = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        filename = now+".txt";

        mAziTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {    }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {   }

            @Override
            public void afterTextChanged(Editable s) {
//                Toast.makeText(getApplicationContext(), "성공",Toast.LENGTH_LONG).show();
                String content = now + "_screenshot.jpg\t"+lat+"\t"+lon+"\t"
                        +tm_x+"\t"+tm_y+"\t"+tm_z+"\t"+azimuth+"\t"+azimuth_true+"\t"
                        +xAxis[0]+"\t"+xAxis[1]+"\t"+xAxis[2]+"\t"
                        +yAxis[0]+"\t"+yAxis[1]+"\t"+yAxis[2]+"\t"
                        +zAxis[0]+"\t"+zAxis[1]+"\t"+zAxis[2]+"\n"+ mTextAzimuth + "\n";
                WriteTextFile(foldername3, filename, content);
            }

            public void WriteTextFile(String foldername, String filename, String contents) {
                try {
                    File dir = new File (foldername);
                    if(!dir.exists()){
                        dir.mkdir();
                    }
                    FileOutputStream fos = new FileOutputStream(foldername+'/'+filename, true);

                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
                    writer.write(contents);
                    writer.flush();

                    writer.close();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        ArScreenshotBtn.setOnClickListener(new Button.OnClickListener() {   // ScreenShot
            @Override
            public void onClick(View view) {
                String result = takeAr(0);
                Toast.makeText(getApplicationContext(), result + " 에 저장되었습니다.", Toast.LENGTH_LONG).show();
                String now = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                String contents = now + "_screenshot.jpg\t"+lat+"\t"+lon+"\t"
                        +tm_x+"\t"+tm_y+"\t"+tm_z+"\t"+azimuth+"\t"+azimuth_true+"\t"
                        +xAxis[0]+"\t"+xAxis[1]+"\t"+xAxis[2]+"\t"
                        +yAxis[0]+"\t"+yAxis[1]+"\t"+yAxis[2]+"\t"
                        +zAxis[0]+"\t"+zAxis[1]+"\t"+zAxis[2]+"\n";
                WriteTextFile(foldername1, filename, contents);
            }

            public void WriteTextFile(String foldername, String filename, String contents){
                try{
                    File dir = new File (foldername);
                    if(!dir.exists()){
                        dir.mkdir();
                    }
                    FileOutputStream fos = new FileOutputStream(foldername+'/'+filename, true);

                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
                    writer.write(contents);
                    writer.flush();

                    writer.close();
                    fos.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        });

        ArCameraBtn.setOnClickListener(new Button.OnClickListener() {   // Only Camera
            @Override
            public void onClick(View view) {
                String result = takeAr(1);
                Toast.makeText(getApplicationContext(), result + " 에 저장되었습니다.", Toast.LENGTH_LONG).show();
                String now = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                String contents = now + "_screenshot.jpg\t"+lat+"\t"+lon+"\t"
                        +tm_x+"\t"+tm_y+"\t"+tm_z+"\t"+azimuth+"\t"+azimuth_true+"\t"
                        +xAxis[0]+"\t"+xAxis[1]+"\t"+xAxis[2]+"\t"
                        +yAxis[0]+"\t"+yAxis[1]+"\t"+yAxis[2]+"\t"
                        +zAxis[0]+"\t"+zAxis[1]+"\t"+zAxis[2]+"\n";
                WriteTextFile(foldername2, filename, contents);
            }

            public void WriteTextFile(String foldername, String filename, String contents){
                try{
                    File dir = new File (foldername);
                    if(!dir.exists()){
                        dir.mkdir();
                    }
                    FileOutputStream fos = new FileOutputStream(foldername+'/'+filename, true);

                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
                    writer.write(contents);
                    writer.flush();

                    writer.close();
                    fos.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        });

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

//                    PointCloud pointCloud = frame.acquirePointCloud();
//                    mRenderer.updatePointCloud(pointCloud);
//                    pointCloud.release();
//
//                    // ************ Test for Visualizing Data **********
//                    mRenderer.addPoint(1, 0, 0);
//                    mRenderer.addPoint((float) 0.5, 0, 0);
//                    mRenderer.addPoint(0, 1, 0);
//                    mRenderer.addPoint(0, 0, 1);
//                    mRenderer.addPoint(0, 0, -1);
//                    //mRenderer.addPoint(0, 0 , (float)-0.5);
//
//                    mRenderer.addLineY(0, 0, -1, 1, 0, -1);
                    // *************************************************
                    mTextAzimuth = "";
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

                    Camera camera = frame.getCamera();
                    float[] projMatrix = new float[16];
                    camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100.0f);
                    float[] viewMatrix = new float[16];
                    camera.getViewMatrix(viewMatrix, 0);

                    //Pose camPose = camera.getDisplayOrientedPose();
                    Pose camPose = camera.getPose();

                    mTextAzimuth += camera.getTrackingState() + "\n";
                    //**************************************************************

                    xAxis = camPose.getXAxis();
                    yAxis = camPose.getYAxis();
                    zAxis = camPose.getZAxis();

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
                    azimuth = (float) Math.toDegrees(orientationAngles[0]);
                    azimuth_LP = (float) Math.toDegrees(orientationAngles_LP[0]);
                    azimuth_true = azimuth_LP + declination;

                    // Pitch - omega
                    float pitch = (float) Math.toDegrees(orientationAngles[1]);

                    // Roll - phi
                    float roll = (float) Math.toDegrees(orientationAngles[2]);

                    mTextString += ("Camera Pose: " + camPose.toString() + "\n"
                            + "Azimuth(true, LP): " + String.format("%3.3f", azimuth_true) + "\n"
                            + "latitude: " + cLat + "\n"
                            + "longitude: " + cLon + "\n"
                            + "X: " + String.format("%.2f", tm_y) + "\n"
                            + "Y: " + String.format("%.2f", tm_x) + "\n"
                            + "Z: " + String.format("%.2f", tm_z) + "\n"
                            + camera.getTrackingState());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTextView.setText(mTextString);
                            mAziTextView.setText(mTextAzimuth);
                        }
                    });

                    if(ContextCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                            PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE_);
                    }

                    mRenderer.setProjectionMatrix(projMatrix);
                    mRenderer.updateViewMatrix(viewMatrix);
                } catch (Throwable t) {
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
        } catch (UnsupportedOperationException e) {
            Log.e(TAG, e.getMessage());
            return;
        } catch (UnavailableArcoreNotInstalledException
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
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    mLocation = locationManager.getLastKnownLocation(provider);
                }
                locationManager.requestLocationUpdates(provider, 0, 0, this);

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
    public void  onLocationChanged(Location location)    {
        // set the new location
        this.mLocation = location;
        cLat = Double.toString(location.getLatitude());
        cLon = Double.toString(location.getLongitude());
        lat = location.getLatitude();
        lon = location.getLongitude();
        tm_z = location.getAltitude();

        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CRSFactory csFactory = new CRSFactory();
        CoordinateReferenceSystem WGS84 = csFactory.createFromParameters(
                "EPSG: 4326", "+proj=longlat +datum=WGS84 +no_defs");
        CoordinateReferenceSystem TM = csFactory.createFromParameters(
                "EPSG: 5186", "+proj=tmerc +lat_0=38 +lon_0=127 " +
                        "+k=1 +x_0=200000 +y_0=600000 +ellps=GRS80 " +
                        "+towgs84=0,0,0,0,0,0,0 +units=m +no_defs");
        CoordinateTransform trans = ctFactory.createTransform(WGS84, TM);

        ProjCoordinate p = new ProjCoordinate();
        ProjCoordinate p2 = new ProjCoordinate();
        p.x = lon;
        p.y = lat;
        trans.transform(p, p2);
        tm_x = p2.x;
        tm_y = p2.y;
        Log.e("location","lat : " + cLat+ ", lon : " + cLon);

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

    private GeomagneticField getGeomagneticField(Location location) {
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

    /*
    private void screenshot(){

        Log.e("screenshot","스크린샷 시작");

        //View v = getWindow().getDecorView().getRootView();
        View v = getWindow().getDecorView();
        v.setDrawingCacheEnabled(true);
        Bitmap bmp = viewToBitmap(v);
        //Bitmap bmp = Bitmap.createBitmap(v.getDrawingCache());
        v.setDrawingCacheEnabled(false);
        try {
            FileOutputStream fos = new FileOutputStream(new File(Environment
                    .getExternalStorageDirectory().toString()+"/DCIM/Screenshots", "AR_Screenshot_"
                    + System.currentTimeMillis() + ".png"));
            Log.e("screenshot","경로 : " + Environment.getExternalStorageDirectory().toString()+"/DCIM/Screenshots");
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            Log.e("screenshot","스크린샷 종료");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    public String takeAr(int num) {
        final String filename = generateFilename();
        View view = mSurfaceView.getRootView();
        SurfaceView test = mSurfaceView;
        //ArSceneView view = arFragment.getArSceneView();

        // Create a bitmap the size of the scene view.
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Create a handler thread to offload the processing of the image.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();

        // Make the request to copy.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PixelCopy.request(mSurfaceView, bitmap, (copyResult) -> {
                if (copyResult == PixelCopy.SUCCESS) {
                    try {
                        saveBitmapToDisk(bitmap, filename, num);
                        Toast.makeText(this,"111111111",Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Toast toast = Toast.makeText(this, e.toString(),
                                Toast.LENGTH_LONG);
                        toast.show();
                        return;
                    }
                    //Toast.makeText(this,"저장저장저장",Toast.LENGTH_SHORT).show();
                    /*Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                            "Photo saved", Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction("열기", v -> {
                        File photoFile = new File(filename);

                        Uri photoURI = FileProvider.getUriForFile(this,
                                this.getPackageName() + ".save.provider",
                                photoFile);
                        Intent intent = new Intent(Intent.ACTION_VIEW, photoURI);
                        intent.setDataAndType(photoURI, "image/*");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    });
                    snackbar.show();*/
                } else {
                    Toast toast = Toast.makeText(this,
                            "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                    toast.show();
                }
                handlerThread.quitSafely();
            }, new Handler(handlerThread.getLooper()));
        }
        return filename;
    }

    private String generateFilename() {
        String date =
                null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            date = new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
        }
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM) + File.separator + "Screenshots/" + date + "_screenshot.jpg";
    }

    private void saveBitmapToDisk(Bitmap bitmap, String filename, int num) throws IOException {

        Bitmap result = null;
        if(num == 0){
            LinearLayout fContainer = (LinearLayout)findViewById(R.id.ar_layout);
            fContainer.buildDrawingCache();
            Bitmap fContainerLayoutView = fContainer.getDrawingCache();

            result = mergeToPin(bitmap, fContainerLayoutView);
        }else if (num == 1){
            result = bitmap;
        }
        /*LinearLayout fContainer = (LinearLayout)findViewById(R.id.ar_layout);
        fContainer.buildDrawingCache();
        Bitmap fContainerLayoutView = fContainer.getDrawingCache();

        Bitmap result = mergeToPin(bitmap, fContainerLayoutView);*/



        File out = new File(filename);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(filename);
             ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
            result.compress(Bitmap.CompressFormat.PNG, 100, outputData);
            outputData.writeTo(outputStream);
            outputStream.flush();

            outputStream.close();
        } catch (IOException ex) {
            throw new IOException("Failed to save bitmap to disk", ex);
        }
    }

    public static Bitmap mergeToPin(Bitmap back, Bitmap front) {
        Bitmap result = Bitmap.createBitmap(back.getWidth(), back.getHeight(), back.getConfig());
        Canvas canvas = new Canvas(result);
        int widthBack = 300; //back.getWidth();
        int widthFront = 100; //front.getWidth();
        //float move = (widthBack - widthFront) / 2;
        canvas.drawBitmap(back, 0f, 0f, null);
        //canvas.drawBitmap(front, move, move, null);
        canvas.drawBitmap(front, 0, 0, null);
        return result;
    }

}
