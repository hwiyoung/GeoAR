package com.example.arcore.chapter7.example7_3;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.Frame;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = MainRenderer.class.getSimpleName();

    private boolean mViewportChanged;
    private int mViewportWidth;
    private int mViewportHeight;

    private CameraRenderer mCamera;
    private PointCloudRenderer mPointCloud;

    private List<Sphere> mSpheres = new ArrayList<Sphere>();
    private List<Line> mLines = new ArrayList<Line>();
    private List<Polygon> mPolygons = new ArrayList<Polygon>();

    private Line mLineX;
    private Line mLineY;
    private Line mLineZ;

    private float[] mProjMatrix = new float[16];

    private RenderCallback mRenderCallback;

    public interface RenderCallback {
        void preRender();
    }

    public MainRenderer(RenderCallback callback) {
        mCamera = new CameraRenderer();
        mPointCloud = new PointCloudRenderer();

        mRenderCallback = callback;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);

        mCamera.init();
        mPointCloud.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mViewportChanged = true;
        mViewportWidth = width;
        mViewportHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mRenderCallback.preRender();

        GLES20.glDepthMask(false);
        mCamera.draw();
        GLES20.glDepthMask(true);

        mPointCloud.draw();

        for (int i = 0; i < mSpheres.size(); i++) {
            Sphere sphere = mSpheres.get(i);
            if (!sphere.isInitialized()) {
                sphere.init();
            }
            sphere.draw();
        }

        for (int i = 0; i < mLines.size(); i++) {
            Line line = mLines.get(i);
            if (!line.isInitialized()) {
                line.init();
            }
            line.draw();
        }

        for (int i = 0; i < mPolygons.size(); i++) {
            Polygon polygon = mPolygons.get(i);
            if (!polygon.isInitialized()) {
                polygon.init();
            }
            polygon.draw();
        }

        if (mLineX != null) {
            if (!mLineX.isInitialized()) {
                mLineX.init();
            }
            mLineX.draw();
        }
        if (mLineY != null) {
            if (!mLineY.isInitialized()) {
                mLineY.init();
            }
            mLineY.draw();
        }
        if (mLineZ != null) {
            if (!mLineZ.isInitialized()) {
                mLineZ.init();
            }
            mLineZ.draw();
        }
    }

    public int getTextureId() {
        return mCamera == null ? -1 : mCamera.getTextureId();
    }

    public void onDisplayChanged() {
        mViewportChanged = true;
    }

    public boolean isViewportChanged() {
        return mViewportChanged;
    }

    public void updateSession(Session session, int displayRotation) {
        if (mViewportChanged) {
            session.setDisplayGeometry(displayRotation, mViewportWidth, mViewportHeight);
            mViewportChanged = false;
        }
    }

    public void transformDisplayGeometry(Frame frame) {
        mCamera.transformDisplayGeometry(frame);
    }

    public void updatePointCloud(PointCloud pointCloud) {
        mPointCloud.update(pointCloud);
    }

    public void setProjectionMatrix(float[] matrix) {
        System.arraycopy(matrix, 0, mProjMatrix, 0, 16);

        mPointCloud.setProjectionMatrix(matrix);
    }

    public void updateViewMatrix(float[] matrix) {
        mPointCloud.setViewMatrix(matrix);

        //mPoint.setViewMatrix(matrix);

        for (int i = 0; i < mSpheres.size(); i++) {
            mSpheres.get(i).setViewMatrix(matrix);
        }

        for (int i = 0; i < mLines.size(); i++) {
            mLines.get(i).setViewMatrix(matrix);
        }

        for (int i = 0; i < mPolygons.size(); i++) {
            mPolygons.get(i).setViewMatrix(matrix);
        }

        if (mLineX != null) {
            mLineX.setViewMatrix(matrix);
        }
        if (mLineY != null) {
            mLineY.setViewMatrix(matrix);
        }
        if (mLineZ != null) {
            mLineZ.setViewMatrix(matrix);
        }
    }

    public void setModelMatrix(float[] matrix) {
    }

    public void addPoint(float x, float y, float z) {
        Sphere currentPoint = new Sphere(0.01f, Color.YELLOW);
        currentPoint.setProjectionMatrix(mProjMatrix);

        float[] translation = new float[16];
        Matrix.setIdentityM(translation, 0);
        Matrix.translateM(translation, 0, x, y, z);
        currentPoint.setModelMatrix(translation);

        mSpheres.add(currentPoint);
    }

    public void addLine(float x1, float y1, float z1, float x2, float y2, float z2) {
        Line currentLine = new Line(x1, y1, z1, x2, y2, z2, 10, Color.WHITE);
        currentLine.setProjectionMatrix(mProjMatrix);

        float[] identity = new float[16];
        Matrix.setIdentityM(identity, 0);
        currentLine.setModelMatrix(identity);

        mLines.add(currentLine);
    }

    public void addPolygon(float x, float y, float z) {
        Polygon currentPolygon = new Polygon(Color.GRAY, 0.5f);
        currentPolygon.setProjectionMatrix(mProjMatrix);

        float[] identity = new float[16];
        Matrix.setIdentityM(identity, 0);
        currentPolygon.setModelMatrix(identity);

        mPolygons.add(currentPolygon);
    }

    public void addLineX(float x1, float y1, float z1, float x2, float y2, float z2) {
        mLineX = new Line(x1, y1, z1, x2, y2, z2, 10, Color.RED);
        mLineX.setProjectionMatrix(mProjMatrix);

        float[] identity = new float[16];
        Matrix.setIdentityM(identity, 0);
        mLineX.setModelMatrix(identity);
    }

    public void addLineY(float x1, float y1, float z1, float x2, float y2, float z2) {
        mLineY = new Line(x1, y1, z1, x2, y2, z2, 10, Color.GREEN);
        mLineY.setProjectionMatrix(mProjMatrix);

        float[] identity = new float[16];
        Matrix.setIdentityM(identity, 0);
        mLineY.setModelMatrix(identity);
    }

    public void addLineZ(float x1, float y1, float z1, float x2, float y2, float z2) {
        mLineZ = new Line(x1, y1, z1, x2, y2, z2, 10, Color.BLUE);
        mLineZ.setProjectionMatrix(mProjMatrix);

        float[] identity = new float[16];
        Matrix.setIdentityM(identity, 0);
        mLineZ.setModelMatrix(identity);
    }
}
