/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import kotlin.internal.ProgressionUtilKt;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.pathfinding.CustomPathFinder;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.Dir;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Renderer3D {
    public final Mesh lines = new ShaderMesh(Shaders.POS_COLOR, DrawMode.Lines, Mesh.Attrib.Vec3, Mesh.Attrib.Color);
    public final Mesh triangles = new ShaderMesh(Shaders.POS_COLOR, DrawMode.Triangles, Mesh.Attrib.Vec3, Mesh.Attrib.Color);

    public void begin() {
        lines.begin();
        triangles.begin();
    }

    public void end() {
        lines.end();
        triangles.end();
    }

    public void render(MatrixStack matrices) {
        lines.render(matrices);
        triangles.render(matrices);
    }

    // Lines

    public void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color1, Color color2) {
        lines.line(
            lines.vec3(x1, y1, z1).color(color1).next(),
            lines.vec3(x2, y2, z2).color(color2).next()
        );
    }

    public void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        line(x1, y1, z1, x2, y2, z2, color, color);
    }

    @SuppressWarnings("Duplicates")
    public void boxLines(double x1, double y1, double z1, double x2, double y2, double z2, Color color, int excludeDir) {
        int blb = lines.vec3(x1, y1, z1).color(color).next();
        int blf = lines.vec3(x1, y1, z2).color(color).next();
        int brb = lines.vec3(x2, y1, z1).color(color).next();
        int brf = lines.vec3(x2, y1, z2).color(color).next();
        int tlb = lines.vec3(x1, y2, z1).color(color).next();
        int tlf = lines.vec3(x1, y2, z2).color(color).next();
        int trb = lines.vec3(x2, y2, z1).color(color).next();
        int trf = lines.vec3(x2, y2, z2).color(color).next();

        if (excludeDir == 0) {
            // Bottom to top
            lines.line(blb, tlb);
            lines.line(blf, tlf);
            lines.line(brb, trb);
            lines.line(brf, trf);

            // Bottom loop
            lines.line(blb, blf);
            lines.line(brb, brf);
            lines.line(blb, brb);
            lines.line(blf, brf);

            // Top loop
            lines.line(tlb, tlf);
            lines.line(trb, trf);
            lines.line(tlb, trb);
            lines.line(tlf, trf);
        }
        else {
            // Bottom to top
            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.NORTH)) lines.line(blb, tlb);
            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.SOUTH)) lines.line(blf, tlf);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.NORTH)) lines.line(brb, trb);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.SOUTH)) lines.line(brf, trf);

            // Bottom loop
            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.DOWN)) lines.line(blb, blf);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.DOWN)) lines.line(brb, brf);
            if (Dir.isNot(excludeDir, Dir.NORTH) && Dir.isNot(excludeDir, Dir.DOWN)) lines.line(blb, brb);
            if (Dir.isNot(excludeDir, Dir.SOUTH) && Dir.isNot(excludeDir, Dir.DOWN)) lines.line(blf, brf);

            // Top loop
            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.UP)) lines.line(tlb, tlf);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.UP)) lines.line(trb, trf);
            if (Dir.isNot(excludeDir, Dir.NORTH) && Dir.isNot(excludeDir, Dir.UP)) lines.line(tlb, trb);
            if (Dir.isNot(excludeDir, Dir.SOUTH) && Dir.isNot(excludeDir, Dir.UP)) lines.line(tlf, trf);
        }

        lines.growIfNeeded();
    }

    public void blockLines(int x, int y, int z, Color color, int excludeDir) {
        boxLines(x, y, z, x + 1, y + 1, z + 1, color, excludeDir);
    }

    // Quads

    public void quad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        triangles.quad(
            triangles.vec3(x1, y1, z1).color(bottomLeft).next(),
            triangles.vec3(x2, y2, z2).color(topLeft).next(),
            triangles.vec3(x3, y3, z3).color(topRight).next(),
            triangles.vec3(x4, y4, z4).color(bottomRight).next()
        );
    }

    public void quad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color color) {
        quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, color, color, color, color);
    }

    public void quadVertical(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        quad(x1, y1, z1, x1, y2, z1, x2, y2, z2, x2, y1, z2, color);
    }

    public void quadHorizontal(double x1, double y, double z1, double x2, double z2, Color color) {
        quad(x1, y, z1, x1, y, z2, x2, y, z2, x2, y, z1, color);
    }

    public void gradientQuadVertical(double x1, double y1, double z1, double x2, double y2, double z2, Color topColor, Color bottomColor) {
        quad(x1, y1, z1, x1, y2, z1, x2, y2, z2, x2, y1, z2, topColor, topColor, bottomColor, bottomColor);
    }

    // Sides

    @SuppressWarnings("Duplicates")
    public void side(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color sideColor, Color lineColor, ShapeMode mode) {
        if (mode.lines()) {
            int i1 = lines.vec3(x1, y1, z1).color(lineColor).next();
            int i2 = lines.vec3(x2, y2, z2).color(lineColor).next();
            int i3 = lines.vec3(x3, y3, z3).color(lineColor).next();
            int i4 = lines.vec3(x4, y4, z4).color(lineColor).next();

            lines.line(i1, i2);
            lines.line(i2, i3);
            lines.line(i3, i4);
            lines.line(i4, i1);
        }

        if (mode.sides()) {
            quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, sideColor);
        }
    }

    public void sideVertical(double x1, double y1, double z1, double x2, double y2, double z2, Color sideColor, Color lineColor, ShapeMode mode) {
        side(x1, y1, z1, x1, y2, z1, x2, y2, z2, x2, y1, z2, sideColor, lineColor, mode);
    }

    public void sideHorizontal(double x1, double y, double z1, double x2, double z2, Color sideColor, Color lineColor, ShapeMode mode) {
        side(x1, y, z1, x1, y, z2, x2, y, z2, x2, y, z1, sideColor, lineColor, mode);
    }

    // Boxes

    @SuppressWarnings("Duplicates")
    public void boxSides(double x1, double y1, double z1, double x2, double y2, double z2, Color color, int excludeDir) {
        int blb = triangles.vec3(x1, y1, z1).color(color).next();
        int blf = triangles.vec3(x1, y1, z2).color(color).next();
        int brb = triangles.vec3(x2, y1, z1).color(color).next();
        int brf = triangles.vec3(x2, y1, z2).color(color).next();
        int tlb = triangles.vec3(x1, y2, z1).color(color).next();
        int tlf = triangles.vec3(x1, y2, z2).color(color).next();
        int trb = triangles.vec3(x2, y2, z1).color(color).next();
        int trf = triangles.vec3(x2, y2, z2).color(color).next();

        if (excludeDir == 0) {
            // Bottom to top
            triangles.quad(blb, blf, tlf, tlb);
            triangles.quad(brb, trb, trf, brf);
            triangles.quad(blb, tlb, trb, brb);
            triangles.quad(blf, brf, trf, tlf);

            // Bottom
            triangles.quad(blb, brb, brf, blf);

            // Top
            triangles.quad(tlb, tlf, trf, trb);
        }
        else {
            // Bottom to top
            if (Dir.isNot(excludeDir, Dir.WEST)) triangles.quad(blb, blf, tlf, tlb);
            if (Dir.isNot(excludeDir, Dir.EAST)) triangles.quad(brb, trb, trf, brf);
            if (Dir.isNot(excludeDir, Dir.NORTH)) triangles.quad(blb, tlb, trb, brb);
            if (Dir.isNot(excludeDir, Dir.SOUTH)) triangles.quad(blf, brf, trf, tlf);

            // Bottom
            if (Dir.isNot(excludeDir, Dir.DOWN)) triangles.quad(blb, brb, brf, blf);

            // Top
            if (Dir.isNot(excludeDir, Dir.UP)) triangles.quad(tlb, tlf, trf, trb);
        }

        triangles.growIfNeeded();
    }

    public void blockSides(int x, int y, int z, Color color, int excludeDir) {
        boxSides(x, y, z, x + 1, y + 1, z + 1, color, excludeDir);
    }

    public void box(double x1, double y1, double z1, double x2, double y2, double z2, Color sideColor, Color lineColor, ShapeMode mode, int excludeDir) {
        if (mode.lines()) boxLines(x1, y1, z1, x2, y2, z2, lineColor, excludeDir);
        if (mode.sides()) boxSides(x1, y1, z1, x2, y2, z2, sideColor, excludeDir);
    }

    public void box(BlockPos pos, Color sideColor, Color lineColor, ShapeMode mode, int excludeDir) {
        if (mode.lines()) boxLines(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, lineColor, excludeDir);
        if (mode.sides()) boxSides(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, sideColor, excludeDir);
    }

    public void vecBox(Vec3 vec, Color sideColor, Color lineColor, ShapeMode mode, int excludeDir) {
        BlockPos pos = new BlockPos(vec.getX(),vec.getY(),vec.getZ());
        if (mode.lines()) boxLines(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1, lineColor, excludeDir);
        if (mode.sides()) boxSides(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1, sideColor, excludeDir);
    }

    public void vecBox(CustomPathFinder.Vec3 vec, Box bb, Color sideColor, Color lineColor, ShapeMode mode, int excludeDir) {
        BlockPos pos = new BlockPos(vec.getX(),vec.getY(),vec.getZ());
        if (mode.lines()) boxLines(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + (bb.maxX - bb.minX), pos.getY() + (bb.maxY - bb.minY), pos.getZ() + (bb.maxZ - bb.minZ), lineColor, excludeDir);
        if (mode.sides()) boxSides(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + (bb.maxX - bb.minX), pos.getY() + (bb.maxY - bb.minY), pos.getZ() + (bb.maxZ - bb.minZ), lineColor, excludeDir);
    }

    public void vecBox(Vec3d vec, Box bb, Color sideColor, Color lineColor, ShapeMode mode, int excludeDir) {
        BlockPos pos = new BlockPos(vec.getX(),vec.getY(),vec.getZ());
        if (mode.lines()) boxLines(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + (bb.maxX - bb.minX), pos.getY() + (bb.maxY - bb.minY), pos.getZ() + (bb.maxZ - bb.minZ), lineColor, excludeDir);
        if (mode.sides()) boxSides(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + (bb.maxX - bb.minX), pos.getY() + (bb.maxY - bb.minY), pos.getZ() + (bb.maxZ - bb.minZ), lineColor, excludeDir);
    }

    public void up2Dbox(Box box, Color sideColor, Color lineColor, ShapeMode mode,double y, int excludeDir) {
        if (mode.lines()) boxLines(box.minX, y-1, box.minZ, box.maxX, y, box.maxZ, lineColor, excludeDir);
        if (mode.sides()) boxSides(box.minX, y-1, box.minZ, box.maxX, y, box.maxZ, sideColor, excludeDir);
    }

    public void box(Box box, Color sideColor, Color lineColor, ShapeMode mode, int excludeDir) {
        if (mode.lines()) boxLines(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, lineColor, excludeDir);
        if (mode.sides()) boxSides(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sideColor, excludeDir);
    }

    public double easeInOutQuad(double x) {
        double percent;
        if (x < 0.5D) {
            percent = (double)2 * x * x;
        } else {
            percent = 1;
            double ii = (double)-2 * x + (double)2;
            byte i = 2;
            percent -= Math.pow(ii, i) / (double)2;
        }

        return percent;
    }

    public void Circle(Entity entity,Color lineColor, ShapeMode mode) {
        Box entitybb = entity.getBoundingBox();
        double maxY = entitybb.maxY + 1;
        double minY = entitybb.minY -1;
        double radius = ((entitybb.maxX - entitybb.minX) + (entitybb.maxZ - entitybb.minZ)) * 0.5f;
        int i = 5;

        double x1 = entity.lastRenderX + (entity.getX() - entity.lastRenderX) - Math.sin((double)i * 3.141592653589793D / (double)180.0F) * radius;
        double z1 = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) + Math.cos((double)i * 3.141592653589793D / (double)180.0F) * radius;
        double x2 = entity.lastRenderX + (entity.getX() - entity.lastRenderX) - Math.sin((double)(i - 5) * 3.141592653589793D / (double)180.0F) * radius;
        double z2 = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) + Math.cos((double)(i - 5) * 3.141592653589793D / (double)180.0F) * radius;


        if (mode.lines()) {
            line(x1, minY, z1, x2, maxY, z2, lineColor);
            line(x2, maxY, z2,x1, minY, z1 ,lineColor);
        }
    }



    public void drawFakeSigma(MatrixStack matrices,Entity entity,Color sideColor, Color lineColor, ShapeMode mode, int excludeDir) {
        int everyTime = 3000;

        int drawTime = (int) (System.currentTimeMillis() % everyTime);
        boolean drawMode = drawTime > (everyTime/2);
        double drawPercent = drawTime / (everyTime/2.0);
        if (drawMode) {drawPercent -= (double)1;} else {drawPercent = (double)1 - drawPercent;}
        drawPercent = easeInOutQuad(drawPercent);
        matrices.push();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Box entitybb = entity.getBoundingBox();
        double radius = ((entitybb.maxX - entitybb.minX) + (entitybb.maxZ - entitybb.minZ)) * 0.5f;
        double height = entitybb.maxY - entitybb.minY;
        double eased = height / (double)3 * (drawPercent > 0.5D ? (double)1 - drawPercent : drawPercent) * (double)(drawMode ? -1 : 1);
        int i = 5;
        int tr = ProgressionUtilKt.getProgressionLastElement(5,360,5);
        if ( i <= tr) {
            while(true) {
                double MPI = Math.PI;
                double x1 = entity.lastRenderX + (entity.getX() - entity.lastRenderX) - Math.sin((double)i * MPI / (double)180.0F) * radius;
                double z1 = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) + Math.cos((double)i * MPI / (double)180.0F) * radius;
                double x2 = entity.lastRenderX + (entity.getX() - entity.lastRenderX) - Math.sin((double)(i - 5) * MPI / (double)180.0F) * radius;
                double z2 = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) + Math.cos((double)(i - 5) * MPI / (double)180.0F) * radius;
                double y = entity.lastRenderY + (entity.getY() - entity.lastRenderY) + height * drawPercent;

                //Draw
                if (mode.lines()) line(x1, y + eased, z1, x2, y + eased, z2, lineColor);
                //End

                if (i == tr) {
                    break;
                }
                i += 5;
                }
            }

            RenderSystem.disableBlend();
            matrices.pop();
    }
}
