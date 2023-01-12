package meteordevelopment.meteorclient.systems.modules.crash;

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import java.util.Random;

public class SBCrash extends Module {
    private final Random r = new Random();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
            .name("SB Amount")
            .description("Let your client crash :D")
            .defaultValue(15)
            .min(1)
            .sliderMax(100)
            .build()
    );

    public SBCrash() {
        super(Categories.Crash, "SB-crash", "Crashes all servers and your client");
    }


    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.getNetworkHandler() == null) return;
        for (int i = 0; i < amount.get(); i++) {
            System.exit(0);
        }
    }
}
