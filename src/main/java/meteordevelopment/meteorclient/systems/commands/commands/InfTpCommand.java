package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.UpdateEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.ClientPosArgumentType;
import meteordevelopment.meteorclient.systems.commands.arguments.PlayerArgumentType;
import meteordevelopment.meteorclient.utils.pathfinding.CustomPathFinder;
import meteordevelopment.meteorclient.utils.player.PathFinder;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.time.TickTimer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class InfTpCommand extends Command {
    private CustomPathFinder.Vec3 to = null;
    private ArrayList<CustomPathFinder.Vec3> path = new ArrayList<>();
    TickTimer timer = new TickTimer();

    public InfTpCommand() {
        super("infTp", "infinite tp", "itp","inf");
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("player").then(argument("player", PlayerArgumentType.player()).executes(context -> {
            PlayerEntity player = PlayerArgumentType.getPlayer(context);
            CustomPathFinder.Vec3 topPlayer = new CustomPathFinder.Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            to = new CustomPathFinder.Vec3(player.getX(),player.getY(),player.getZ());
            path = PathFinder.computePath(topPlayer,to);
            for (CustomPathFinder.Vec3 pathElm : path) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pathElm.getX(), pathElm.getY(), pathElm.getZ(), true));
                mc.player.updatePosition(pathElm.getX(), pathElm.getY(), pathElm.getZ());
            }
            return SINGLE_SUCCESS;
        })));
        builder.then(literal("pos").then(argument("pos", ClientPosArgumentType.pos()).executes(ctx -> {
            Vec3d pos = ClientPosArgumentType.getPos(ctx, "pos");
            to = new CustomPathFinder.Vec3(pos.x,pos.y,pos.z);
            CustomPathFinder.Vec3 topPlayer = new CustomPathFinder.Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            path = PathFinder.computePath(topPlayer,to);
            for (CustomPathFinder.Vec3 pathElm : path) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pathElm.getX(), pathElm.getY(), pathElm.getZ(), true));
                mc.player.updatePosition(pathElm.getX(), pathElm.getY(), pathElm.getZ());
            }
            return SINGLE_SUCCESS;
        })));
    }

    @EventHandler
    public void onUpdate(UpdateEvent e) {
        if (path != null) {
            if (timer.hasTimePassed(15)) {
                path = null;
                timer.reset();
            }
            timer.update();
        }
    }

    @EventHandler
    public void render(Render3DEvent e) {
        if (path != null) {
            for (CustomPathFinder.Vec3 pos : path) {
                if (pos != null)
                    drawPath(pos, e);
            }
        }
    }

    public void drawPath(CustomPathFinder.Vec3 vec, Render3DEvent r) {
        r.renderer.vecBox(vec,mc.player.getBoundingBox(mc.player.getPose()), new Color(255, 255, 255, 200), new Color(255, 255, 255, 200), ShapeMode.Lines, 0);
    }
}
