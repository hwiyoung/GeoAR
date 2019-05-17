package com.example.arcore.chapter7.example7_3;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Line {

    private static final String TAG = Line.class.getSimpleName();

    public static final int RED = 0;
    public static final int GREEN = 1;
    public static final int BLUE = 2;
    public static final int ALPHA = 3;

    private final String vertexShaderString =
            "attribute vec3 aPosition;\n" +
            "attribute vec4 aColor;\n" +
            "uniform mat4 uMvpMatrix; \n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            "  vColor = aColor;\n" +
            "  gl_Position = uMvpMatrix * vec4(aPosition.x, aPosition.y, aPosition.z, 1.0);\n" +
            "}";

    private final String fragmentShaderString =
            "precision mediump float;\n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            "  gl_FragColor = vColor;\n" +
            "}";

    private boolean mIsInitialized = false;

    private int mProgram;

    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mProjMatrix = new float[16];

    private FloatBuffer mVertices;
    private FloatBuffer mColors;
    private ShortBuffer mIndices;

    private float mLineWidth = 1.0f;

    public Line(float startX, float startY, float startZ, float endX, float endY, float endZ, int lineWidth, int color) {
        float[] vertices = new float[]{ startX, startY, startZ, endX, endY, endZ };

        float r  = Color.red(color) / 255.f;
        float g = Color.green(color) / 255.f;
        float b = Color.blue(color) / 255.f;
        float a = Color.alpha(color) / 255.f;

        float[] colors = new float[]{ r, g, b, a, r, g, b, a };

        short[] indices = new short[] { 0, 1 };

        mVertices = ByteBuffer.allocateDirect(vertices.length * Float.SIZE / 8).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(vertices);
        mVertices.position(0);

        mColors = ByteBuffer.allocateDirect(colors.length * Float.SIZE / 8).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mColors.put(colors);
        mColors.position(0);

        mIndices = ByteBuffer.allocateDirect(indices.length * Short.SIZE / 8).order(ByteOrder.nativeOrder()).asShortBuffer();
        mIndices.put(indices);
        mIndices.position(0);

        mLineWidth = (float) lineWidth;
    }

    public void init() {
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

        mIsInitialized = true;
    }

    public void draw() {
        GLES20.glUseProgram(mProgram);

        int position = GLES20.glGetAttribLocation(mProgram, "aPosition");
        int color = GLES20.glGetAttribLocation(mProgram, "aColor");
        int mvp = GLES20.glGetUniformLocation(mProgram, "uMvpMatrix");

        float[] mvMatrix = new float[16];
        float[] mvpMatrix = new float[16];
        Matrix.multiplyMM(mvMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mProjMatrix, 0, mvMatrix, 0);

        GLES20.glUniformMatrix4fv(mvp, 1, false, mvpMatrix, 0);

        GLES20.glEnableVertexAttribArray(position);
        GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false, 4 * 3, mVertices);

        GLES20.glEnableVertexAttribArray(color);
        GLES20.glVertexAttribPointer(color, 4, GLES20.GL_FLOAT, false, 4 * 4, mColors);

        GLES20.glLineWidth(mLineWidth);
        GLES20.glDrawElements(GLES20.GL_LINES, mIndices.capacity(), GLES20.GL_UNSIGNED_SHORT, mIndices);
        GLES20.glLineWidth(1.0f);

        GLES20.glDisableVertexAttribArray(position);
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public void setModelMatrix(float[] modelMatrix) {
        System.arraycopy(modelMatrix, 0, mModelMatrix, 0, 16);
    }

    public void setProjectionMatrix(float[] projMatrix) {
        System.arraycopy(projMatrix, 0, mProjMatrix, 0, 16);
    }

    public void setViewMatrix(float[] viewMatrix) {
        System.arraycopy(viewMatrix, 0, mViewMatrix, 0, 16);
    }
}

