/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.WindowResizedEvent;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.ImageButton;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.ImageRender;
import meteordevelopment.meteorclient.renderer.Framebuffer;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.PostProcessRenderer;
import meteordevelopment.meteorclient.renderer.Shader;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Blur;
import meteordevelopment.meteorclient.utils.misc.MeteorIdentifier;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.listeners.ConsumerListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MainScreen extends Screen {
    public static final CubeMapRenderer PANORAMA_CUBE_MAP = new CubeMapRenderer(new Identifier("textures/gui/title/background/panorama"));
    private static final Identifier PANORAMA_OVERLAY = new Identifier("textures/gui/title/background/panorama_overlay.png");
    private final RotatingCubeMapRenderer backgroundRenderer;
    private long backgroundFadeStart;
    boolean doBackgroundFade = true;

    public MainScreen() {
        super(Text.translatable("narrator.screen.title"));
        this.backgroundRenderer = new RotatingCubeMapRenderer(PANORAMA_CUBE_MAP);
        blurstart();
    }

    public boolean shouldPause() {
        return false;
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    public static MeteorIdentifier IDSINGLEPLAYER, IDMULITPLAYER,IDEXIT,IDLANGUAGE,IDSETTINGS,IDLOGO;

    static {
        IDSINGLEPLAYER = new MeteorIdentifier("textures/mainmenu/singleplayer.png");
        IDMULITPLAYER = new MeteorIdentifier("textures/mainmenu/mulitplayer.png");
        IDSETTINGS = new MeteorIdentifier("textures/mainmenu/settings.png");
        IDLOGO = new MeteorIdentifier("textures/mainmenu/logo.png");
        IDLANGUAGE = new MeteorIdentifier("textures/mainmenu/language.png");
    }

    @Override
    public void renderBackground(MatrixStack matrices) {
        // bar start
        Color bgColor = new Color(117,117,125,190);
        Color lineColor = new Color(152,151,150,220);
        //left
        int leftBarWidth = 110;
        int leftBarHeight = this.height;
        int left_part1Y = 80;
        int left_part2Y = 270;
        //right
        int rightBarX = width - 110;
        int rightBarWidth = 110;
        int rightBarHeight = this.height;
        int right_part1Y = 270;

        fill(matrices,0,0,leftBarWidth,leftBarHeight,bgColor.toAWTColor().getRGB());
        fill(matrices,rightBarX,0,rightBarWidth,rightBarHeight,bgColor.toAWTColor().getRGB());
        fill(matrices,0,left_part1Y,leftBarWidth,2,lineColor.toAWTColor().getRGB());
        fill(matrices,0,left_part2Y,leftBarWidth,2,lineColor.toAWTColor().getRGB());
        fill(matrices,rightBarX,right_part1Y,rightBarWidth,2,lineColor.toAWTColor().getRGB());
        // bar end
        super.renderBackground(matrices);
    }

    protected void init() {
        //Logo
        addDrawableChild(new ImageRender( 11,35,87,24,IDLOGO,false));

        //Buttons
        // OldTitleMenu Button
        addDrawableChild(new ButtonWidget(width-100,height-25,95,20,Text.literal("OldTitleMenu"),button -> {
            client.setScreen(new TitleScreen());
        }));

        int leftBarButtonX = 30;
        addDrawableChild(new ImageButton(leftBarButtonX,100,50,50,IDSINGLEPLAYER,button -> {
            this.client.setScreen(new SelectWorldScreen(this));
        },false));
        addDrawableChild(new ImageButton(leftBarButtonX,200,50,50,IDMULITPLAYER,button -> {
            Screen screen = this.client.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this);
            this.client.setScreen(screen);
        },false));
        addDrawableChild(new ImageButton(leftBarButtonX,315,50,50,IDSETTINGS,button -> {
            this.client.setScreen(new OptionsScreen(this, this.client.options));
        },false));
        addDrawableChild(new ImageButton(leftBarButtonX,415,50,50,IDLANGUAGE,button -> {
            this.client.setScreen(new LanguageOptionsScreen(this, this.client.options, this.client.getLanguageManager()));
        },false));
        addDrawableChild(new ImageButton(leftBarButtonX,this.height-80,50,50,IDEXIT,button -> {
            this.client.scheduleStop();
        },false));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (this.backgroundFadeStart == 0L && doBackgroundFade) {
            this.backgroundFadeStart = Util.getMeasuringTimeMs();
        }

        float f = this.doBackgroundFade ? (float)(Util.getMeasuringTimeMs() - this.backgroundFadeStart) / 1000.0F : 1.0F;
        this.backgroundRenderer.render(delta, MathHelper.clamp(f, 0.0F, 1.0F));
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, PANORAMA_OVERLAY);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.doBackgroundFade ? (float)MathHelper.ceil(MathHelper.clamp(f, 0.0F, 1.0F)) : 1.0F);
        drawTexture(matrices, 0, 0, this.width, this.height, 0.0F, 0.0F, 16, 128, 16, 128);
        renderBlur();
        super.render(matrices, mouseX, mouseY, delta);
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

    @Override
    public Optional<Element> hoveredElement(double mouseX, double mouseY) {
        return super.hoveredElement(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void setInitialFocus(@Nullable Element element) {
        super.setInitialFocus(element);
    }

    @Override
    public void focusOn(@Nullable Element element) {
        super.focusOn(element);
    }

    @Override
    public boolean changeFocus(boolean lookForwards) {
        return super.changeFocus(lookForwards);
    }
}
