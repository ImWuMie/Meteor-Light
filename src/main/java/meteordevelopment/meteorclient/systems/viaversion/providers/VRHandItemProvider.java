package meteordevelopment.meteorclient.systems.viaversion.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.HandItemProvider;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.viaversion.util.RemappingUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class VRHandItemProvider extends HandItemProvider {
    public Item clientItem = null;

    @Override
    public Item getHandItem(UserConnection info) {
        if (info.isClientSide()) {
            return getClientItem();
        }
        return super.getHandItem(info);
    }

    private Item getClientItem() {
        if (clientItem == null) {
            return new DataItem(0, (byte) 0, (short) 0, null);
        }
        return new DataItem(clientItem);
    }

    @EventHandler
    public void registerClientTick(TickEvent.Pre e) {
        try {
            tickClient();
        } catch (NoClassDefFoundError ignored) {
            MeteorClient.JLOGGER.info("Fabric Lifecycle V1 isn't installed");
        }
    }

    private void tickClient() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p != null) {
            clientItem = fromNative(p.getInventory().getMainHandStack());
        }
    }

    private Item fromNative(ItemStack original) {
        Identifier iid = Registry.ITEM.getId(original.getItem());
        int id = RemappingUtil.swordId(iid.toString());
        return new DataItem(id, (byte) original.getCount(), (short) original.getDamage(), null);
    }
}
