/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.WindowResizedEvent;
import meteordevelopment.meteorclient.renderer.Framebuffer;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.PostProcessRenderer;
import meteordevelopment.meteorclient.renderer.Shader;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Blur;
import meteordevelopment.meteorclient.utils.misc.MeteorIdentifier;
import meteordevelopment.orbit.listeners.ConsumerListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;

public abstract class RenderImage extends DrawableHelper implements Drawable, Element, Selectable {
    public MeteorIdentifier WIDGETS_TEXTURE;
    protected int width;
    protected int height;
    public int x;
    public int y;
    protected boolean hovered;
    public boolean active = true;
    public boolean visible = true;
    protected float alpha = 1.0F;
    private boolean focused;

    private boolean blur;
    private Shader shader;
    private Framebuffer fbo1, fbo2;
    private boolean enabled;
    private long fadeEndAt;

    public RenderImage(int x, int y, int imageWidth, int imageHeight, MeteorIdentifier image,boolean blur) {
        this.x = x / 2;
        this.y = y / 2;
        this.width = imageWidth / 2;
        this.height = imageHeight / 2;
        this.WIDGETS_TEXTURE = image;
        this.blur = blur;

        MeteorClient.EVENT_BUS.subscribe(new ConsumerListener<>(WindowResizedEvent.class, event -> {
            if (fbo1 != null) {
                fbo1.resize(width,height);
                fbo2.resize(width,height);
            }
        }));

    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (WIDGETS_TEXTURE == null) return;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        drawTexture(matrices, x, y, this.width, this.height, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
        if (blur) {
            renderBlur();
        }
    }


    public void onClick(double mouseX, double mouseY) {
    }

    public void onRelease(double mouseX, double mouseY) {
    }

    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
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

    private boolean shouldRender() {
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible) {
            if (this.isValidClickButton(button)) {
                boolean bl = this.clicked(mouseX, mouseY);
                if (bl) {
                    this.playDownSound(MinecraftClient.getInstance().getSoundManager());
                    this.onClick(mouseX, mouseY);
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isValidClickButton(button)) {
            this.onRelease(mouseX, mouseY);
            return true;
        } else {
            return false;
        }
    }

    protected boolean isValidClickButton(int button) {
        return button == 0;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isValidClickButton(button)) {
            this.onDrag(mouseX, mouseY, deltaX, deltaY);
            return true;
        } else {
            return false;
        }
    }

    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= (double)this.x && mouseY >= (double)this.y && mouseX < (double)(this.x + this.width) && mouseY < (double)(this.y + this.height);
    }

    public boolean isHovered() {
        return this.hovered || this.focused;
    }

    public boolean changeFocus(boolean lookForwards) {
        if (this.active && this.visible) {
            this.focused = !this.focused;
            this.onFocusedChanged(this.focused);
            return this.focused;
        } else {
            return false;
        }
    }

    protected void onFocusedChanged(boolean newFocused) {
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= (double)this.x && mouseY >= (double)this.y && mouseX < (double)(this.x + this.width) && mouseY < (double)(this.y + this.height);
    }

    public void renderTooltip(MatrixStack matrices, int mouseX, int mouseY) {
    }

    public void playDownSound(SoundManager soundManager) {
        soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void setImage(MeteorIdentifier id) {
        WIDGETS_TEXTURE = id;
    }

    public MeteorIdentifier getImage() {
        return WIDGETS_TEXTURE;
    }

    public boolean isFocused() {
        return this.focused;
    }

    public boolean isNarratable() {
        return this.visible && this.active;
    }

    protected void setFocused(boolean focused) {
        this.focused = focused;
    }

    public SelectionType getType() {
        if (this.focused) {
            return SelectionType.FOCUSED;
        } else {
            return this.hovered ? SelectionType.HOVERED : SelectionType.NONE;
        }
    }

}
