/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.via;

import io.netty.channel.Channel;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.viaversion.handler.PipelineReorderEvent;
import meteordevelopment.meteorclient.systems.viaversion.service.ProtocolAutoDetector;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;


@Mixin(ClientConnection.class)
public class MixinClientConnection {
	@Shadow
	private Channel channel;

    @Inject(method = "connect", at = @At("HEAD"))
    private static void onConnect(InetSocketAddress address, boolean useEpoll, CallbackInfoReturnable<ClientConnection> cir) {
        try {
            if (!MeteorClient.config.isClientSideEnabled()) return;
            ProtocolAutoDetector.detectVersion(address).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            MeteorClient.JLOGGER.log(Level.WARNING, "Could not auto-detect protocol for " + address + " " + e);
        }
    }

	@Inject(method = "setCompressionThreshold", at = @At("RETURN"))
	private void reorderCompression(int compressionThreshold, boolean rejectBad, CallbackInfo ci) {
		channel.pipeline().fireUserEventTriggered(new PipelineReorderEvent());
	}
}
