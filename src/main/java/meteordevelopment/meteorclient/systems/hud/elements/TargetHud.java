/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.renderer.text.CustomTextRenderer;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class TargetHud extends HudElement {
    public static final HudElementInfo<TargetHud> INFO = new HudElementInfo<>(Hud.GROUP, "target-hud", "Displays information about your combat target.", TargetHud::new);

    public TargetHud() {
        super(INFO);
    }

    private List<PlayerEntity> targets = new ArrayList<>();
    public float hue;
    Color customColor;
    float astolfoHelathAnim = 0f;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .defaultValue(Mode.Meteor)
        .build()
    );

    private final Setting<SettingColor> healthColor = sgGeneral.add(new ColorSetting.Builder()
        .name("health-color")
        .description("The color on the left of the health gradient.")
        .defaultValue(new SettingColor(255, 15, 15))
        .build()
    );

    private final Setting<StackMode> stackModeSetting = sgGeneral.add(new EnumSetting.Builder<StackMode>()
        .name("stack-mode")
        .defaultValue(StackMode.Down)
        .build()
    );


    public enum StackMode {
        Up,
        Down
    }

    public enum Mode {
        Meteor,
        Astolfo
    }

    @Override
    public void tick(HudRenderer renderer) {
        this.targets = Modules.get().get(KillAura.class).getTargets();
        super.tick(renderer);
    }

    @Override
    public void render(HudRenderer renderer) {
        renderer.post(() -> {
            double boxX = this.x;
            double boxY = this.y;
            double offsetY = 0;
            for (PlayerEntity player : targets) {
                assert player != null;
                if (mode.get().equals(Mode.Meteor)) {
                    TextRenderer font = TextRenderer.get();
                    double bgY = boxY + offsetY;
                    double nameWidth = gameRender.getStringWidth(player.getName().getString(), 1);
                    double nameHeight = gameRender.getStringHeight(false, 1);
                    double bgWidth = 5 + 60 + 80 + nameWidth;
                    double bgHeight = 5 + 60 + 5;
                    float absorption = player.getAbsorptionAmount();
                    int health = Math.round(player.getHealth() + absorption);
                    double healthPercent = health / (player.getMaxHealth() + absorption);
                    double hpWidth = ((bgWidth - (5 + 60 + 5)) - 5) * healthPercent;

                    gameRender.drawRect(boxX, bgY, bgWidth, bgHeight, new Color(0, 0, 0, 153));
                    setSize(bgWidth, bgHeight);
                    gameRender.drawEntity(player,(int) boxX+35,(int) (bgY+bgHeight -5),31);
                    String pName;
                    if (player.getDisplayName().getString().startsWith("§")) {
                        if (player.getDisplayName().getString().charAt(1) == '1' ||
                            player.getDisplayName().getString().charAt(1) == '2' ||
                            player.getDisplayName().getString().charAt(1) == '3' ||
                            player.getDisplayName().getString().charAt(1) == '4' ||
                            player.getDisplayName().getString().charAt(1) == '5' ||
                            player.getDisplayName().getString().charAt(1) == '6' ||
                            player.getDisplayName().getString().charAt(1) == '7' ||
                            player.getDisplayName().getString().charAt(1) == '8' ||
                            player.getDisplayName().getString().charAt(1) == '9' ||
                            player.getDisplayName().getString().charAt(1) == '0' ||
                            player.getDisplayName().getString().charAt(1) == 'a' ||
                            player.getDisplayName().getString().charAt(1) == 'b' ||
                            player.getDisplayName().getString().charAt(1) == 'c' ||
                            player.getDisplayName().getString().charAt(1) == 'd' ||
                            player.getDisplayName().getString().charAt(1) == 'e' ||
                            player.getDisplayName().getString().charAt(1) == 'f'
                        ) {
                            pName = player.getDisplayName().getString().replace("§" + player.getDisplayName().getString().charAt(1), "");
                        } else {
                            pName = player.getDisplayName().getString().replace("§", "");
                        }
                    } else {
                        pName = player.getDisplayName().getString();
                    }
                    font.render(pName, boxX + 70, bgY + 5, Color.WHITE);
                    font.render(health + "HP", boxX + 70, bgY + 6 + nameHeight, Color.WHITE, 0.7);

                    gameRender.drawRect(boxX + 5 + 65 - 1, bgY + bgHeight - 5 - 10 - 1, bgWidth - 70 - 5, 11, new Color(0, 0, 0, healthColor.get().a));
                    gameRender.drawRect(boxX + 5 + 65, bgY + bgHeight - 5 - 10, hpWidth, 10, healthColor.get());

                    if (stackModeSetting.get().equals(StackMode.Down)) {
                        offsetY += bgHeight + 5;
                    }
                    if (stackModeSetting.get().equals(StackMode.Up)) {
                        offsetY -= bgHeight + 5;
                    }
                }
            }
        });
        super.render(renderer);
    }
}
