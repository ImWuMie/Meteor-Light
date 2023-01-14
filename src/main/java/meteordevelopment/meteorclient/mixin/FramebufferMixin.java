package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IFramebuffer;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Framebuffer.class)
public class FramebufferMixin implements IFramebuffer {
    @Shadow protected int depthAttachment;

    @Override
    public void setDepthAttachment(int v) {
        this.depthAttachment = v;
    }
}
