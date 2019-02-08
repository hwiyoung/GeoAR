package com.example.arcore.chapter7.example7_3;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.PointCloud;

public class PointCloudRenderer {
    private static final String TAG = PointCloudRenderer.class.getSimpleName();

    private final String vertexShaderString =
            "uniform mat4 uMvpMatrix;\n" +
            "uniform vec4 uColor;\n" +
            "uniform float uPointSize;\n" +
            "attribute vec4 aPosition;\n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            "   vColor = uColor;\n" +
            "   gl_Position = uMvpMatrix * vec4(aPosition.xyz, 1.0);\n" +
            "   gl_PointSize = uPointSize;\n" +
            "}";

    private final String fragmentShaderString =
            "precision mediump float;\n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            "    gl_FragColor = vColor;\n" +
            "}";

    private int[] mVbo;
    private int mProgram;

    private float[] mViewMatrix = new float[16];
    private float[] mProjMatrix = new float[16];

    private int mNumPoints = 0;

    private PointCloud mPointCloud;

    public PointCloudRenderer() {
    }

    public void init() {
        mVbo = new int[1];
        GLES20.glGenBuffers(1, mVbo, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 1000 * 16, null, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        int vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vShader, vertexShaderString);
        GLES20.glCompileShader(vShader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(vShader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile vertex shader.");
            GLES20.glDeleteShader(vShader);
        }

        int fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fShader, fragmentShaderString);
        GLES20.glCompileShader(fShader);
        GLES20.glGetShaderiv(fShader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile fragment shader.");
            GLES20.glDeleteShader(fShader);
        }

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vShader);
        GLES20.glAttachShader(mProgram, fShader);
        GLES20.glLinkProgram(mProgram);
        int[] linked = new int[1];
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "Could not link program.");
        }
    }

    public void update(PointCloud pointCloud) {
        mPointCloud = pointCloud;

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo[0]);

        // TODO : size...
        mNumPoints = mPointCloud.getPoints().remaining() / 4;
        //GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mNumPoints * 16, null, GLES20.GL_DYNAMIC_DRAW);

        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, mNumPoints * 16, mPointCloud.getPoints());
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void draw() {
        float[] mvpMatrix = new float[16];
        Matrix.multiplyMM(mvpMatrix, 0, mProjMatrix, 0, mViewMatrix, 0);

        GLES20.glUseProgram(mProgram);

        int position = GLES20.glGetAttribLocation(mProgram, "aPosition");
        int color = GLES20.glGetUniformLocation(mProgram, "uColor");
        int mvp = GLES20.glGetUniformLocation(mProgram, "uMvpMatrix");
        int size = GLES20.glGetUniformLocation(mProgram, "uPointSize");

        GLES20.glEnableVertexAttribArray(position);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo[0]);
        GLES20.glVertexAttribPointer(position, 4, GLES20.GL_FLOAT, false, 16, 0);
        GLES20.glUniform4f(color, 31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f);
        GLES20.glUniformMatrix4fv(mvp, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(size, 5.0f);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mNumPoints);
        GLES20.glDisableVertexAttribArray(position);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void setProjectionMatrix(float[] projMatrix) {
        System.arraycopy(projMatrix, 0, mProjMatrix, 0, 16);
    }

    public void setViewMatrix(float[] viewMatrix) {
        System.arraycopy(viewMatrix, 0, mViewMatrix, 0, 16);
    }
}
