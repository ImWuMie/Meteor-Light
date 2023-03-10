/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import kotlin.jvm.internal.Intrinsics;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.WindowResizedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.*;
import meteordevelopment.meteorclient.renderer.text.CFont;
import meteordevelopment.meteorclient.renderer.text.TTFFontRender;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.NameProtect;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.listeners.ConsumerListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class Nametags extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgItems = settings.createGroup("Items");
    private final SettingGroup sgJello = settings.createGroup("Jello");

    // General

    private final Setting<RenderMode> mode = sgGeneral.add(new EnumSetting.Builder<RenderMode>()
        .name("mode")
        .defaultValue(RenderMode.Jello)
        .build()
    );

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Select entities to draw nametags on.")
        .defaultValue(EntityType.PLAYER, EntityType.ITEM)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the nametag.")
        .defaultValue(1.5)
        .min(0.1)
        .build()
    );

    private final Setting<Boolean> yourself = sgGeneral.add(new BoolSetting.Builder()
        .name("self")
        .description("Displays a nametag on your player if you're in Freecam.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> background = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description("The color of the nametag background.")
        .defaultValue(new SettingColor(0, 0, 0, 75))
        .build()
    );

    private final Setting<SettingColor> names = sgGeneral.add(new ColorSetting.Builder()
        .name("primary-color")
        .description("The color of the nametag names.")
        .defaultValue(new SettingColor())
        .build()
    );

    private final Setting<Boolean> culling = sgGeneral.add(new BoolSetting.Builder()
        .name("culling")
        .description("Only render a certain number of nametags at a certain distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> maxCullRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("culling-range")
        .description("Only render nametags within this distance of your player.")
        .defaultValue(20)
        .min(0)
        .sliderMax(200)
        .visible(culling::get)
        .build()
    );

    private final Setting<Integer> maxCullCount = sgGeneral.add(new IntSetting.Builder()
        .name("culling-count")
        .description("Only render this many nametags.")
        .defaultValue(50)
        .min(1)
        .sliderRange(1, 100)
        .visible(culling::get)
        .build()
    );

    //Players

    private final Setting<Boolean> displayItems = sgPlayers.add(new BoolSetting.Builder()
        .name("display-items")
        .description("Displays armor and hand items above the name tags.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> itemSpacing = sgPlayers.add(new DoubleSetting.Builder()
        .name("item-spacing")
        .description("The spacing between items.")
        .defaultValue(2)
        .range(0, 10)
        .visible(displayItems::get)
        .build()
    );

    private final Setting<Boolean> ignoreEmpty = sgPlayers.add(new BoolSetting.Builder()
        .name("ignore-empty-slots")
        .description("Doesn't add spacing where an empty item stack would be.")
        .defaultValue(true)
        .visible(displayItems::get)
        .build()
    );

    private final Setting<Boolean> displayItemEnchants = sgPlayers.add(new BoolSetting.Builder()
        .name("display-enchants")
        .description("Displays item enchantments on the items.")
        .defaultValue(true)
        .visible(displayItems::get)
        .build()
    );

    private final Setting<Position> enchantPos = sgPlayers.add(new EnumSetting.Builder<Position>()
        .name("enchantment-position")
        .description("Where the enchantments are rendered.")
        .defaultValue(Position.Above)
        .visible(displayItemEnchants::get)
        .build()
    );

    private final Setting<Integer> enchantLength = sgPlayers.add(new IntSetting.Builder()
        .name("enchant-name-length")
        .description("The length enchantment names are trimmed to.")
        .defaultValue(3)
        .range(1, 5)
        .sliderRange(1, 5)
        .visible(displayItemEnchants::get)
        .build()
    );

    private final Setting<List<Enchantment>> ignoredEnchantments = sgPlayers.add(new EnchantmentListSetting.Builder()
        .name("ignored-enchantments")
        .description("The enchantments that aren't shown on nametags.")
        .visible(displayItemEnchants::get)
        .build()
    );

    private final Setting<Double> enchantTextScale = sgPlayers.add(new DoubleSetting.Builder()
        .name("enchant-text-scale")
        .description("The scale of the enchantment text.")
        .defaultValue(1)
        .range(0.1, 2)
        .sliderRange(0.1, 2)
        .visible(displayItemEnchants::get)
        .build()
    );

    private final Setting<Boolean> displayGameMode = sgPlayers.add(new BoolSetting.Builder()
        .name("gamemode")
        .description("Shows the player's GameMode.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> displayPing = sgPlayers.add(new BoolSetting.Builder()
        .name("ping")
        .description("Shows the player's ping.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> displayDistance = sgPlayers.add(new BoolSetting.Builder()
        .name("distance")
        .description("Shows the distance between you and the player.")
        .defaultValue(true)
        .build()
    );

    //Items

    private final Setting<Boolean> itemCount = sgItems.add(new BoolSetting.Builder()
        .name("show-count")
        .description("Displays the number of items in the stack.")
        .defaultValue(true)
        .build()
    );

    // Jello
/*
    private final Setting<Boolean> jelloBlur = sgJello.add(new BoolSetting.Builder()
        .name("Blur")
        .defaultValue(false)
        .build()
    );*/


    private final Color WHITE = new Color(255, 255, 255);
    private final Color RED = new Color(255, 25, 25);
    private final Color AMBER = new Color(255, 105, 25);
    private final Color GREEN = new Color(25, 252, 25);
    private final Color GOLD = new Color(232, 185, 35);
    private final Color GREY = new Color(150, 150, 150);
    private final Color BLUE = new Color(20, 170, 170);

    private final Vec3 pos = new Vec3();
    private final double[] itemWidths = new double[6];

    public enum RenderMode {
        Meteor,
        Jello,
        Other
    }

    private final List<Entity> entityList = new ArrayList<>();

    private Shader shader;
    private Framebuffer fbo1, fbo2;
    private boolean enabled;
    private long fadeEndAt;

    private int blurWidth = 0,blurHeight = 0;

    public Nametags() {
        super(Categories.Render, "nametags", "Displays customizable nametags above players.");
    }

    @Override
    public void onActivate() {
        /*
        if (mode.get().equals(RenderMode.Jello)) {
            if (jelloBlur.get()) {
                MeteorClient.EVENT_BUS.subscribe(new ConsumerListener<>(WindowResizedEvent.class, event -> {
                    if (fbo1 != null) {
                        if (blurHeight != 0 && blurWidth != 0) {
                            fbo1.resize(blurWidth, blurHeight);
                            fbo2.resize(blurWidth, blurHeight);
                        }
                    }
                }));
            }
        }*/
        super.onActivate();
    }

    private static String ticksToTime(int ticks) {
        if (ticks > 20 * 3600) {
            int h = ticks / 20 / 3600;
            return h + " h";
        } else if (ticks > 20 * 60) {
            int m = ticks / 20 / 60;
            return m + " m";
        } else {
            int s = ticks / 20;
            int ms = (ticks % 20) / 2;
            return s + "." + ms + " s";
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        entityList.clear();

        boolean freecamNotActive = !Modules.get().isActive(Freecam.class);
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        for (Entity entity : mc.world.getEntities()) {
            EntityType<?> type = entity.getType();
            if (!entities.get().containsKey(type)) continue;

            if (type == EntityType.PLAYER) {
                if ((!yourself.get() || freecamNotActive) && entity == mc.player) continue;
            }

            if (!culling.get() || entity.getPos().distanceTo(cameraPos) < maxCullRange.get()) {
                entityList.add(entity);
            }
        }

        entityList.sort(Comparator.comparing(e -> e.squaredDistanceTo(cameraPos)));
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        int count = getRenderCount();
        boolean shadow = Config.get().customFont.get();

        for (int i = count - 1; i > -1; i--) {
            Entity entity = entityList.get(i);

            pos.set(entity, event.tickDelta);
            pos.add(0, getHeight(entity), 0);

            EntityType<?> type = entity.getType();

            if (NametagUtils.to2D(pos, scale.get())) {
                if (mode.get().equals(RenderMode.Meteor)) {
                    if (type == EntityType.PLAYER) renderNametagPlayer((PlayerEntity) entity, shadow);
                    else if (type == EntityType.ITEM) renderNametagItem(((ItemEntity) entity).getStack(), shadow);
                    else if (type == EntityType.ITEM_FRAME)
                        renderNametagItem(((ItemFrameEntity) entity).getHeldItemStack(), shadow);
                    else if (type == EntityType.TNT) renderTntNametag((TntEntity) entity, shadow);
                    else if (entity instanceof LivingEntity) renderGenericNametag((LivingEntity) entity, shadow);
                }
                if (mode.get().equals(RenderMode.Jello)) {
                    if (entity instanceof LivingEntity) renderJello((LivingEntity) entity,shadow);
                }
            }
        }
    }

    private int getRenderCount() {
        int count = culling.get() ? maxCullCount.get() : entityList.size();
        count = MathHelper.clamp(count, 0, entityList.size());

        return count;
    }

    @Override
    public String getInfoString() {
        return Integer.toString(getRenderCount());
    }

    private double getHeight(Entity entity) {
        double height = entity.getEyeHeight(entity.getPose());

        if (entity.getType() == EntityType.ITEM || entity.getType() == EntityType.ITEM_FRAME) height += 0.2;
        else height += 0.5;

        return height;
    }

    private void renderNametagPlayer(PlayerEntity player, boolean shadow) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        // Gamemode
        GameMode gm = EntityUtils.getGameMode(player);
        String gmText = "BOT";
        if (gm != null) {
            gmText = switch (gm) {
                case SPECTATOR -> "Sp";
                case SURVIVAL -> "S";
                case CREATIVE -> "C";
                case ADVENTURE -> "A";
            };
        }

        gmText = "[" + gmText + "] ";

        // Name
        String name;
        Color nameColor = PlayerUtils.getPlayerColor(player, names.get());

        if (player == mc.player) name = Modules.get().get(NameProtect.class).getName(player.getEntityName());
        else name = player.getEntityName();

        name = name + " ";

        // Health
        float absorption = player.getAbsorptionAmount();
        int health = Math.round(player.getHealth() + absorption);
        double healthPercentage = health / (player.getMaxHealth() + absorption);

        String healthText = String.valueOf(health);
        Color healthColor;

        if (healthPercentage <= 0.333) healthColor = RED;
        else if (healthPercentage <= 0.666) healthColor = AMBER;
        else healthColor = GREEN;

        // Ping
        int ping = EntityUtils.getPing(player);
        String pingText = " [" + ping + "ms]";

        // Distance
        double dist = Math.round(PlayerUtils.distanceToCamera(player) * 10.0) / 10.0;
        String distText = " " + dist + "m";

        // Calc widths
        double gmWidth = text.getWidth(gmText, shadow);
        double nameWidth = text.getWidth(name, shadow);
        double healthWidth = text.getWidth(healthText, shadow);
        double pingWidth = text.getWidth(pingText, shadow);
        double distWidth = text.getWidth(distText, shadow);
        double width = nameWidth + healthWidth;

        if (displayGameMode.get()) width += gmWidth;
        if (displayPing.get()) width += pingWidth;
        if (displayDistance.get()) width += distWidth;

        double widthHalf = width / 2;
        double heightDown = text.getHeight(shadow);

        drawBg(-widthHalf, -heightDown, width, heightDown);

        // Render texts
        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        if (displayGameMode.get()) hX = text.render(gmText, hX, hY, GOLD, shadow);
        hX = text.render(name, hX, hY, nameColor, shadow);

        hX = text.render(healthText, hX, hY, healthColor, shadow);
        if (displayPing.get()) hX = text.render(pingText, hX, hY, BLUE, shadow);
        if (displayDistance.get()) text.render(distText, hX, hY, GREY, shadow);
        text.end();

        if (displayItems.get()) {
            // Item calc
            Arrays.fill(itemWidths, 0);
            boolean hasItems = false;
            int maxEnchantCount = 0;

            for (int i = 0; i < 6; i++) {
                ItemStack itemStack = getItem(player, i);

                // Setting up widths
                if (itemWidths[i] == 0 && (!ignoreEmpty.get() || !itemStack.isEmpty()))
                    itemWidths[i] = 32 + itemSpacing.get();

                if (!itemStack.isEmpty()) hasItems = true;

                if (displayItemEnchants.get()) {
                    Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(itemStack);

                    int size = 0;
                    for (var enchantment : enchantments.keySet()) {
                        if (ignoredEnchantments.get().contains(enchantment)) continue;
                        String enchantName = Utils.getEnchantSimpleName(enchantment, enchantLength.get()) + " " + enchantments.get(enchantment);
                        itemWidths[i] = Math.max(itemWidths[i], (text.getWidth(enchantName, shadow) / 2));
                        size++;
                    }

                    maxEnchantCount = Math.max(maxEnchantCount, size);
                    maxEnchantCount = Math.max(maxEnchantCount, size);
                }
            }

            double itemsHeight = (hasItems ? 32 : 0);
            double itemWidthTotal = 0;
            for (double w : itemWidths) itemWidthTotal += w;
            double itemWidthHalf = itemWidthTotal / 2;

            double y = -heightDown - 7 - itemsHeight;
            double x = -itemWidthHalf;

            // Rendering items and enchants
            for (int i = 0; i < 6; i++) {
                ItemStack stack = getItem(player, i);

                gameRender.drawItem(stack, (int) x, (int) y, 2, true);

                if (maxEnchantCount > 0 && displayItemEnchants.get()) {
                    text.begin(0.5 * enchantTextScale.get(), false, true);

                    Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
                    Map<Enchantment, Integer> enchantmentsToShow = new HashMap<>();

                    for (Enchantment enchantment : enchantments.keySet()) {
                        if (!ignoredEnchantments.get().contains(enchantment)) {
                            enchantmentsToShow.put(enchantment, enchantments.get(enchantment));
                        }
                    }

                    double aW = itemWidths[i];
                    double enchantY = 0;

                    double addY = switch (enchantPos.get()) {
                        case Above -> -((enchantmentsToShow.size() + 1) * text.getHeight(shadow));
                        case OnTop -> (itemsHeight - enchantmentsToShow.size() * text.getHeight(shadow)) / 2;
                    };

                    double enchantX;

                    for (Enchantment enchantment : enchantmentsToShow.keySet()) {
                        String enchantName = Utils.getEnchantSimpleName(enchantment, enchantLength.get()) + " " + enchantmentsToShow.get(enchantment);

                        Color enchantColor = WHITE;
                        if (enchantment.isCursed()) enchantColor = RED;

                        enchantX = switch (enchantPos.get()) {
                            case Above -> x + (aW / 2) - (text.getWidth(enchantName, shadow) / 2);
                            case OnTop -> x + (aW - text.getWidth(enchantName, shadow)) / 2;
                        };

                        text.render(enchantName, enchantX, y + addY + enchantY, enchantColor, shadow);

                        enchantY += text.getHeight(shadow);
                    }

                    text.end();
                }

                x += itemWidths[i];
            }
        } else if (displayItemEnchants.get()) displayItemEnchants.set(false);

        NametagUtils.end();
    }

    private void renderNametagItem(ItemStack stack, boolean shadow) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        String name = stack.getName().getString();
        String count = " x" + stack.getCount();

        double nameWidth = text.getWidth(name, shadow);
        double countWidth = text.getWidth(count, shadow);
        double heightDown = text.getHeight(shadow);

        double width = nameWidth;
        if (itemCount.get()) width += countWidth;
        double widthHalf = width / 2;

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        hX = text.render(name, hX, hY, names.get(), shadow);
        if (itemCount.get()) text.render(count, hX, hY, GOLD, shadow);
        text.end();

        NametagUtils.end();
    }

    private void renderGenericNametag(LivingEntity entity, boolean shadow) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        //Name
        String nameText = entity.getType().getName().getString();
        nameText += " ";

        //Health
        float absorption = entity.getAbsorptionAmount();
        int health = Math.round(entity.getHealth() + absorption);
        double healthPercentage = health / (entity.getMaxHealth() + absorption);

        String healthText = String.valueOf(health);
        Color healthColor;

        if (healthPercentage <= 0.333) healthColor = RED;
        else if (healthPercentage <= 0.666) healthColor = AMBER;
        else healthColor = GREEN;

        double nameWidth = text.getWidth(nameText, shadow);
        double healthWidth = text.getWidth(healthText, shadow);
        double heightDown = text.getHeight(shadow);

        double width = nameWidth + healthWidth;
        double widthHalf = width / 2;

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        hX = text.render(nameText, hX, hY, names.get(), shadow);
        text.render(healthText, hX, hY, healthColor, shadow);
        text.end();

        NametagUtils.end();
    }

    private void renderTntNametag(TntEntity entity, boolean shadow) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        String fuseText = ticksToTime(entity.getFuse());

        double width = text.getWidth(fuseText, shadow);
        double heightDown = text.getHeight(shadow);

        double widthHalf = width / 2;

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        text.render(fuseText, hX, hY, names.get(), shadow);
        text.end();

        NametagUtils.end();
    }

    private ItemStack getItem(PlayerEntity entity, int index) {
        return switch (index) {
            case 0 -> entity.getMainHandStack();
            case 1 -> entity.getInventory().armor.get(3);
            case 2 -> entity.getInventory().armor.get(2);
            case 3 -> entity.getInventory().armor.get(1);
            case 4 -> entity.getInventory().armor.get(0);
            case 5 -> entity.getOffHandStack();
            default -> ItemStack.EMPTY;
        };
    }

    private void renderJello(LivingEntity entity, boolean shadow) {
        TTFFontRender jw = CFont.jelloRegular;
        TTFFontRender hw = CFont.googleSans;
        TTFFontRender nameFont = CFont.jelloRegular;
        TTFFontRender healthFont = CFont.googleSans;
        double nameFontScale = 0.9;
        double healthFontScale = 0.65;

        NametagUtils.begin(pos);

        String entityName;

        Color healthBarColor;
        if (entity.getDisplayName().getString().startsWith("??")) {
            healthBarColor = getColorForString(entity.getDisplayName().getString().substring(1,2),170);
            if (entity.getDisplayName().getString().charAt(1) == '1' ||
                entity.getDisplayName().getString().charAt(1) == '2' ||
                entity.getDisplayName().getString().charAt(1) == '3' ||
                entity.getDisplayName().getString().charAt(1) == '4' ||
                entity.getDisplayName().getString().charAt(1) == '5' ||
                entity.getDisplayName().getString().charAt(1) == '6' ||
                entity.getDisplayName().getString().charAt(1) == '7' ||
                entity.getDisplayName().getString().charAt(1) == '8' ||
                entity.getDisplayName().getString().charAt(1) == '9' ||
                entity.getDisplayName().getString().charAt(1) == '0' ||
                entity.getDisplayName().getString().charAt(1) == 'a' ||
                entity.getDisplayName().getString().charAt(1) == 'b' ||
                entity.getDisplayName().getString().charAt(1) == 'c' ||
                entity.getDisplayName().getString().charAt(1) == 'd' ||
                entity.getDisplayName().getString().charAt(1) == 'e' ||
                entity.getDisplayName().getString().charAt(1) == 'f'
            ) {
                entityName = entity.getDisplayName().getString().replace("??" + entity.getDisplayName().getString().charAt(1), "");
            } else {
                entityName = entity.getDisplayName().getString().replace("??", "");
            }
        }
        else {
            healthBarColor = new Color(255,255,255,170);
            entityName=entity.getDisplayName().getString();
            }
        Color bgColor = new Color(65,65,65,170);

        float absorption = entity.getAbsorptionAmount();
        int health = Math.round(entity.getHealth() + absorption);
        double healthPercent = health / (entity.getMaxHealth() + absorption);

        int healthBarHeight = 2;
        double bgWidth = jw.getWidth(entityName, shadow, nameFontScale) +3;
        double bgY = 1 + jw.getHeight(shadow,nameFontScale) + 1 + hw.getHeight(shadow, healthFontScale) + 1 + healthBarHeight;
        double hpStrWidth = hw.getWidth("Health: " + health, shadow, healthFontScale)+3;

        if (entity instanceof PlayerEntity p) {
            if (Friends.get().isFriend(p)) {
                entityName = entityName + " [F]";
            }
        }

        // draw bg
        if (bgWidth < hpStrWidth) {
            double healthBarWidth =(hpStrWidth * healthPercent);
            double bgX = hpStrWidth / 2;
            double bgBarX = -bgX;
            double bgBarY = -bgY;
            double nameX = bgBarX + 2;
            double healthStringY = bgBarY + jw.getHeight(shadow, nameFontScale) + 1;
            // draw Bg
            drawBg(-bgX,-bgY,hpStrWidth,bgY-2,bgColor);
            // draw Health Bar
            drawBg(bgBarX,(bgBarY + bgY) -2,healthBarWidth,healthBarHeight,healthBarColor);
            // draw Name
            nameFont.render(entityName,nameX, bgBarY-1,Color.WHITE,nameFontScale);
            // draw Health String
            healthFont.render("Health: " + health, nameX,healthStringY,Color.WHITE,healthFontScale);
        } else if (bgWidth >= hpStrWidth) {
            double healthBarWidth = (bgWidth * healthPercent);
            double bgX = bgWidth / 2;
            double bgBarX = -bgX;
            double bgBarY = -bgY;
            double nameX = bgBarX + 2;
            double healthStringY = bgBarY + jw.getHeight(shadow, nameFontScale) + 1;
            // draw Bg
            drawBg(-bgX,-bgY,bgWidth,bgY-2,bgColor);
            // draw Health Bar
            drawBg(bgBarX,(bgBarY + bgY)-2,healthBarWidth,healthBarHeight,healthBarColor);
            // draw Name
            nameFont.render(entityName,nameX, bgBarY-1,Color.WHITE,nameFontScale);
            // draw Health String
            healthFont.render("Health: " + health, nameX,healthStringY,Color.WHITE,healthFontScale);
        }
        NametagUtils.end();
    }

    private void drawBg(double x, double y, double width, double height) {
        gameRender.drawRect(x,y,width,height,background.get());
    }

    private void drawBg(double x, double y, double width, double height,Color color) {
        gameRender.drawRect(x,y,width,height,color);
    }

    public Color getColorForString(String code, int alpha) {
        return gameRender.getColorForString(code,alpha);
    }

    public enum Position {
        Above,
        OnTop
    }
}
