package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.font.adapter.FontAdapter;
import meteordevelopment.meteorclient.renderer.font.renderer.ColoredTextSegment;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Utils {

    private static final ExecutorService esv = Executors.newCachedThreadPool();
    public static boolean sendPackets = true;

    public static void waitUntil(BooleanSupplier e, Runnable v) {
        if (!e.getAsBoolean()) {
            v.run(); // dont need to spin up thread
        } else {
            esv.submit(() -> {
                while (!e.getAsBoolean()) {
                    Thread.onSpinWait();
                }
                v.run();
            });
        }
    }

    public static Stream<LivingEntity> findEntities(Predicate<? super LivingEntity> requirement) {
        Spliterator<Entity> spliterator = MeteorClient.client.world.getEntities().spliterator();

        return StreamSupport.stream(spliterator, false)
            .filter(entity -> !entity.equals(MeteorClient.client.player))
            .filter(entity -> entity instanceof LivingEntity)
            .map(entity -> (LivingEntity) entity)
            .filter(requirement);
    }

    public static boolean isABFree(Vec3d a, Vec3d b) {
        assert MeteorClient.client.player != null;
        assert MeteorClient.client.world != null;
        RaycastContext rc = new RaycastContext(a, b, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, MeteorClient.client.player);
        BlockHitResult raycast = MeteorClient.client.world.raycast(rc);
        return raycast.getType() == HitResult.Type.MISS;
    }

    public static float dist(double ax, double ay, double bx, double by) {
        return (float) java.lang.Math.sqrt(java.lang.Math.pow(ax - bx, 2) + java.lang.Math.pow(ay - by, 2));
    }

    public static <T> T throwSilently(ThrowingSupplier<T> func) {
        return throwSilently(func, throwable -> {
        });
    }

    public static BitSet searchMatches(String original, String search) {
        int searchIndex = 0;
        BitSet matches = new BitSet();
        char[] chars = search.toLowerCase().toCharArray();
        String lower = original.toLowerCase();
        for (char aChar : chars) {
            if (searchIndex >= original.length()) {
                matches.clear();
                return matches;
            }
            int index;
            if ((index = lower.substring(searchIndex).indexOf(aChar)) >= 0) {
                matches.set(searchIndex + index);
                searchIndex += index + 1;
            } else {
                matches.clear();
                return matches;
            }
        }
        return matches;
    }

    public static <T> T throwSilently(ThrowingSupplier<T> func, Consumer<Throwable> errorHandler) {
        try {
            return func.get();
        } catch (Throwable t) {
            errorHandler.accept(t);
            return null;
        }
    }

    @SafeVarargs
    public static <T> T firstMatching(Function<T, Boolean> func, T... elements) {
        return Arrays.stream(elements).filter(func::apply).findFirst().orElse(null);
    }

    @SafeVarargs
    public static <T> T firstNonNull(T... elements) {
        return firstMatching(Objects::nonNull, elements);
    }

    private static String recursiveToString(Object o) {
        String s = o.toString();
        if (s.split("@").length == 2) { // a.b.c@abcdef, default impl
            return forceToString(o);
        } else {
            return s;
        }
    }

    public static String forceToString(Object o) {
        return forceToString(o.getClass().getSimpleName(), o);
    }

    public static String forceToString(String classname, Object o) {
        StringBuilder props = new StringBuilder();
        Class<?> dumpingClass = o.getClass();
        while (dumpingClass != null) {
            try {
                dumpFieldsToSb(props, o, dumpingClass);
                dumpingClass = dumpingClass.getSuperclass();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String s = props.substring(0, java.lang.Math.max(props.length() - 1, 0));
        return String.format("%s{%s}", classname, s);
    }

    private static void dumpFieldsToSb(StringBuilder sb, Object o, Class<?> c) throws IllegalAccessException {
        for (Field declaredField : c.getDeclaredFields()) {
            declaredField.setAccessible(true);
            sb.append(declaredField.getName()).append("=").append(recursiveToString(declaredField.get(o))).append(",");
        }
    }

    public static void throwIfAnyEquals(String message, Object ifEquals, Object... toCheck) {
        for (Object o : toCheck) {
            if (o == ifEquals) {
                throw new IllegalArgumentException(message);
            }
        }
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {
        }
    }

    public static void sendPacketNoEvent(Packet<?> packet) {
        sendPackets = false;
        MeteorClient.client.player.networkHandler.sendPacket(packet);
        sendPackets = true;
    }

    public static Color getCurrentRGB() {
        return Color.getHSBColor((System.currentTimeMillis() % 4750) / 4750f, 0.5f, 1);
    }

    public static Vec3d getInterpolatedEntityPosition(Entity entity) {
        Vec3d a = entity.getPos();
        Vec3d b = new Vec3d(entity.prevX, entity.prevY, entity.prevZ);
        float p = MeteorClient.client.getTickDelta();
        return new Vec3d(MathHelper.lerp(p, b.x, a.x), MathHelper.lerp(p, b.y, a.y), MathHelper.lerp(p, b.z, a.z));
    }

    public static void registerBufferedImageTexture(Identifier i, BufferedImage bi) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            byte[] bytes = baos.toByteArray();
            registerTexture(i, bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ColoredTextSegment getContent(Text text) {
        List<ColoredTextSegment> segments = new ArrayList<>();
        text.visit((style, asString) -> {
            TextColor color = style.getColor();
            Color rgb = new Color(color == null ? 0xFFFFFF : color.getRgb());
            segments.add(new ColoredTextSegment(new ColoredTextSegment[0],
                asString,
                rgb.getRed() / 255f,
                rgb.getGreen() / 255f,
                rgb.getBlue() / 255f,
                rgb.getAlpha() / 255f));
            return Optional.empty();
        }, Style.EMPTY);
        return new ColoredTextSegment(segments.toArray(ColoredTextSegment[]::new), "", 1f, 1f, 1f, 1f);
    }

    public static ColoredTextSegment getContent(OrderedText text, boolean trimStart, boolean trimEnd) {
        List<ColoredTextSegment> children = new ArrayList<>();
        List<Map.Entry<String, Style>> l = new ArrayList<>();
        text.accept((index, style, codePoint) -> {
            if (trimStart && l.isEmpty() && codePoint == ' ') {
                return true;
            }
            l.add(new AbstractMap.SimpleEntry<>(String.valueOf((char) codePoint), style));
            return true;
        });
        int i = 1;
        while (i < l.size()) {
            Map.Entry<String, Style> first = l.get(i - 1);
            Map.Entry<String, Style> second = l.get(i);
            if (first.getValue().equals(second.getValue())) {
                l.remove(i);
                l.remove(i - 1);
                l.add(i - 1, new AbstractMap.SimpleEntry<>(first.getKey() + second.getKey(), first.getValue()));
            } else {
                i++;
            }
        }
        for (Map.Entry<String, Style> stringStyleEntry : l) {
            TextColor color = stringStyleEntry.getValue().getColor();
            Color c = new Color(color != null ? color.getRgb() : 0xFFFFFF);
            children.add(new ColoredTextSegment(new ColoredTextSegment[0],
                stringStyleEntry.getKey(),
                c.getRed() / 255f,
                c.getGreen() / 255f,
                c.getBlue() / 255f,
                c.getAlpha() / 255f));
        }
        return new ColoredTextSegment(children.toArray(ColoredTextSegment[]::new), "", 1f, 1f, 1f, 1f);
    }

    public static void registerTexture(Identifier i, byte[] content) {
        try {
            ByteBuffer data = BufferUtils.createByteBuffer(content.length).put(content);
            data.flip();
            NativeImageBackedTexture tex = new NativeImageBackedTexture(NativeImage.read(data));
            MeteorClient.client.execute(() -> MeteorClient.client.getTextureManager().registerTexture(i, tex));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String[] splitLinesToWidth(String input, double maxWidth, FontAdapter rendererUsed) {
        List<String> dSplit = List.of(input.split("\n"));
        List<String> splits = new ArrayList<>();
        for (String s : dSplit) {
            List<String> splitContent = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String c : s.split(" ")) {
                if (rendererUsed.getStringWidth(line + c) >= maxWidth) {
                    splitContent.add(line.toString().trim());
                    line = new StringBuilder();
                }
                line.append(c).append(" ");
            }
            splitContent.add(line.toString().trim());
            splits.addAll(splitContent);
        }
        return splits.toArray(new String[0]);
    }

    public static String capAtLength(String input, double maxWidth, FontAdapter rendererUsed) {
        String suffix = "...";
        StringBuilder constructed = new StringBuilder();
        for (char c : input.toCharArray()) {
            constructed.append(c);
            if (rendererUsed.getStringWidth(constructed + suffix) >= maxWidth) {
                constructed.deleteCharAt(constructed.length() - 1);
                return constructed + suffix;
            }
        }
        return input;
    }

    public static ItemStack generateItemStackWithMeta(String nbt, Item item) {
        try {
            ItemStack stack = new ItemStack(item);
            stack.setNbt(StringNbtReader.parse(nbt));
            return stack;
        } catch (Exception ignored) {
            return new ItemStack(item);
        }
    }

    public interface ThrowingSupplier<T> {
        T get() throws Throwable;
    }

    public static class Inventory {

        public static int slotIndexToId(int index) {
            int translatedSlotId;
            if (index >= 0 && index < 9) {
                translatedSlotId = 36 + index;
            } else {
                translatedSlotId = index;
            }
            return translatedSlotId;
        }

        public static void drop(int index) {
            int translatedSlotId = slotIndexToId(index);
            Objects.requireNonNull(MeteorClient.client.interactionManager)
                .clickSlot(Objects.requireNonNull(MeteorClient.client.player).currentScreenHandler.syncId,
                    translatedSlotId,
                    1,
                    SlotActionType.THROW,
                    MeteorClient.client.player);
        }

        public static void moveStackToOther(int slotIdFrom, int slotIdTo) {
            Objects.requireNonNull(MeteorClient.client.interactionManager)
                .clickSlot(0, slotIdFrom, 0, SlotActionType.PICKUP, MeteorClient.client.player); // pick up item from stack
            MeteorClient.client.interactionManager.clickSlot(0, slotIdTo, 0, SlotActionType.PICKUP, MeteorClient.client.player); // put item to target
            MeteorClient.client.interactionManager.clickSlot(0,
                slotIdFrom,
                0,
                SlotActionType.PICKUP,
                MeteorClient.client.player); // (in case target slot had item) put item from target back to from
        }
    }

    public static class Math {

        public static double roundToDecimal(double n, int point) {
            if (point == 0) {
                return java.lang.Math.floor(n);
            }
            double factor = java.lang.Math.pow(10, point);
            return java.lang.Math.round(n * factor) / factor;
        }

        public static int tryParseInt(String input, int defaultValue) {
            try {
                return Integer.parseInt(input);
            } catch (Exception ignored) {
                return defaultValue;
            }
        }

        public static Vec3d getRotationVector(float pitch, float yaw) {
            float f = pitch * 0.017453292F;
            float g = -yaw * 0.017453292F;
            float h = MathHelper.cos(g);
            float i = MathHelper.sin(g);
            float j = MathHelper.cos(f);
            float k = MathHelper.sin(f);
            return new Vec3d(i * j, -k, h * j);
        }

        public static boolean isABObstructed(Vec3d a, Vec3d b, World world, Entity requester) {
            RaycastContext rcc = new RaycastContext(a, b, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, requester);
            BlockHitResult bhr = world.raycast(rcc);
            return !bhr.getPos().equals(b);
        }
    }

    public static class Mouse {

        public static double getMouseX() {
            return MeteorClient.client.mouse.getX() / MeteorClient.client.getWindow().getScaleFactor();
        }

        public static double getMouseY() {
            return MeteorClient.client.mouse.getY() / MeteorClient.client.getWindow().getScaleFactor();
        }
    }

    public static class Players {

        public static boolean isPlayerNameValid(String name) {
            if (name.length() < 3 || name.length() > 16) {
                return false;
            }
            String valid = "abcdefghijklmnopqrstuvwxyz0123456789_";
            boolean isValidEntityName = true;
            for (char c : name.toLowerCase().toCharArray()) {
                if (!valid.contains(c + "")) {
                    isValidEntityName = false;
                    break;
                }
            }
            return isValidEntityName;
        }

        public static int[] decodeUUID(UUID uuid) {
            long sigLeast = uuid.getLeastSignificantBits();
            long sigMost = uuid.getMostSignificantBits();
            return new int[] { (int) (sigMost >> 32), (int) sigMost, (int) (sigLeast >> 32), (int) sigLeast };
        }
    }
}
