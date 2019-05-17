package com.example.arcore.chapter7.example7_3;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Sphere {

    private static final String TAG = Sphere.class.getSimpleName();

    private final int POINT_COUNT = 20;
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

    private float[] mColor = new float[]{0.0f, 0.0f, 0.0f, 1.0f};

    public Sphere(float radius, int color) {
        float[] vertices = new float[POINT_COUNT * POINT_COUNT * 3];
        for (int i = 0; i < POINT_COUNT; i++) {
            for (int j = 0; j < POINT_COUNT; j++) {
                float theta = i * (float) Math.PI / (POINT_COUNT - 1);
                float phi = j * 2 * (float) Math.PI / (POINT_COUNT - 1);
                float x = (float) (radius * Math.sin(theta) * Math.cos(phi));
                float y = (float) (radius * Math.cos(theta));
                float z = (float) -(radius * Math.sin(theta) * Math.sin(phi));
                int index = i * POINT_COUNT + j;
                vertices[3 * index] = x;
                vertices[3 * index + 1] = y;
                vertices[3 * index + 2] = z;
            }
        }

        mColor[RED] = Color.red(color) / 255.f;
        mColor[GREEN] = Color.green(color) / 255.f;
        mColor[BLUE] = Color.blue(color) / 255.f;
        mColor[ALPHA] = Color.alpha(color) / 255.f;

        float[] colors = new float[POINT_COUNT * POINT_COUNT * 4];
        for (int i = 0; i < POINT_COUNT ; i++) {
            for (int j = 0; j < POINT_COUNT; j++) {
                int index = i * POINT_COUNT + j;
                colors[4 * index + 0] = mColor[RED];
                colors[4 * index + 1] = mColor[GREEN];
                colors[4 * index + 2] = mColor[BLUE];
                colors[4 * index + 3] = mColor[ALPHA];
            }
        }

        int numIndices = 2 * (POINT_COUNT - 1) * POINT_COUNT;
        short[] indices = new short[numIndices];
        short index = 0;
        for (int i = 0; i < POINT_COUNT - 1; i++) {
            if ((i & 1) == 0) {
                for (int j = 0; j < POINT_COUNT; j++) {
                    indices[index++] = (short) (i * POINT_COUNT + j);
                    indices[index++] = (short) ((i + 1) * POINT_COUNT + j);
                }
            } else {
                for (int j = POINT_COUNT - 1; j >= 0; j--) {
                    indices[index++] = (short) ((i + 1) * POINT_COUNT + j);
                    indices[index++] = (short) (i * POINT_COUNT + j);
                }
            }
        }

        mVertices = ByteBuffer.allocateDirect(vertices.length * Float.SIZE / 8).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(vertices);
        mVertices.position(0);

        mColors = ByteBuffer.allocateDirect(colors.length * Float.SIZE / 8).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mColors.put(colors);
        mColors.position(0);

        mIndices = ByteBuffer.allocateDirect(indices.length * Short.SIZE / 8).order(ByteOrder.nativeOrder()).asShortBuffer();
        mIndices.put(indices);
        mIndices.position(0);
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

        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, mIndices.capacity(), GLES20.GL_UNSIGNED_SHORT, mIndices);

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
