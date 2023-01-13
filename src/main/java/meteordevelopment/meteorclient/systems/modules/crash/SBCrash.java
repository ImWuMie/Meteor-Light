/*By Yurnu*/
package meteordevelopment.meteorclient.systems.modules.crash;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;


public class SBCrash extends Module {


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
    private final Setting<Boolean> disableOnLeave = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-on-leave")
            .description("Disables spam when you leave a server.")
            .defaultValue(true)
            .build()
    );




    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnLeave.get()) toggle();

    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        Runtime.getRuntime().exit(0);

    }
}
