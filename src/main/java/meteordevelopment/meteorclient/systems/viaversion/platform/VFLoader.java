package meteordevelopment.meteorclient.systems.viaversion.platform;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.viaversion.providers.FabricVersionProvider;
import meteordevelopment.meteorclient.systems.viaversion.providers.VRHandItemProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.platform.ViaPlatformLoader;
import com.viaversion.viaversion.bungee.providers.BungeeMovementTransmitter;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.HandItemProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.MovementTransmitterProvider;

public class VFLoader implements ViaPlatformLoader {
    @Override
    public void load() {
        Via.getManager().getProviders().use(MovementTransmitterProvider.class, new BungeeMovementTransmitter());
        Via.getManager().getProviders().use(VersionProvider.class, new FabricVersionProvider());

        if (Via.getPlatform().getConf().isItemCache()) {
            VRHandItemProvider handProvider = new VRHandItemProvider();
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                handProvider.registerClientTick(new TickEvent.Pre());
            }
            Via.getManager().getProviders().use(HandItemProvider.class, handProvider);
        }
    }

    @Override
    public void unload() {
        // Nothing to do
    }
}
