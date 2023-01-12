/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;

import java.awt.*;

public class MeteorRender {
    private Color color = new Color(255,255,255,255);
    private float lineWidth = 1f;
    public MatrixStack matrices = RenderSystem.getModelViewStack();
    public Matrix4f matrix4f = matrices.peek().getPositionMatrix();
    private VertexFormat.DrawMode drawMode = VertexFormat.DrawMode.QUADS;
    private VertexFormat vertexFormatMode = VertexFormats.POSITION;
    private final Tessellator tessellator;
    private final BufferBuilder bufferBuilder;

    public MeteorRender(Tessellator tessellator) {
        this.tessellator = tessellator;
        if (tessellator == null) {
            this.bufferBuilder = RenderSystem.renderThreadTesselator().getBuffer();
        } else {
            this.bufferBuilder = tessellator.getBuffer();
        }
    }

    public void vertex2d(double x,double y) {
        bufferBuilder.vertex(matrix4f,(float) x,(float) y,0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
    }

    public void vertex2d(int x,int y) {
        vertex2d(x,(double) y);
    }

    public void vertex2d(float x,float y) {
        vertex2d(x,(double) y);
    }

    public void vertex3d(double x,double y,double z) {
        bufferBuilder.vertex(matrix4f,(float) x,(float) y,(float) z).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
    }

    public void vertex3d(int x,int y,int z) {
        vertex3d(x, (double) y,z);
    }

    public void vertex3d(float x,float y, float z) {
        vertex3d(x,(double) y,z);
    }

    public void pushMatrix() {
        RenderSystem.assertOnRenderThread();
        matrices.push();
    }

    public void popMatrix() {
        RenderSystem.assertOnRenderThread();
        matrices.pop();
    }

    public float getLineWidth() {
        RenderSystem.assertOnRenderThread();
        return this.lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        RenderSystem.assertOnRenderThread();
        this.lineWidth = lineWidth;
        RenderSystem.lineWidth(lineWidth);
    }

    public Color getColor() {
        RenderSystem.assertOnRenderThread();
        return color;
    }

    public void setColor(Color color) {
        RenderSystem.assertOnRenderThread();
        this.color = color;
    }

    public void setColor(double red,double green,double blue,double alpha) {
        RenderSystem.assertOnRenderThread();
        this.color = new Color((int) red, (int) green, (int) blue, (int) alpha);
    }

    public void setColor(int red,int green,int blue,int alpha) {
        setColor(red,(double) green,blue,alpha);
    }

    public void setColor(float red,float green,float blue,float alpha) {
        setColor(red,(double) green,blue,alpha);
    }

    public void color(double red,double green,double blue ,double alpha) {
        setColor(red, green,blue,alpha);
    }

    public void color(int red,int green,int blue ,int alpha) {
        setColor(red,(double) green,blue,alpha);
    }

    public void color(float red,float green,float blue ,float alpha) {
        setColor(red,(double) green,blue,alpha);
    }


    public void begin(VertexFormat.DrawMode mode,VertexFormat vertexFormats) {
        RenderSystem.assertOnRenderThread();
        this.drawMode = mode;
        this.vertexFormatMode = vertexFormats;
        RenderSystem.assertOnRenderThread();
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesShader);
        bufferBuilder.begin(drawMode, vertexFormatMode);
    }

    public void begin(VertexFormat.DrawMode mode) {
        RenderSystem.assertOnRenderThread();
        this.drawMode = mode;
    }

    public void begin(VertexFormat vertexFormats) {
        RenderSystem.assertOnRenderThread();
        this.vertexFormatMode = vertexFormats;
    }

    public void end() {
        RenderSystem.assertOnRenderThread();
        tessellator.draw();
        this.drawMode = VertexFormat.DrawMode.LINES;
        this.vertexFormatMode = VertexFormats.LINES;
    }

    public void translate(double x,double y,double z) {
        RenderSystem.assertOnRenderThread();
        matrices.translate(x,y,z);
    }

    public void translate(int x,int y,int z) {
        translate(x,(double) y,z);
    }

    public void translate(float x,float y,float z) {
        translate(x,(double) y,z);
    }

    public void rotate(double x,double y,double z) {
        RenderSystem.assertOnRenderThread();
        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion((float) x));
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion((float) y));
        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion((float) z));
    }

    public void rotate(int x,int y,int z) {
        rotate((double) x,y,z);
    }
    public void rotate(float x,float y,float z) {
        rotate((double) x,y,z);
    }
}
