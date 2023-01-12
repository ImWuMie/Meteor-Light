package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.InfiniteAura;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;

public class AStarCustomPathFinders {
    private Vec3 startVec3;
    private Vec3 endVec3;
    private ArrayList<Vec3> path = new ArrayList<Vec3>();
    private ArrayList<Hub> hubs = new ArrayList<Hub>();
    private ArrayList<Hub> hubsToWork = new ArrayList<Hub>();
    private double minDistanceSquared = 9;
    private boolean nearest = true;

    private static Vec3[] flatCardinalDirections = {
            new Vec3(1, 0, 0),
            new Vec3(-1, 0, 0),
            new Vec3(0, 0, 1),
            new Vec3(0, 0, -1)
    };

    public AStarCustomPathFinders(Vec3 startVec3, Vec3 endVec3) {
        this.startVec3 = startVec3.addVector(0, 0, 0).floor();
        this.endVec3 = endVec3.addVector(0, 0, 0).floor();
    }

    public ArrayList<Vec3> getPath() {
        return path;
    }

    public void compute() {
        compute(1000, 4);
    }

    public void compute(int loops, int depth) {
        this.path.clear();
        this.hubsToWork.clear();
        ArrayList<Vec3> initPath = new ArrayList<Vec3>();
        Vec3 startCustomVec3 = this.startVec3;
        initPath.add(startCustomVec3);
        Vec3[] flatCardinalDirections = AStarCustomPathFinders.flatCardinalDirections;
        this.hubsToWork.add(new Hub(startCustomVec3, null, initPath, startCustomVec3.squareDistanceTo(this.endVec3), 0.0, 0.0));
        block0:
        for (int i = 0; i < loops; ++i) {
            ArrayList<Hub> hubsToWork = this.hubsToWork;
            int hubsToWorkSize = hubsToWork.size();
            hubsToWork.sort(new CompareHub());
            int j = 0;
            if (hubsToWorkSize == 0) break;
            for (int i1 = 0; i1 < hubsToWorkSize; ++i1) {
                Vec3 loc2;
                Hub hub = hubsToWork.get(i1);
                if (++j > depth) continue block0;
                hubsToWork.remove(hub);
                this.hubs.add(hub);
                Vec3 hLoc = hub.getLoc();
                int flatCardinalDirectionsLength = flatCardinalDirections.length;
                for (int i2 = 0; i2 < flatCardinalDirectionsLength; ++i2) {
                    Vec3 loc = hLoc.add(flatCardinalDirections[i2]).floor();
                    if (AStarCustomPathFinders.checkPositionValidity(loc) && this.addHub(hub, loc, 0.0)) break block0;
                }
                Vec3 loc1 = hLoc.addVector(0.0, 1.0, 0.0).floor();
                if (AStarCustomPathFinders.checkPositionValidity(loc1) && this.addHub(hub, loc1, 0.0) || AStarCustomPathFinders.checkPositionValidity(loc2 = hLoc.addVector(0.0, -1.0, 0.0).floor()) && this.addHub(hub, loc2, 0.0))
                    break block0;
            }
        }
        this.hubs.sort(new CompareHub());
        this.path = this.hubs.get(0).getPath();
    }

    public static boolean checkPositionValidity(Vec3 loc) {
        return checkPositionValidity((int) loc.getX(), (int) loc.getY(), (int) loc.getZ());
    }

    public static boolean checkPositionValidity(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (AStarCustomPathFinders.isBlockSolid(pos) || AStarCustomPathFinders.isBlockSolid(pos.add(0, 1, 0))) {
            return false;
        }
        return AStarCustomPathFinders.isSafeToWalkOn(pos.add(0, -1, 0));
    }

    private static boolean isBlockSolid(BlockPos block) {
        InfiniteAura infiniteAura = (InfiniteAura) Modules.get().get(InfiniteAura.class);

        BlockState bs = PathFinder.getBlockState(block);
        Block b = PathFinder.getBlock(block);
        boolean a = bs.isFullCube(MinecraftClient.getInstance().world, block);
        switch (infiniteAura.twomod.get()) {
            case 0 -> {
                a = (bs.isSolidBlock(MinecraftClient.getInstance().world, block) || b.isShapeFullCube(bs, MinecraftClient.getInstance().world, block));
            }
            case 1 -> {
                a = (bs.isSolidBlock(MinecraftClient.getInstance().world, block));
            }
            case 2 -> {
                a = (b.isShapeFullCube(bs, MinecraftClient.getInstance().world, block));
            }
            case 3 -> {
                a = bs.isFullCube(MinecraftClient.getInstance().world, block);
            }
            case 4 -> {
                a = (bs.isSolidBlock(MinecraftClient.getInstance().world, block) || b.isShapeFullCube(bs, MinecraftClient.getInstance().world, block) || bs.isFullCube(MinecraftClient.getInstance().world, block));
            }
            case 5 -> {
                a = (bs.isFullCube(MinecraftClient.getInstance().world, block) && (bs.isSolidBlock(MinecraftClient.getInstance().world, block)));
            }
        }

        return a || b instanceof SlabBlock || b instanceof StairsBlock || b instanceof CactusBlock || b instanceof ChestBlock || b instanceof EnderChestBlock || b instanceof SkullBlock || b instanceof PaneBlock ||
                b instanceof FenceBlock || b instanceof WallBlock || b instanceof GlassBlock || b instanceof PistonBlock || b instanceof PistonExtensionBlock || b instanceof PistonHeadBlock || b instanceof StainedGlassBlock ||
                b instanceof TrapdoorBlock ||
                // 1.19
                b instanceof AnvilBlock || b instanceof BambooBlock || b instanceof BarrelBlock || b instanceof BarrierBlock || b instanceof BellBlock ||
                b instanceof CakeBlock || b instanceof JukeboxBlock;
    }

    private static boolean isSafeToWalkOn(BlockPos block) {
        Block b = PathFinder.getBlock(block);
        return !(b instanceof FenceBlock) && !(b instanceof WallBlock);
    }

    public Hub isHubExisting(Vec3 loc) {
        for (Hub hub : hubs) {
            if (hub.getLoc().getX() == loc.getX() && hub.getLoc().getY() == loc.getY() && hub.getLoc().getZ() == loc.getZ()) {
                return hub;
            }
        }
        for (Hub hub : hubsToWork) {
            if (hub.getLoc().getX() == loc.getX() && hub.getLoc().getY() == loc.getY() && hub.getLoc().getZ() == loc.getZ()) {
                return hub;
            }
        }
        return null;
    }

    public boolean addHub(Hub parent, Vec3 loc, double cost) {
        Hub existingHub = isHubExisting(loc);
        double totalCost = cost;
        if (parent != null) {
            totalCost += parent.getTotalCost();
        }
        if (existingHub == null) {
            if ((loc.getX() == endVec3.getX() && loc.getY() == endVec3.getY() && loc.getZ() == endVec3.getZ()) || (minDistanceSquared != 0 && loc.squareDistanceTo(endVec3) <= minDistanceSquared)) {
                path.clear();
                path = parent.getPath();
                path.add(loc);
                return true;
            } else {
                ArrayList<Vec3> path = new ArrayList<Vec3>(parent.getPath());
                path.add(loc);
                hubsToWork.add(new Hub(loc, parent, path, loc.squareDistanceTo(endVec3), cost, totalCost));
            }
        } else if (existingHub.getCost() > cost) {
            ArrayList<Vec3> path = new ArrayList<Vec3>(parent.getPath());
            path.add(loc);
            existingHub.setLoc(loc);
            existingHub.setParent(parent);
            existingHub.setPath(path);
            existingHub.setSquareDistanceToFromTarget(loc.squareDistanceTo(endVec3));
            existingHub.setCost(cost);
            existingHub.setTotalCost(totalCost);
        }
        return false;
    }

    private class Hub {
        private Vec3 loc = null;
        private Hub parent = null;
        private ArrayList<Vec3> path;
        private double squareDistanceToFromTarget;
        private double cost;
        private double totalCost;

        public Hub(Vec3 loc, Hub parent, ArrayList<Vec3> path, double squareDistanceToFromTarget, double cost, double totalCost) {
            this.loc = loc;
            this.parent = parent;
            this.path = path;
            this.squareDistanceToFromTarget = squareDistanceToFromTarget;
            this.cost = cost;
            this.totalCost = totalCost;
        }

        public Vec3 getLoc() {
            return loc;
        }

        public Hub getParent() {
            return parent;
        }

        public ArrayList<Vec3> getPath() {
            return path;
        }

        public double getSquareDistanceToFromTarget() {
            return squareDistanceToFromTarget;
        }

        public double getCost() {
            return cost;
        }

        public void setLoc(Vec3 loc) {
            this.loc = loc;
        }

        public void setParent(Hub parent) {
            this.parent = parent;
        }

        public void setPath(ArrayList<Vec3> path) {
            this.path = path;
        }

        public void setSquareDistanceToFromTarget(double squareDistanceToFromTarget) {
            this.squareDistanceToFromTarget = squareDistanceToFromTarget;
        }

        public void setCost(double cost) {
            this.cost = cost;
        }

        public double getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(double totalCost) {
            this.totalCost = totalCost;
        }
    }

    public class CompareHub implements Comparator<Hub> {
        @Override
        public int compare(Hub o1, Hub o2) {
            return (int) (
                    (o1.getSquareDistanceToFromTarget() + o1.getTotalCost()) - (o2.getSquareDistanceToFromTarget() + o2.getTotalCost())
            );
        }
    }
}

