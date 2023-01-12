package meteordevelopment.meteorclient.systems.modules.render;

import ladysnake.satin.api.event.ShaderEffectRenderCallback;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import ladysnake.satin.api.managed.ShaderEffectManager;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorIdentifier;
import net.minecraft.util.Identifier;

public class MotionBlur extends Module {
    private final SettingGroup sgVisual = settings.createGroup("Visual");

    public final Setting<Integer> blurAmount = sgVisual.add(new IntSetting.Builder()
            .name("blur-amount")
            .description("blur amount.")
            .range(0,100)
            .sliderRange(0,100)
            .defaultValue(50)
            .build()
    );

    public MotionBlur() {
        super(Categories.Render, "motion-blur", "blur~~");
    }
}
