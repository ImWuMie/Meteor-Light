/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.gui.renderer.packer.TextureRegion;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.notifications.DrawUtils;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

public class Renderer2D {
    public static Renderer2D COLOR;
    public static Renderer2D TEXTURE;

    public final Mesh triangles;
    public final Mesh lines;

    public Renderer2D(boolean texture) {
        triangles = new ShaderMesh(
            texture ? Shaders.POS_TEX_COLOR : Shaders.POS_COLOR,
            DrawMode.Triangles,
            texture ? new Mesh.Attrib[]{Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Color} : new Mesh.Attrib[]{Mesh.Attrib.Vec2, Mesh.Attrib.Color}
        );

        lines = new ShaderMesh(Shaders.POS_COLOR, DrawMode.Lines, Mesh.Attrib.Vec2, Mesh.Attrib.Color);
    }

    @PreInit(dependencies = Shaders.class)
    public static void init() {
        DrawUtils.init();
        COLOR = new Renderer2D(false);
        TEXTURE = new Renderer2D(true);
    }

    public void setAlpha(double alpha) {
        triangles.alpha = alpha;
    }

    public void begin() {
        triangles.begin();
        lines.begin();
    }

    public void end() {
        triangles.end();
        lines.end();
    }

    public void render(MatrixStack matrices) {
        triangles.render(matrices);
        lines.render(matrices);
    }

    // Lines

    public void line(double x1, double y1, double x2, double y2, Color color) {
        lines.line(
            lines.vec2(x1, y1).color(color).next(),
            lines.vec2(x2, y2).color(color).next()
        );
    }

    public void boxLines(double x, double y, double width, double height, Color color) {
        int i1 = lines.vec2(x, y).color(color).next();
        int i2 = lines.vec2(x, y + height).color(color).next();
        int i3 = lines.vec2(x + width, y + height).color(color).next();
        int i4 = lines.vec2(x + width, y).color(color).next();

        lines.line(i1, i2);
        lines.line(i2, i3);
        lines.line(i3, i4);
        lines.line(i4, i1);
    }

    // Quads

    public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft) {
        triangles.quad(
            triangles.vec2(x, y).color(cTopLeft).next(),
            triangles.vec2(x, y + height).color(cBottomLeft).next(),
            triangles.vec2(x + width, y + height).color(cBottomRight).next(),
            triangles.vec2(x + width, y).color(cTopRight).next()
        );
    }

    public void quad(double x, double y, double width, double height, Color color) {
        quad(x, y, width, height, color, color, color, color);
    }

    public void roundedQuad(double x, double y, double width, double height,float radius, Color color) {
        x += (float) (radius / 2.0f + 0.5);
        y += (float) (radius / 2.0f + 0.5);
        width -= (float) (radius / 2.0f + 0.5);
        height -= (float) (radius / 2.0f + 0.5);
        quad(x, y, width, height, color);
        circle(width - radius / 2.0f, y + radius / 2.0f, radius, color);
        circle(x + radius / 2.0f, height - radius / 2.0f, radius, color);
        circle(x + radius / 2.0f, y + radius / 2.0f, radius, color);
        circle(width - radius / 2.0f, height - radius / 2.0f, radius, color);
        quad((x - radius / 2.0f - 0.5f), (y + radius / 2.0f), width, (height - radius / 2.0f), color);
        quad(x,(y + radius / 2.0f), (width + radius / 2.0f + 0.5f), (height - radius / 2.0f), color);
        quad((x + radius / 2.0f), (y - radius / 2.0f - 0.5f), (width - radius / 2.0f), (height - radius / 2.0f), color);
        quad((x + radius / 2.0f), y, (width - radius / 2.0f), (height + radius / 2.0f + 0.5f), color);
    }

    public void circle(double x,double y, float radius,Color fillColor) {
        arc(x, y, 0.0f, 360.0f, radius, fillColor);
    }

    public void arc(double x, double y,double start,double end,float radius,Color color) {
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        arcEllipse(x, y, start, end, radius, radius, color);
    }

    public void arcEllipse(double x,double y, double start, double end, float w,float h,Color color) {
        double temp;
        if (start > end) {
            temp = end;
            end = start;
            start = temp;
        }
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.blendFunc(770, 771);
            GL11.glLineWidth(2.0f);
            for (double i = end; i >= start; i -= 4.0) {
                final float ldx = (float) Math.cos(i * 3.141592653589793 / 180.0) * w * 1.001f;
                final float ldy = (float) Math.sin(i * 3.141592653589793 / 180.0) * h * 1.001f;
                line(x,y,x + ldx, y + ldy,color);
            }
        for (double i = end; i >= start; i -= 4.0) {
            final float ldx = (float) Math.cos(i * 3.141592653589793 / 180.0) * w;
            final float ldy = (float) Math.sin(i * 3.141592653589793 / 180.0) * h;
            line(x,y,x + ldx, y + ldy,color);
        }
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    // Textured quads

    public void texQuad(double x, double y, double width, double height, Color color) {
        triangles.quad(
            triangles.vec2(x, y).vec2(0, 0).color(color).next(),
            triangles.vec2(x, y + height).vec2(0, 1).color(color).next(),
            triangles.vec2(x + width, y + height).vec2(1, 1).color(color).next(),
            triangles.vec2(x + width, y).vec2(1, 0).color(color).next()
        );
    }

    public void texQuad(double x, double y, double width, double height, TextureRegion texture, Color color) {
        triangles.quad(
            triangles.vec2(x, y).vec2(texture.x1, texture.y1).color(color).next(),
            triangles.vec2(x, y + height).vec2(texture.x1, texture.y2).color(color).next(),
            triangles.vec2(x + width, y + height).vec2(texture.x2, texture.y2).color(color).next(),
            triangles.vec2(x + width, y).vec2(texture.x2, texture.y1).color(color).next()
        );
    }

    public void texQuad(double x, double y, double width, double height, double rotation, double texX1, double texY1, double texX2, double texY2, Color color) {
        double rad = Math.toRadians(rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double oX = x + width / 2;
        double oY = y + height / 2;

        double _x1 = ((x - oX) * cos) - ((y - oY) * sin) + oX;
        double _y1 = ((y - oY) * cos) + ((x - oX) * sin) + oY;
        int i1 = triangles.vec2(_x1, _y1).vec2(texX1, texY1).color(color).next();

        double _x2 = ((x - oX) * cos) - ((y + height - oY) * sin) + oX;
        double _y2 = ((y + height - oY) * cos) + ((x - oX) * sin) + oY;
        int i2 = triangles.vec2(_x2, _y2).vec2(texX1, texY2).color(color).next();

        double _x3 = ((x + width - oX) * cos) - ((y + height - oY) * sin) + oX;
        double _y3 = ((y + height - oY) * cos) + ((x + width - oX) * sin) + oY;
        int i3 = triangles.vec2(_x3, _y3).vec2(texX2, texY2).color(color).next();

        double _x4 = ((x + width - oX) * cos) - ((y - oY) * sin) + oX;
        double _y4 = ((y - oY) * cos) + ((x + width - oX) * sin) + oY;
        int i4 = triangles.vec2(_x4, _y4).vec2(texX2, texY1).color(color).next();

        triangles.quad(i1, i2, i3, i4);
    }

    public void texQuad(double x, double y, double width, double height, double rotation, TextureRegion region, Color color) {
        texQuad(x, y, width, height, rotation, region.x1, region.y1, region.x2, region.y2, color);
    }
}
