/*
package meteordevelopment.meteorclient.mixin.advancement;

import meteordevelopment.meteorclient.advancement.AdvancementInfo;
import meteordevelopment.meteorclient.advancement.AdvancementWidgetAccessor;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementWidget.class)
public class AdvancementWidgetMixin implements AdvancementWidgetAccessor {

    @Shadow
    private AdvancementProgress progress;

    @Inject(method = "drawTooltip", at = @At("HEAD"))
    public void rememberTooltip(MatrixStack stack, int i, int j, float f, int y, int k, CallbackInfo ci) {
        AdvancementInfo.mouseOver = (AdvancementWidget) (Object) this;
    }

    @Override
    public AdvancementProgress getProgress() {
        return this.progress;
    }
}
*/