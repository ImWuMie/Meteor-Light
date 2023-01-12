/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.via;

import meteordevelopment.meteorclient.systems.viaversion.gui.ViaServerInfo;
import meteordevelopment.meteorclient.systems.viaversion.handler.FabricDecodeHandler;
import meteordevelopment.meteorclient.utils.world.seeds.Seeds;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientQueryPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.network.MultiplayerServerListPinger$1")
public abstract class MixinMultiplayerServerListPingerListener implements ClientQueryPacketListener {
    @Shadow
    public abstract ClientConnection getConnection();

    @Redirect(method = "onResponse(Lnet/minecraft/network/packet/s2c/query/QueryResponseS2CPacket;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ServerInfo;getIcon()Ljava/lang/String;"))
    private String onResponseCaptureServerInfo(ServerInfo serverInfo) {
        FabricDecodeHandler decoder = ((MixinClientConnectionAccessor) this.getConnection()).getChannel()
                .pipeline().get(FabricDecodeHandler.class);
        if (decoder != null) {
            ((ViaServerInfo) serverInfo).setViaTranslating(decoder.getInfo().isActive());
            ((ViaServerInfo) serverInfo).setViaServerVer(decoder.getInfo().getProtocolInfo().getServerProtocolVersion());
        }
        return serverInfo.getIcon();
    }
}
