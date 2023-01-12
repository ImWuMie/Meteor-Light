/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.mixininterface.IMatrix4f;
import meteordevelopment.meteorclient.renderer.*;
import meteordevelopment.meteorclient.renderer.text.TTFFontRender;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.utils.misc.MeteorIdentifier;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import org.lwjgl.opengl.GL11;

import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class RenderUtils {
    public static Vec3d center;
    public RenderSystem renderSystem = new RenderSystem();
    public Renderer2D render2d = Renderer2D.COLOR;
    public MeteorRender glRender = new MeteorRender(RenderSystem.renderThreadTesselator());
    public Renderer3D render3d = new Renderer3D();
    public MatrixStack matrices = RenderSystem.getModelViewStack();
    public static RenderUtils instance = new RenderUtils();

    public boolean isMouseHoveringRect(float x, float y, int width, int height, int mouseX, int mouseY){
        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
    }

    public boolean isMouseHoveringRect(float x, float y, int width, int height, double mouseX, double mouseY){
        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
    }

    public Identifier getSkinByName(String name) {
        if (mc.getNetworkHandler() == null) return null;
        for (String part : name.split("(§.)|[^\\w]")) {
            if (part.isBlank()) continue;
            PlayerListEntry p = mc.getNetworkHandler().getPlayerListEntry(part);
            if (p != null) {
                return p.getSkinTexture();
            }
        }
        return null;
    }

    // Items
    public void drawItem(ItemStack itemStack, int x, int y, double scale, boolean overlay) {
        //RenderSystem.disableDepthTest();
        MatrixStack matrices = RenderSystem.getModelViewStack();

        matrices.push();
        matrices.scale((float) scale, (float) scale, 1);

        mc.getItemRenderer().renderGuiItemIcon(itemStack, (int) (x / scale), (int) (y / scale));
        if (overlay) mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, (int) (x / scale), (int) (y / scale), null);

        matrices.pop();
        //RenderSystem.enableDepthTest();
    }

    public void drawItem(ItemStack itemStack, int x, int y, boolean overlay) {
        drawItem(itemStack, x, y, 1, overlay);
    }

    public static void updateScreenCenter() {
        MinecraftClient mc = MinecraftClient.getInstance();

        Vec3d pos = new Vec3d(0, 0, 1);

        if (mc.options.getBobView().getValue()) {
            MatrixStack bobViewMatrices = new MatrixStack();

            bobView(bobViewMatrices);
            bobViewMatrices.peek().getPositionMatrix().invert();

            pos = ((IMatrix4f) (Object) bobViewMatrices.peek().getPositionMatrix()).mul(pos);
        }

        center = new Vec3d(pos.x, -pos.y, pos.z)
            .rotateX(-(float) Math.toRadians(mc.gameRenderer.getCamera().getPitch()))
            .rotateY(-(float) Math.toRadians(mc.gameRenderer.getCamera().getYaw()))
            .add(mc.gameRenderer.getCamera().getPos());
    }

    private static void bobView(MatrixStack matrices) {
        Entity cameraEntity = MinecraftClient.getInstance().getCameraEntity();

        if (cameraEntity instanceof PlayerEntity playerEntity) {
            float f = MinecraftClient.getInstance().getTickDelta();
            float g = playerEntity.horizontalSpeed - playerEntity.prevHorizontalSpeed;
            float h = -(playerEntity.horizontalSpeed + g * f);
            float i = MathHelper.lerp(f, playerEntity.prevStrideDistance, playerEntity.strideDistance);

            matrices.translate(-(MathHelper.sin(h * 3.1415927f) * i * 0.5), -(-Math.abs(MathHelper.cos(h * 3.1415927f) * i)), 0);
            matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(MathHelper.sin(h * 3.1415927f) * i * 3));
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(Math.abs(MathHelper.cos(h * 3.1415927f - 0.2f) * i) * 5));
        }
    }

    public Color getColorForString(String code, int alpha) {
        if (code.equals("0")) {
            return new Color(0, 0, 0, alpha);
        }
        if (code.equals("1")) {
            return new Color(0, 0, 170, alpha);
        }
        if (code.equals("2")) {
            return new Color(0, 170, 0, alpha);
        }
        if (code.equals("3")) {
            return new Color(0, 170, 170, alpha);
        }
        if (code.equals("4")) {
            return new Color(170, 0, 0, alpha);
        }
        if (code.equals("5")) {
            return new Color(170, 0, 170, alpha);
        }
        if (code.equals("6")) {
            return new Color(255, 170, 0, alpha);
        }
        if (code.equals("7")) {
            return new Color(170, 170, 170, alpha);
        }
        if (code.equals("8")) {
            return new Color(85, 85, 85, alpha);
        }
        if (code.equals("9")) {
            return new Color(85, 85, 255, alpha);
        }
        if (code.equals("a")) {
            return new Color(85, 255, 85, alpha);
        }
        if (code.equals("b")) {
            return new Color(85, 255, 255, alpha);
        }
        if (code.equals("c")) {
            return new Color(255, 85, 85, alpha);
        }
        if (code.equals("d")) {
            return new Color(255, 85, 255, alpha);
        }
        if (code.equals("e")) {
            return new Color(255, 255, 85, alpha);
        }
        return new Color(255, 255, 255, alpha);
    }

    public void drawHead(PlayerEntity entity, double x, double y, double width, double height) {
        Identifier headTexture = getSkinTexture(entity);

        RenderSystem.setShaderTexture(0,headTexture);
        GL.textureParam(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL.textureParam(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        DrawableHelper.drawTexture(RenderSystem.getModelViewStack(), (int) x,(int) y,0,0,(int) width,(int) height,(int) width,(int) height);
    }

    public void drawImage(MeteorIdentifier image, double x, double y, double width, double height) {
        RenderSystem.setShaderTexture(0,image);
        DrawableHelper.drawTexture(RenderSystem.getModelViewStack(), (int) x,(int) y,0,0,(int) width,(int) height,(int) width,(int) height);
    }

    public Identifier getSkinTexture(PlayerEntity entity) {
        return DefaultSkinHelper.getTexture(entity.getUuid());
    }

    public void drawEntity(LivingEntity entity,int x, int y, int size) {
        float yaw = entity.getYaw();
        float pitch = entity.getPitch();
        InventoryScreen.drawEntity(x,y,size,-yaw,-pitch,entity);
    }

    public double getStringWidth(String text,double fontScale) {
        double object;
        Fonts.RENDERER.begin(fontScale,false,true);
        object = Fonts.RENDERER.getWidth(text,false);
        Fonts.RENDERER.end();
        return object;
    }

    public double getStringWidth(String text) {
        return TextRenderer.get().getWidth(text);
    }

    public double getStringWidth(String text,boolean shadow,double fontScale) {
        double object;
        Fonts.RENDERER.begin(fontScale,false,false);
        object = Fonts.RENDERER.getWidth(text,shadow);
        Fonts.RENDERER.end();
        return object;
    }

    public double getStringWidth(TTFFontRender font, String text, boolean shadow) {
        double object;
        object = font.getWidth(text,shadow);
        return object;
    }

    public double getStringWidth(TTFFontRender font, String text, boolean shadow,double fontScale) {
        double object;
        object = font.getWidth(text,shadow,fontScale);
        return object;
    }

    public double getStringHeight(boolean shadow,double fontScale) {
        double object;
        Fonts.RENDERER.begin(fontScale,false,false);
        object = Fonts.RENDERER.getHeight(shadow);
        Fonts.RENDERER.end();
        return object;
    }

    public double getStringHeight(TTFFontRender font,boolean shadow) {
        double object;
        object = font.getHeight(shadow);
        return object;
    }

    public double getStringHeight(TTFFontRender font,boolean shadow,double fontScale) {
        double object;
        object = font.getHeight(shadow,fontScale);
        return object;
    }

    public double getStringHeight(boolean shadow) {
        return TextRenderer.get().getHeight(shadow);
    }

    public void drawRect(double x, double y, double width, double height, Color color) {
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(x, y, width, height, color);
        Renderer2D.COLOR.render(null);
    }

    public void drawRect(double x, double y, double width, double height, int color) {
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(x, y, width, height, new Color(color));
        Renderer2D.COLOR.render(null);
    }

    public void drawLine(double x1, double y1, double x2, double y2, Color color) {
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.line(x1, y1, x2, y2, color);
        Renderer2D.COLOR.render(null);
    }

    public void drawLine(double x1, double y1, double x2, double y2, int color) {
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.line(x1, y1, x2, y2, new Color(color));
        Renderer2D.COLOR.render(null);
    }

    public double getAnimationState(double animation, double finalState, double speed) {
        float add = (float) (0.01 * speed);
        if (animation < finalState) {
            if (animation + add < finalState)
                animation += add;
            else
                animation = finalState;
        } else {
            if (animation - add > finalState)
                animation -= add;
            else
                animation = finalState;
        }
        return animation;
    }

    public double getAnimationState2(double animation, double finalState, double speed) {
        float add = (float) (0.01 * speed);
        if (animation < finalState) {
            animation = finalState;
        } else {
            if (animation - add > finalState)
                animation -= add;
            else
                animation = finalState;
        }
        return animation;
    }
}

