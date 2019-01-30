package com.example.arcore.chapter7.example7_3;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import com.google.ar.core.Frame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class CameraRenderer {
    private static final String TAG = CameraRenderer.class.getSimpleName();

    private final String vertexShaderString =
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "   vTexCoord = aTexCoord;\n" +
            "   gl_Position = aPosition;\n" +
            "}";

    private final String fragmentShaderString =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(sTexture, vTexCoord);\n" +
            "}";

    private static final float[] QUAD_COORDS =
            new float[] {-1.0f, -1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 0.0f};

    private static final float[] QUAD_TEXCOORDS =
            new float[] {0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f};

    private static final int COORDS_PER_VERTEX = 3;
    private static final int TEXCOORDS_PER_VERTEX = 2;

    private int[] mTextures;
    private FloatBuffer mVertices;
    private FloatBuffer mTexCoords;
    private FloatBuffer mTexCoordsTransformed;
    private int mProgram;

    public CameraRenderer() {
        mVertices = ByteBuffer.allocateDirect(QUAD_COORDS.length * Float.SIZE / 8).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(QUAD_COORDS);
        mVertices.position(0);

        mTexCoords = ByteBuffer.allocateDirect(QUAD_TEXCOORDS.length * Float.SIZE / 8).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTexCoords.put(QUAD_TEXCOORDS);
        mTexCoords.position(0);

        mTexCoordsTransformed = ByteBuffer.allocateDirect(QUAD_TEXCOORDS.length * Float.SIZE / 8).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public void init() {
        mTextures = new int[1];
        GLES20.glGenTextures(1, mTextures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextures[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

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

    public void draw() {
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextures[0]);

        GLES20.glUseProgram(mProgram);

        int position = GLES20.glGetAttribLocation(mProgram, "aPosition");
        int texcoord = GLES20.glGetAttribLocation(mProgram, "aTexCoord");

        GLES20.glVertexAttribPointer(position, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, mVertices);
        GLES20.glVertexAttribPointer(texcoord, TEXCOORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, mTexCoordsTransformed);

        GLES20.glEnableVertexAttribArray(position);
        GLES20.glEnableVertexAttribArray(texcoord);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(position);
        GLES20.glDisableVertexAttribArray(texcoord);
    }

    public int getTextureId() {
        return mTextures[0];
    }

    public void transformDisplayGeometry(Frame frame) {
        frame.transformDisplayUvCoords(mTexCoords, mTexCoordsTransformed);
    }
}
