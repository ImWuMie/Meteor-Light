/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.Cancellable;
import meteordevelopment.meteorclient.events.entity.EntityDestroyEvent;
import meteordevelopment.meteorclient.events.entity.player.PickItemsEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.ContainerSlotUpdateEvent;
import meteordevelopment.meteorclient.events.packets.PlaySoundPacketEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.ChunkPosDataEvent;
import meteordevelopment.meteorclient.events.world.PlayerRespawnEvent;
import meteordevelopment.meteorclient.mixininterface.IExplosionS2CPacket;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
/*
import meteordevelopment.meteorclient.systems.seedcrack.config.Config;
import meteordevelopment.meteorclient.systems.seedcrack.config.StructureSave;
import meteordevelopment.meteorclient.systems.seedcrack.cracker.DataAddedEvent;
import meteordevelopment.meteorclient.systems.seedcrack.cracker.HashedSeedData;
import meteordevelopment.meteorclient.systems.seedcrack.finder.FinderQueue;
import meteordevelopment.meteorclient.systems.seedcrack.finder.ReloadFinders;
import meteordevelopment.meteorclient.systems.seedcrack.init.ClientCommands;
import meteordevelopment.meteorclient.systems.seedcrack.util.Log;*/
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.telemetry.TelemetrySender;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Shadow @Final private MinecraftClient client;
    @Shadow private ClientWorld world;
    @Shadow private CommandDispatcher<CommandSource> commandDispatcher;
    private boolean worldNotNull;


    @Inject(method = "onGameJoin", at = @At("HEAD"))
    private void onGameJoinHead(GameJoinS2CPacket packet, CallbackInfo info) {
        worldNotNull = world != null;
    }

/*
    @SuppressWarnings("unchecked")
    @Inject(method = "onCommandTree", at = @At("TAIL"))
    public void onOnCommandTree(CommandTreeS2CPacket packet, CallbackInfo ci) {
        ClientCommands.registerCommands((CommandDispatcher<ServerCommandSource>) (Object) this.commandDispatcher);
    }
*/

    //called on dimension change too
    @Inject(method = "onPlayerRespawn", at = @At("TAIL"))
    public void onPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        /*
        newDimension(new HashedSeedData(packet.getSha256Seed()), true);*/
        MeteorClient.EVENT_BUS.post(PlayerRespawnEvent.get());
    }

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void onGameJoinTail(GameJoinS2CPacket packet, CallbackInfo info) {
      /*  newDimension(new HashedSeedData(packet.sha256Seed()), false);
        var preloaded = StructureSave.loadStructures();
        if (!preloaded.isEmpty()) {
            Log.warn("foundRestorableStructures", preloaded.size());
        }*/
        if (worldNotNull) {
            MeteorClient.EVENT_BUS.post(GameLeftEvent.get());
        }

        MeteorClient.EVENT_BUS.post(GameJoinedEvent.get());
    }

/*
    public void newDimension(HashedSeedData hashedSeedData, boolean dimensionChange) {
        DimensionType dimension = MinecraftClient.getInstance().world.getDimension();
        ReloadFinders.reloadHeight(dimension.minY(), dimension.minY() + dimension.logicalHeight());

        if (MeteorClient.INSTANCE.getDataStorage().addHashedSeedData(hashedSeedData, DataAddedEvent.POKE_BIOMES) && Config.get().active && dimensionChange) {
            Log.error(Log.translate("fetchedHashedSeed"));
            if (Config.get().debug) {
                Log.error("Hashed seed [" + hashedSeedData.getHashedSeed() + "]");
            }
        }
    }
*/
    @Inject(method = "onPlaySound", at = @At("HEAD"))
    private void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(PlaySoundPacketEvent.get(packet));
    }

    @Inject(method = "onChunkData", at = @At("TAIL"))
    private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo info) {
      /*  int chunkX = packet.getX();
        int chunkZ = packet.getZ();
        FinderQueue.get().onChunkData(this.world, new ChunkPos(chunkX, chunkZ));*/
        WorldChunk chunk = client.world.getChunk(packet.getX(), packet.getZ());
        MeteorClient.EVENT_BUS.post(ChunkPosDataEvent.get(packet.getX(), packet.getZ()));
        MeteorClient.EVENT_BUS.post(ChunkDataEvent.get(chunk));
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("TAIL"))
    private void onContainerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(ContainerSlotUpdateEvent.get(packet));
    }

    @Inject(method = "onEntitiesDestroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/EntitiesDestroyS2CPacket;getEntityIds()Lit/unimi/dsi/fastutil/ints/IntList;"))
    private void onEntitiesDestroy(EntitiesDestroyS2CPacket packet, CallbackInfo ci) {
        for (int id : packet.getEntityIds()) {
            MeteorClient.EVENT_BUS.post(EntityDestroyEvent.get(client.world.getEntityById(id)));
        }
    }

    @Inject(
        method = "onExplosion",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onExplosionVelocity(ExplosionS2CPacket packet, CallbackInfo ci) {
        Velocity velocity = Modules.get().get(Velocity.class); //Velocity for explosions
        if (velocity.mode.get().equals(Velocity.Mode.Vanilla)) {
            if (!velocity.explosions.get()) return;

            ((IExplosionS2CPacket) packet).setVelocityX((float) (packet.getPlayerVelocityX() * velocity.getHorizontal(velocity.explosionsHorizontal)));
            ((IExplosionS2CPacket) packet).setVelocityY((float) (packet.getPlayerVelocityY() * velocity.getVertical(velocity.explosionsVertical)));
            ((IExplosionS2CPacket) packet).setVelocityZ((float) (packet.getPlayerVelocityZ() * velocity.getHorizontal(velocity.explosionsHorizontal)));
        }
    }

    @Inject(method = "onItemPickupAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getEntityById(I)Lnet/minecraft/entity/Entity;", ordinal = 0))
    private void onItemPickupAnimation(ItemPickupAnimationS2CPacket packet, CallbackInfo info) {
        Entity itemEntity = client.world.getEntityById(packet.getEntityId());
        Entity entity = client.world.getEntityById(packet.getCollectorEntityId());

        if (itemEntity instanceof ItemEntity && entity == client.player) {
            MeteorClient.EVENT_BUS.post(PickItemsEvent.get(((ItemEntity) itemEntity).getStack(), packet.getStackAmount()));
        }
    }
}
