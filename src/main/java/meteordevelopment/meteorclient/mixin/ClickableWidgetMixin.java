/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.WindowResizedEvent;
import meteordevelopment.meteorclient.renderer.Framebuffer;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.PostProcessRenderer;
import meteordevelopment.meteorclient.renderer.Shader;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Blur;
import meteordevelopment.orbit.listeners.ConsumerListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClickableWidget.class)
public class ClickableWidgetMixin {
    @Shadow
    protected int width;

    @Shadow
    protected int height;

    @Inject(method = "render",at = @At("TAIL"))
        private void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    }

    @Inject(method = "<init>",at = @At("TAIL"))
    private void awa(int x, int y, int width, int height, Text message, CallbackInfo ci) {
    }

    private Shader shader;
    private Framebuffer fbo1, fbo2;
    private boolean enabled;
    private long fadeEndAt;

    public void blurstart() {
        MeteorClient.EVENT_BUS.subscribe(new ConsumerListener<>(WindowResizedEvent.class, event -> {
            if (fbo1 != null) {
                fbo1.resize(width,height);
                fbo2.resize(width,height);
            }
        }));
    }

    private boolean shouldRender() {
        return true;
    }

    public void renderBlur() {
        Blur blur = Modules.get().get(Blur.class);
        // Enable / disable with fading
        boolean shouldRender = shouldRender();
        long time = System.currentTimeMillis();

        if (enabled) {
            if (!shouldRender) {
                if (fadeEndAt == -1) fadeEndAt = System.currentTimeMillis() + blur.fadeTime.get();

                if (time >= fadeEndAt) {
                    enabled = false;
                    fadeEndAt = -1;
                }
            }
        }
        else if (shouldRender) {
            enabled = true;
            fadeEndAt = System.currentTimeMillis() + blur.fadeTime.get();
        }

        if (!enabled) return;

        // Initialize shader and framebuffer if running for the first time
        if (shader == null) {
            shader = new Shader("blur.vert", "blur.frag");
            fbo1 = new Framebuffer();
            fbo2 = new Framebuffer();
        }

        // Prepare stuff for rendering
        int sourceTexture = MinecraftClient.getInstance().getFramebuffer().getColorAttachment();

        shader.bind();
        shader.set("u_Size", MinecraftClient.getInstance().getWindow().getFramebufferWidth(), MinecraftClient.getInstance().getWindow().getFramebufferHeight());
        shader.set("u_Texture", 0);

        // Update progress
        double progress = 1;

        if (time < fadeEndAt) {
            if (shouldRender) progress = 1 - (fadeEndAt - time) / blur.fadeTime.get().doubleValue();
            else progress = (fadeEndAt - time) / blur.fadeTime.get().doubleValue();
        }
        else {
            fadeEndAt = -1;
        }

        // Render the blur
        shader.set("u_Radius", Math.floor(blur.radius.get() * progress));

        PostProcessRenderer.beginRender();

        fbo1.bind();
        GL.bindTexture(sourceTexture);
        shader.set("u_Direction", 1.0, 0.0);
        PostProcessRenderer.render();

        if (blur.mode.get() == Blur.Mode.Fancy) fbo2.bind();
        else fbo2.unbind();
        GL.bindTexture(fbo1.texture);
        shader.set("u_Direction", 0.0, 1.0);
        PostProcessRenderer.render();

        if (blur.mode.get() == Blur.Mode.Fancy) {
            fbo1.bind();
            GL.bindTexture(fbo2.texture);
            shader.set("u_Direction", 1.0, 0.0);
            PostProcessRenderer.render();

            fbo2.unbind();
            GL.bindTexture(fbo1.texture);
            shader.set("u_Direction", 0.0, 1.0);
            PostProcessRenderer.render();
        }

        PostProcessRenderer.endRender();
    }
}
