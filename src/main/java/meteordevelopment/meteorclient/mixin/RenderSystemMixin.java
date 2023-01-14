package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.glrender.Core;
import meteordevelopment.meteorclient.glrender.MeteorGL;
import meteordevelopment.meteorclient.glrender.fragment.Fragment;
import meteordevelopment.meteorclient.glrender.opengl.GLCore;
import meteordevelopment.meteorclient.glrender.opengl.GLSurfaceCanvas;
import meteordevelopment.meteorclient.glrender.opengl.ShaderManager;
import meteordevelopment.meteorclient.glrender.screen.UIManager;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import net.minecraft.util.TimeSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
   /* @Inject(method = "initRenderer", at = @At("TAIL"))
    private static void onInitRenderer(int debugVerbosity, boolean debugSync, CallbackInfo ci) {
        new MeteorGL();
        Core.initMainThread();
        Core.initOpenGL();
        UIManager.initialize();
        UIManager.initializeRenderer();
    }*/
}
