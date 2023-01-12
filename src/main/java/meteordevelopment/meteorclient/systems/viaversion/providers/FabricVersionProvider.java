package meteordevelopment.meteorclient.systems.viaversion.providers;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.ChannelPipeline;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.viaversion.provider.AbstractFabricVersionProvider;
import meteordevelopment.meteorclient.systems.viaversion.service.ProtocolAutoDetector;
import net.minecraft.network.ClientConnection;
import meteordevelopment.meteorclient.systems.viaversion.config.VFConfig;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class FabricVersionProvider extends AbstractFabricVersionProvider {
    @Override
    protected Logger getLogger() {
        return MeteorClient.JLOGGER;
    }

    @Override
    protected VFConfig getConfig() {
        return MeteorClient.config;
    }

    @Override
    protected CompletableFuture<ProtocolVersion> detectVersion(InetSocketAddress address) {
        return ProtocolAutoDetector.detectVersion(address);
    }

    @Override
    protected boolean isMulticonnectHandler(ChannelPipeline pipe) {
        return pipe.get(ClientConnection.class).getPacketListener()
                .getClass().getName().startsWith("net.earthcomputer.multiconnect");
    }
}
