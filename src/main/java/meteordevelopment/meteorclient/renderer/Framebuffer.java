/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.opengl.GL32C.*;

public class Framebuffer {
    private int id;
    public int texture;

    public Framebuffer() {
        draw(mc.getWindow().getFramebufferWidth(),mc.getWindow().getFramebufferHeight());
    }

    private void draw(int width,int height) {
        id = GL.genFramebuffer();
        bind();

        texture = GL.genTexture();
        GL.bindTexture(texture);
        GL.defaultPixelStore();

        GL.textureParam(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GL.textureParam(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        GL.textureParam(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        GL.textureParam(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        GL.textureImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, null);
        GL.framebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);

        unbind();
    }

    public void bind() {
        GL.bindFramebuffer(id);
    }

    public void unbind() {
        mc.getFramebuffer().beginWrite(false);
    }

    public void resize(int width,int height) {
        GL.deleteFramebuffer(id);
        GL.deleteTexture(texture);

        draw(width,height);
    }
}
