/*
package meteordevelopment.meteorclient.mixin.advancement;

import meteordevelopment.meteorclient.advancement.AdvancementInfo;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.advancement.AdvancementInfo.active;

@Mixin(AdvancementDisplay.class)
public class AdvancementDisplayMixin {

    @Shadow
    @Final
    private boolean hidden;
    @Shadow
    @Final
    private Text title;

    @Inject(method = "isHidden", at = @At("HEAD"), cancellable = true)
    public void isHiddenOverride(CallbackInfoReturnable<Boolean> cir) {
        if (!active) return;
        cir.cancel();
        cir.setReturnValue(hidden && !AdvancementInfo.showAll);
    }
}
*/