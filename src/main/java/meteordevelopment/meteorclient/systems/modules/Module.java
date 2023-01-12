/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.sound.MeteorSoundManager;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.NotificationUtil;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.Packet;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

public abstract class Module implements ISerializable<Module>, Comparable<Module> {
    protected final MinecraftClient mc;
    public final MathHelper mcMath = new MathHelper();
    public final RenderUtils gameRender = RenderUtils.instance;
    public Random soundRandom = SoundInstance.createRandom();
    public final Category category;
    public final String name;
    public final String title;
    public final MeteorSoundManager soundManager = new MeteorSoundManager();
    public final String description;
    public final Color color;
    public final NotificationUtil notification = NotificationUtil.instance;
    public final File moduleFolder = new File(MeteorClient.FOLDER,"module-file");

    public final Settings settings = new Settings();

    private boolean active;

    public boolean serialize = true;
    public boolean runInMainMenu = false;
    public boolean autoSubscribe = true;

    public final Keybind keybind = Keybind.none();
    public boolean toggleOnBindRelease = false;
    public boolean chatFeedback = true;
    public boolean favorite = false;

    public Module(Category category, String name, String description) {
        this.mc = MinecraftClient.getInstance();
        this.category = category;
        this.name = name;
        this.title = Utils.nameToTitle(name);
        this.description = description;
        this.color = Color.fromHsv(Utils.random(0.0, 360.0), 0.35, 1);

        if (!moduleFolder.exists()) {
            moduleFolder.mkdir();
        }
    }

    public void sendChatMessage(String message) {
        ChatUtils.sendPlayerMsg(message);
    }

    public void sendPacket(Packet packet) {
        mc.getNetworkHandler().sendPacket(packet);
    }

    public void attackEntity(Entity entity) {
        assert mc.interactionManager != null;
        mc.interactionManager.attackEntity(mc.player,entity);
    }

    public WWidget getWidget(GuiTheme theme) {
        return null;
    }

    public void onActivate() {}
    public void onDeactivate() {}

    public void toggle() {
        if (active) {
            if (runInMainMenu || Utils.canUpdate()) {
                if (autoSubscribe) MeteorClient.EVENT_BUS.unsubscribe(this);
                onDeactivate();
            }

            active = false;
            Modules.get().removeActive(this);

            if (Config.get().toggleSound.get()) soundManager.disableSound.play();
        } else {
            active = true;
            Modules.get().addActive(this);

            // "error" not error
            if (Config.get().toggleSound.get()) soundManager.enableSound.play();

            settings.onActivated();

            if (runInMainMenu || Utils.canUpdate()) {
                if (autoSubscribe) MeteorClient.EVENT_BUS.subscribe(this);
                onActivate();
            }
        }
    }

    public void sendToggledMsg() {
        if (Config.get().chatFeedback.get() && chatFeedback) {
            ChatUtils.forceNextPrefixClass(getClass());
            ChatUtils.sendMsg(this.hashCode(), Formatting.GRAY, "Toggled (highlight)%s(default) %s(default).", title, isActive() ? Formatting.GREEN + "on" : Formatting.RED + "off");
        }
    }

    public void info(Text message) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.sendMsg(title, message);
    }

    public void info(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.info(title, message, args);
    }

    public void warning(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.warning(title, message, args);
    }

    public void error(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.error(title, message, args);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean bool) {
        this.active = bool;
    }

    public String getInfoString() {
        return null;
    }

    @Override
    public NbtCompound toTag() {
        if (!serialize) return null;
        NbtCompound tag = new NbtCompound();

        tag.putString("name", name);
        tag.put("keybind", keybind.toTag());
        tag.putBoolean("toggleOnKeyRelease", toggleOnBindRelease);
        tag.putBoolean("chatFeedback", chatFeedback);
        tag.putBoolean("favorite", favorite);
        tag.put("settings", settings.toTag());

        tag.putBoolean("active", active);

        return tag;
    }

    @Override
    public Module fromTag(NbtCompound tag) {
        // General
        if (tag.contains("key")) keybind.set(true, tag.getInt("key"));
        else keybind.fromTag(tag.getCompound("keybind"));

        toggleOnBindRelease = tag.getBoolean("toggleOnKeyRelease");
        chatFeedback = !tag.contains("chatFeedback") || tag.getBoolean("chatFeedback");
        favorite = tag.getBoolean("favorite");

        // Settings
        NbtElement settingsTag = tag.get("settings");
        if (settingsTag instanceof NbtCompound) settings.fromTag((NbtCompound) settingsTag);

        boolean active = tag.getBoolean("active");
        if (active != isActive()) toggle();

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Module module = (Module) o;
        return Objects.equals(name, module.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(@NotNull Module o) {
        return name.compareTo(o.name);
    }
}
