package ru.ytkab0bp.beamklipper.view;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import ru.ytkab0bp.beamklipper.KlipperApp;

public class GLShadersManager {
    public final static String KEY_INTRO = "beam_intro";

    private Map<String, GLShader> shaders = new HashMap<>();
    Stack<GLShader> shaderStack = new Stack<>();

    private static String read(InputStream in) throws IOException {
        byte[] buffer = new byte[10240]; int c;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while ((c = in.read(buffer)) != -1) {
            bos.write(buffer, 0, c);
        }
        in.close();
        bos.close();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    public GLShader get(String key) {
        GLShader shader = shaders.get(key);
        if (shader == null) {
            int tries = 0;
            while (tries <= 30) {
                try {
                    shader = new GLShader(this, read(KlipperApp.INSTANCE.getAssets().open("shaders/" + key + ".vs")), read(KlipperApp.INSTANCE.getAssets().open("shaders/" + key + ".fs")));
                    break;
                } catch (Exception e) {
                    tries++;
                }
            }
            shaders.put(key, shader);
        }
        return shader;
    }

    public GLShader getCurrent() {
        return shaderStack.isEmpty() ? null : shaderStack.peek();
    }

    public void release() {
        for (GLShader shader : shaders.values()) {
            shader.release();
        }
        shaders.clear();
        shaderStack.clear();
    }
}