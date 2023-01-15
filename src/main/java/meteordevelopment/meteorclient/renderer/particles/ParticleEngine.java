package meteordevelopment.meteorclient.renderer.particles;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.util.ByteBufferTypeAdapter;
import meteordevelopment.meteorclient.renderer.Framebuffer;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.utils.misc.Vec2;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;

public class ParticleEngine {

	public CopyOnWriteArrayList<Particle> particles = Lists.newCopyOnWriteArrayList();
	public float lastMouseX;
	public float lastMouseY;

	public void render(MatrixStack matrixStack, float mouseX, float mouseY, int displayWidth, int displayHeight){
        RenderSystem.enableBlend();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glColor4f(1,1,1,1);
        float xOffset = displayWidth/2-mouseX;
        float yOffset = displayHeight/2-mouseY;
		for(particles.size(); particles.size() < (int)(displayWidth/19.2f); particles.add(new Particle(displayWidth, displayHeight,new Random().nextFloat()*2 + 2, new Random().nextFloat()*5 + 5)));
		List<Particle> toremove = Lists.newArrayList();
		for(Particle p : particles){
			if(p.opacity < 32){
				p.opacity += 2;
			}
			if(p.opacity > 32){
				p.opacity = 32;
			}
			Color c = new Color((int)255, (int)255, (int)255, (int)p.opacity);
			drawBorderedCircle(p.x + Math.sin(p.ticks/2)*50 + -xOffset/5, (p.ticks*p.speed)*p.ticks/10 + -yOffset/5, p.radius*(p.opacity/32), c.getRGB(), c.getRGB());
			p.ticks += 0.05;// +(0.005*1.777*(GLUtils.getMouseX()-lastMouseX) + 0.005*(GLUtils.getMouseY()-lastMouseY));
			if(((p.ticks*p.speed)*p.ticks/10 + -yOffset/5) > displayHeight || ((p.ticks*p.speed)*p.ticks/10 + -yOffset/5) < 0 || (p.x + Math.sin(p.ticks/2)*50 + -xOffset/5) > displayWidth|| (p.x + Math.sin(p.ticks/2)*50 + -xOffset/5) < 0){
				toremove.add(p);
			}
		}

		particles.removeAll(toremove);
        GL11.glColor4f(1, 1, 1, 1);
        RenderSystem.enableBlend();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
		lastMouseX = getMouseX(displayWidth);
		lastMouseY = getMouseY(displayHeight);
	}

    public int getMouseX(int displayWidth) {
        return (int) (MinecraftClient.getInstance().mouse.getX() * displayWidth / displayWidth);
    }

    public int getMouseY(int displayHeight) {
        return (int) (displayHeight - MinecraftClient.getInstance().mouse.getY() * displayHeight / displayHeight - 1);
    }

    public static void drawBorderedCircle(double x, double y, float radius, int outsideC, int insideC) {
        GL11.glDisable((int)3553);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glEnable((int)2848);
        GL11.glPushMatrix();
        GL11.glScalef((float)0.1f, (float)0.1f, (float)0.1f);
        drawCircle(x *= 10, y *= 10, radius *= 10.0f, insideC);
        GL11.glScalef((float)10.0f, (float)10.0f, (float)10.0f);
        GL11.glPopMatrix();
        GL11.glEnable((int)3553);
        GL11.glDisable((int)2848);
    }

    public static void drawCircle(double x, double y, float radius, int color) {
        float alpha = (float)(color >> 24 & 255) / 255.0f;
        float red = (float)(color >> 16 & 255) / 255.0f;
        float green = (float)(color >> 8 & 255) / 255.0f;
        float blue = (float)(color & 255) / 255.0f;
        GL11.glColor4f((float)red, (float)green, (float)blue, (float)alpha);
        GL11.glBegin((int)9);
        int i = 0;
        while (i <= 360) {
            GL11.glVertex2d(x + Math.sin((double)i * 3.141526 / 180.0) * (double)radius, (double)((double)y + Math.cos((double)i * 3.141526 / 180.0) * (double)radius));
            ++i;
        }
        GL11.glEnd();
    }
}
