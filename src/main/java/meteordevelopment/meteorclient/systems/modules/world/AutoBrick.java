/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.combat.Offhand;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PathFinder;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.time.WaitTimer;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

public class AutoBrick extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup b1g = settings.createGroup("blockSign1 -- start");
    private final SettingGroup b2g = settings.createGroup("blockSign2 -- end");

    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Item> defaultitem = sgGeneral.add(new ItemSetting.Builder()
        .name("Item")
        .defaultValue(Items.CACTUS)
        .build()
    );

    private final Setting<String> startRes = sgGeneral.add(new StringSetting.Builder()
        .name("StartCommand")
        .description("res tp xxx & warp xxx")
        .defaultValue("res tp ")
        .build()
    );

    private final Setting<String> endRes = sgGeneral.add(new StringSetting.Builder()
        .name("EndCommand")
        .description("res tp xxx && warp xxx")
        .defaultValue("warp ")
        .build()
    );

    private final Setting<Integer> allDelay = sgGeneral.add(new IntSetting.Builder()
        .name("all-delay")
        .defaultValue(100)
        .build()
    );

    private final Setting<Boolean> attackblock = sgRender.add(new BoolSetting.Builder()
        .name("attackblock")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> tp_x = sgGeneral.add(new IntSetting.Builder()
        .name("tp_x")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> tp_y = sgGeneral.add(new IntSetting.Builder()
            .name("tp_y")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> tp_z = sgGeneral.add(new IntSetting.Builder()
        .name("tp_z")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> b1_x = b1g.add(new IntSetting.Builder()
        .name("block1_x")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> b1_y = b1g.add(new IntSetting.Builder()
            .name("block1_y")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> b1_z = b1g.add(new IntSetting.Builder()
        .name("block1_z")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> b2_x = b2g.add(new IntSetting.Builder()
        .name("block2_x")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> b2_y = b2g.add(new IntSetting.Builder()
        .name("block2_y")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> b2_z = b2g.add(new IntSetting.Builder()
        .name("block2_z")
        .defaultValue(0)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay on the block being broken.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 10))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    private BlockPos targetBlock1;
    private BlockPos targetBlock2;
    private boolean signyes;
    private boolean inshop;
    private Vec3d lastPos;
    private boolean bb;
    private int timer;

    @Override
    public void onActivate() {
        yongyustop1 = false;
        yongyustop2 = false;
        yongyustop3 = false;
        yongyustop4 = false;
        yongyustop5 = false;
        startTpRes();
        lastPos = mc.player.getPos();
        timer = allDelay.get();
        //start();
        super.onActivate();
    }

    @EventHandler
    private void onDamageBlock(StartBreakingBlockEvent e) {
        if (!attackblock.get()) return;
        b1_x.set(e.blockPos.getX());
        b1_y.set(e.blockPos.getY());
        b1_z.set(e.blockPos.getZ());
    }

    @EventHandler
    private void onRender(Render3DEvent e) {
        if (b1_x.get() == null) return;
        if (b1_y.get() == null) return;
        if (b1_z.get() == null) return;
        if (b2_x.get() == null) return;
        if (b2_y.get() == null) return;
        if (b2_z.get() == null) return;

        targetBlock1 = new BlockPos(b1_x.get(),b1_y.get(),b1_z.get());
        targetBlock2 = new BlockPos(b2_x.get(),b2_y.get(),b2_z.get());

        if (inshop) e.renderer.box(targetBlock2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        else e.renderer.box(targetBlock1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    @Override
    public void onDeactivate() {
        yongyustop1 = false;
        yongyustop2 = false;
        yongyustop3 = false;
        yongyustop4 = false;
        yongyustop5 = false;
        signyes = false;
        inshop = false;
        bb = false;
        lastPos = null;
        super.onDeactivate();
    }

private boolean yongyustop1 = false;
private boolean yongyustop2 = false;
private boolean yongyustop3 = false;
private boolean yongyustop4 = false;
private boolean yongyustop5 = false;

    @EventHandler
    private void onTicke(TickEvent.Pre e) {
        if (targetBlock1 == null) return;
        if (targetBlock2 == null) return;

        if (yongyustop5) {
            if (mc.player.getMainHandStack().getItem().equals(Items.AIR)) {
                if (!yongyustop1) {
                    startTpRes();
                    yongyustop1 = true;
                    yongyustop2 = false;
                    yongyustop5 = false;
                }
            } else {
                if (distanceTo(targetBlock2) <= 10) {
                    mc.player.setPos(tp_x.get(), tp_y.get(), tp_z.get());
                }
                mc.interactionManager.attackBlock(targetBlock2, Direction.getFacing(b2_x.get(), b2_y.get(), b2_z.get()));
                Rotations.rotate(Rotations.getYaw(targetBlock2), Rotations.getPitch(targetBlock2));
            }
    }
        if (mc.player.getMainHandStack().getItem().equals(defaultitem.get())) {
            if (!yongyustop2) {
                endTpRes();
                yongyustop5 = true;
                yongyustop2 = true;
                yongyustop1 = false;
            }
        } else {
            mc.interactionManager.attackBlock(targetBlock1, Direction.getFacing(b1_x.get(), b1_y.get(), b1_z.get()));
            Rotations.rotate(Rotations.getYaw(targetBlock1), Rotations.getPitch(targetBlock1));
        }
    }

    @EventHandler
    private void onReceMessage(ReceiveMessageEvent e) {
        PathFinder pathFinder = new PathFinder();
        String message = e.getMessage().getString();

        if (message.contains("这个商店已缺货")) {
            BlockPos tblock = new BlockPos(b2_x.get()-1, b2_y.get(), b2_z.get());
            if (pathFinder.getBlockAtPos(tblock).equals(Blocks.OAK_SIGN)) {
                targetBlock1 = tblock;
                setb1value(tblock);
            } else {
                BlockPos t2b = new BlockPos(b2_x.get(), b2_y.get()-1, b2_z.get());
                if (pathFinder.getBlockAtPos(t2b).equals(Blocks.OAK_SIGN)) {
                    targetBlock1 = t2b;
                    setb1value(t2b);
                } else {
                    BlockPos t3b = new BlockPos(b2_x.get()+1, b2_y.get(), b2_z.get());
                    if (pathFinder.getBlockAtPos(t2b).equals(Blocks.OAK_SIGN)) {
                        setb1value(t3b);
                        targetBlock1 = t3b;
                    } else {
                        BlockPos t4b = new BlockPos(b2_x.get(), b2_y.get()+1, b2_z.get());
                        if (pathFinder.getBlockAtPos(t2b).equals(Blocks.OAK_SIGN)) {
                            targetBlock1 = t4b;
                            setb1value(t4b);
                        } else {
                            BlockPos t5b = new BlockPos(b2_x.get()-1, b2_y.get(), b2_z.get());
                            if (pathFinder.getBlockAtPos(t2b).equals(Blocks.OAK_SIGN)) {
                                targetBlock1 = t5b;
                                setb1value(t5b);
                            }
                        }
                    }
                }
            }
        }
    }

    void setb1value(Integer x,Integer y,Integer z) {
        b1_x.set(x);
        b1_y.set(y);
        b1_z.set(z);
    }

    void setb1value(BlockPos bpos) {
        b1_x.set(bpos.getX());
        b1_y.set(bpos.getY());
        b1_z.set(bpos.getZ());
    }

    public float distanceTo(BlockPos pos) {
        float f = (float)(mc.player.getX() - pos.getX());
        float g = (float)(mc.player.getY() - pos.getY());
        float h = (float)(mc.player.getZ() - pos.getZ());
        return MathHelper.sqrt(f * f + g * g + h * h);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (timer <= 0) {
            String text = "all";

           sendChatMessage(text);
            timer = allDelay.get();
        }
        else {
            timer--;
        }
    }

    /*
    @EventHandler
    private void onTick(TickEvent.Pre e) {

        int aa = 0;
        int bbsa = 0;
        PathFinder pathFinder = new PathFinder();
        if (targetBlock1 == null) return;
        if (targetBlock2 == null) return;
            if (inshop) {
                if (mc.player.getPos() == lastPos) return;
                //mc.player.setPos(targetBlock2.getX(),targetBlock2.getY(),targetBlock2.getZ());
                if (mc.player.getMainHandStack().getItem().equals(defaultitem.get())) {
                    mc.interactionManager.attackBlock(targetBlock2, Direction.getFacing(b2_x.get(), b2_y.get(), b2_z.get()));
                    Rotations.rotate(Rotations.getYaw(targetBlock2), Rotations.getPitch(targetBlock2));
                    bb = true;
                } else bb = false;
                if (mc.player.getMainHandStack().getItem().equals(Items.AIR) || inshop) {
                    bb = true;
                }
                if (pathFinder.getBlockAtPos(targetBlock2) instanceof SignBlock || inshop) {
                    signyes = true;
                }
                if (signyes) {
                    if (bb) {
                        bb = false;
                        mc.player.sendChatMessage("all");
                            signyes = false;
                            inshop = false;
                            if (mc.player.getMainHandStack().getItem().equals(Items.AIR)) {
                                startTpRes();
                            }
                    }
                }
            } else {
                if (mc.player.getPos() != lastPos) return;
               // mc.player.setPos(targetBlock1.getX(),targetBlock1.getY(),targetBlock1.getZ());
                if (mc.player.getMainHandStack().getItem().equals(Items.AIR)) {
                    mc.interactionManager.attackBlock(targetBlock1, Direction.getFacing(b1_x.get(), b1_y.get(), b1_z.get()));
                    Rotations.rotate(Rotations.getYaw(targetBlock1), Rotations.getPitch(targetBlock2));
                    bb = true;
                } else bb = false;
                if (mc.player.getMainHandStack().getItem().equals(defaultitem.get()) || !inshop) {
                    bb = false;
                }
                if (pathFinder.getBlockAtPos(targetBlock1) instanceof SignBlock || !inshop) {
                    signyes = true;
                }
                if (signyes) {
                    if (!bb) {
                        bb = true;
                        mc.player.sendChatMessage("all");
                        signyes = false;
                        inshop = true;
                        if (mc.player.getMainHandStack().getItem().equals(defaultitem.get())) {
                            endTpRes();
                        }
                    }
                }
            }
    }
    */


    void startTpRes() {
        // start banzhuan di
        inshop = true;
        if (mc.player != null) {
        sendChatMessage("/" +startRes.get());
            inshop = false;
        }
    }

    void endTpRes() {
        // shop aw
        inshop = false;
        if (mc.player != null) {
          sendChatMessage("/" + endRes.get());
            inshop = true;
        }
    }

    public AutoBrick() {
        super(Categories.World, "infinite-money", "some server auto brick");
    }
}
