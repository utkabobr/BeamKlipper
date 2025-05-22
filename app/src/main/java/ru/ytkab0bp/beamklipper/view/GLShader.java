package ru.ytkab0bp.beamklipper.view;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_TRUE;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform2f;
import static android.opengl.GLES20.glUniform3f;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUseProgram;

import android.graphics.Color;

public class GLShader {
    private int program, vertex, fragment;
    private GLShadersManager manager;

    GLShader(GLShadersManager manager, String vertex, String fragment) {
        this.manager = manager;
        this.vertex = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(this.vertex, vertex);
        glCompileShader(this.vertex);

        int[] params = new int[1];
        glGetShaderiv(this.vertex, GL_COMPILE_STATUS, params, 0);
        if (params[0] != GL_TRUE) {
            throw new IllegalArgumentException("Failed to compile vertex shader: " + glGetShaderInfoLog(this.vertex));
        }

        this.fragment = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(this.fragment, fragment);
        glCompileShader(this.fragment);

        params = new int[1];
        glGetShaderiv(this.fragment, GL_COMPILE_STATUS, params, 0);
        if (params[0] != GL_TRUE) {
            throw new IllegalArgumentException("Failed to compile fragment shader: " + glGetShaderInfoLog(this.fragment));
        }

        program = glCreateProgram();
        glAttachShader(program, this.vertex);
        glAttachShader(program, this.fragment);
        glLinkProgram(program);

        glGetShaderiv(this.fragment, GL_LINK_STATUS, params, 0);
        if (params[0] != GL_TRUE) {
            throw new IllegalArgumentException("Failed to link program: " + glGetProgramInfoLog(program));
        }
    }

    public int getAttribLocation(String name) {
        return glGetAttribLocation(program, name);
    }

    public void uniform1i(String name, int v) {
        int loc = glGetUniformLocation(program, name);
        if (loc != -1) glUniform1i(loc, v);
    }

    public void uniform1f(String name, float v) {
        int loc = glGetUniformLocation(program, name);
        if (loc != -1) glUniform1f(loc, v);
    }

    public void uniform2f(String name, float a, float b) {
        int loc = glGetUniformLocation(program, name);
        if (loc != -1) glUniform2f(loc, a, b);
    }

    public void uniform3f(String name, float a, float b, float c) {
        int loc = glGetUniformLocation(program, name);
        if (loc != -1) glUniform3f(loc, a, b, c);
    }

    public void uniformColor4f(String name, int color) {
        int loc = glGetUniformLocation(program, name);
        if (loc != -1) glUniform4f(loc, Color.red(color) / (float) 0xFF, Color.green(color) / (float) 0xFF, Color.blue(color) / (float) 0xFF, Color.alpha(color) / (float) 0xFF);
    }

    public void uniform4f(String name, float a, float b, float c, float d) {
        int loc = glGetUniformLocation(program, name);
        if (loc != -1) glUniform4f(loc, a, b, c, d);
    }

    public void startUsing() {
        manager.shaderStack.push(this);
        glUseProgram(program);
    }

    public void stopUsing() {
        manager.shaderStack.remove(this);
        if (!manager.shaderStack.isEmpty()) {
            glUseProgram(manager.shaderStack.peek().program);
        } else {
            glUseProgram(0);
        }
    }

    public void release() {
        glDeleteProgram(program);
        glDeleteShader(vertex);
        glDeleteShader(fragment);
    }
}