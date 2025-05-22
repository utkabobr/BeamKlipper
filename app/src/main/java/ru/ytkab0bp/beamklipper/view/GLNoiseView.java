package ru.ytkab0bp.beamklipper.view;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_SHORT;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;

import androidx.core.graphics.ColorUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ru.ytkab0bp.beamklipper.utils.ViewUtils;

public class GLNoiseView extends GLSurfaceView {
    private final static int COORDINATES_PER_VERTEX = 2;
    private final static float[] QUADRANT_COORDINATES = {
            -1f, -1f,
            1f, -1f,
            1f, 1f,
            -1f, 1f
    };
    private final static float[] TEXTURE_COORDINATES = {
            1, 0,
            0, 0,
            0, 1,
            1, 1
    };
    private final static short[] DRAW_ORDER = {
            0, 1, 2, 0, 2, 3
    };

    private FloatBuffer quadrantCoordinatesBuffer = (FloatBuffer) ByteBuffer.allocateDirect(QUADRANT_COORDINATES.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(QUADRANT_COORDINATES)
            .position(0);

    private FloatBuffer textureCoordinatesBuffer = (FloatBuffer) ByteBuffer.allocateDirect(TEXTURE_COORDINATES.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(TEXTURE_COORDINATES)
            .position(0);

    private ShortBuffer drawOrderBuffer = (ShortBuffer) ByteBuffer.allocateDirect(DRAW_ORDER.length * 4)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(DRAW_ORDER)
            .position(0);

    private GLShadersManager shadersManager = new GLShadersManager();

    private boolean created;
    private float time;
    private long lastDraw;

    public GLNoiseView(Context context) {
        super(context);

        setEGLContextClientVersion(3);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.RGBA_8888);
        setRenderer(new Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {}

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                if (created) {
                    onDestroy();
                }
                glViewport(0, 0, width, height);
                onCreate(width, height);
            }

            @Override
            public void onDrawFrame(GL10 gl) {
                if (!isAttachedToWindow()) {
                    return;
                }
                long dt = Math.min(System.currentTimeMillis() - lastDraw, 16);
                lastDraw = System.currentTimeMillis();
                time += dt / 1000f;

                glBindFramebuffer(GL_FRAMEBUFFER, 0);
                glClearColor(0, 0, 0, 0);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                GLShader shader = shadersManager.get(GLShadersManager.KEY_INTRO);
                shader.startUsing();
                int topColor = ViewUtils.resolveColor(getContext(), android.R.attr.colorAccent);
                int bottomColor = ViewUtils.resolveColor(getContext(), android.R.attr.windowBackground);
                bottomColor = ColorUtils.blendARGB(bottomColor, topColor, 0.5f);

                shader.uniformColor4f("top_color", topColor);
                shader.uniformColor4f("bottom_color", bottomColor);
                shader.uniform1f("progress", -0.4f);
                shader.uniform1f("time", time);
                drawTexture();
                shader.stopUsing();
            }
        });
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    private void drawTexture() {
        GLShader shader = shadersManager.getCurrent();
        int posHandle = shader.getAttribLocation("v_position");
        if (posHandle != -1) {
            glVertexAttribPointer(posHandle, COORDINATES_PER_VERTEX, GL_FLOAT, false, COORDINATES_PER_VERTEX * 4, quadrantCoordinatesBuffer);
            glEnableVertexAttribArray(posHandle);
        }
        int texHandle = shader.getAttribLocation("v_tex_coord");
        if (texHandle != -1) {
            glVertexAttribPointer(texHandle, COORDINATES_PER_VERTEX, GL_FLOAT, false, COORDINATES_PER_VERTEX * 4, textureCoordinatesBuffer);
            glEnableVertexAttribArray(texHandle);
        }

        glDrawElements(GL_TRIANGLES, DRAW_ORDER.length, GL_UNSIGNED_SHORT, drawOrderBuffer);
        if (posHandle != -1) {
            glDisableVertexAttribArray(posHandle);
        }
        if (texHandle != -1) {
            glDisableVertexAttribArray(texHandle);
        }
    }

    private void onCreate(int width, int height) {
        created = true;
    }

    private void onDestroy() {
        created = false;
        shadersManager.release();
    }
}