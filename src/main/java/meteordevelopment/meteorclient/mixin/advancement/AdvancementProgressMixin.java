/*
package meteordevelopment.meteorclient.mixin.advancement;

import meteordevelopment.meteorclient.advancement.AdvancementProgressAccessor;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.advancement.AdvancementProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(AdvancementProgress.class)
public class AdvancementProgressMixin implements AdvancementProgressAccessor {
    private Map<String, AdvancementCriterion> savedCriteria;

    @Inject(method = "init", at = @At("HEAD"))
    public void saveCriteria(Map<String, AdvancementCriterion> criteria, String[][] requirements, CallbackInfo ci) {
        savedCriteria = criteria;
    }

    @Override
    public AdvancementCriterion getCriterion(String name) {
        return savedCriteria.get(name);
    }
}
*/