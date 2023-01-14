package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.mixininterface.IFramebuffer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

public class Stencil {
	public static MinecraftClient mc = MinecraftClient.getInstance();
	
	public static int nextColor;
	
    public static void write(boolean renderClipLayer) {
      //  Stencil.checkSetupFBO();
        GlStateManager._clearStencil(0);
        GlStateManager._clear(GL11.GL_STENCIL_BUFFER_BIT,false);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GlStateManager._stencilFunc(GL11.GL_ALWAYS, 1, 65535);
        GlStateManager._stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        if(!renderClipLayer){
        GlStateManager._colorMask(false, false, false, false);
          //  GlStateManager.colorMask(true, true, true, true);
        }
    }

    public static void erase(boolean invert) {
        RenderSystem.stencilFunc(invert ? GL11.GL_EQUAL : GL11.GL_NOTEQUAL, (int)1, (int)65535);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderSystem.colorMask(true, true, true, true);
       // GL11.glEnable(GL11.GL_ALPHA_TEST);
        RenderSystem.enableBlend();
       // GL11.glAlphaFunc(GL11.GL_GREATER, 0.0f );
	 }
    

    public static void dispose() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
       // GL11.glDisable(GL11.GL_ALPHA_TEST);
        RenderSystem.disableBlend();
    }

    public static void checkSetupFBO() {
        net.minecraft.client.gl.Framebuffer fbo = mc.getFramebuffer();
        if (fbo != null && fbo.getDepthAttachment() > -1) {
            Stencil.setupFBO(fbo);
            ((IFramebuffer)fbo).setDepthAttachment(-1);
        }
    }

    
    public static void setupFBO(net.minecraft.client.gl.Framebuffer fbo) {
        EXTFramebufferObject.glDeleteRenderbuffersEXT((int)fbo.getDepthAttachment());
        int stencil_depth_buffer_ID = EXTFramebufferObject.glGenRenderbuffersEXT();
        EXTFramebufferObject.glBindRenderbufferEXT((int)36161, (int)stencil_depth_buffer_ID);
        EXTFramebufferObject.glRenderbufferStorageEXT((int)36161, (int)34041, mc.getWindow().getScaledWidth(),mc.getWindow().getScaledHeight());
        EXTFramebufferObject.glFramebufferRenderbufferEXT((int)36160, (int)36128, (int)36161, stencil_depth_buffer_ID);
        EXTFramebufferObject.glFramebufferRenderbufferEXT((int)36160, (int)36096, (int)36161, (int)stencil_depth_buffer_ID);
    }
}

