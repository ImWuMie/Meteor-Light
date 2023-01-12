package meteordevelopment.meteorclient.utils.pathfinding;

import com.google.common.collect.Lists;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class BBRayTrace {

    private static MinecraftClient mc = MinecraftClient.getInstance();

    private int highestHitBlockHeight;
    private BlockPos highestBlock = null;
    private boolean hitBlock;
    private ArrayList<BlockPos> hitBlocks = new ArrayList<BlockPos>();

    public BBRayTrace(Vec3d pos1, Vec3d pos2, int checks, double bbSize) {
        Box bb = mc.player.getBoundingBox();
        bb.expand(bbSize, 0, bbSize);
        double xDist = pos2.x - pos1.x;
        double yDist = pos2.y - pos1.y;
        double zDist = pos2.z - pos1.z;
        for(int i = 0; i < checks; i++) {
            bb = bb.offset((zDist / checks) * i, ((yDist / checks) * i) + 0.05, (xDist / checks) * i);
            for(BlockPos pos : getCollidingBlockPositions(mc.player, bb)) {
                if(!hitBlocks.contains(pos)) {
                    hitBlocks.add(pos);
                }
            }
        }
        if(hitBlocks.isEmpty()) {
            hitBlock = false;
            return;
        }
        hitBlock = true;
        int maxHeight = -1000;
        for(BlockPos pos : hitBlocks) {
            if(pos.getY() > maxHeight) {
                maxHeight = pos.getY();
                highestBlock = pos;
            }
        }
    }

    public boolean didHitBlock() {
        return hitBlock;
    }

    public ArrayList<BlockPos> getHitBlocks() {
        return hitBlocks;
    }

    public int getHighestHitBlockHeight() {
        return highestHitBlockHeight;
    }

    public BlockPos getHighestBlock() {
        return highestBlock;
    }

    public static List<BlockPos> getCollidingBlockPositions(Entity entityIn, Box bb) {
        List<BlockPos> list = Lists.newArrayList();
        int minX = MathHelper.floor(bb.minX);
        int maxX = MathHelper.floor(bb.maxX + 1.0D);
        int minY = MathHelper.floor(bb.minY);
        int maxY = MathHelper.floor(bb.maxY + 1.0D);
        int minZ = MathHelper.floor(bb.minZ);
        int maxZ = MathHelper.floor(bb.maxZ + 1.0D);
        BlockPos.Mutable blockPos = new BlockPos.Mutable();

        for (int loop0 = minX; loop0 < maxX; ++loop0) {
            for (int loop1 = minZ; loop1 < maxZ; ++loop1) {
                if (isBlockLoaded(blockPos.set(loop0, 64, loop1))) {
                    for (int loop2 = minY - 1; loop2 < maxY; ++loop2) {
                        blockPos.set(loop0, loop2, loop1);

                        list.add(blockPos);
                    }
                }
            }
        }
        return list;
    }

    public static boolean isBlockLoaded(BlockPos pos)
    {
        return mc.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }
}
