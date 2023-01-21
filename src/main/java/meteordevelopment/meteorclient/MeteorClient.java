/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient;

import com.google.common.collect.Range;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import kaptainwutax.seedcrackerX.api.SeedCrackerAPI;
import ladysnake.satin.api.event.ShaderEffectRenderCallback;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import ladysnake.satin.api.managed.ShaderEffectManager;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.config.SeedConfigs;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.DiscordPresence;
import meteordevelopment.meteorclient.systems.modules.render.MotionBlur;
import meteordevelopment.meteorclient.systems.viaversion.commands.VRCommandHandler;
import meteordevelopment.meteorclient.systems.viaversion.config.VFConfig;
import meteordevelopment.meteorclient.systems.viaversion.platform.FabricInjector;
import meteordevelopment.meteorclient.systems.viaversion.platform.FabricPlatform;
import meteordevelopment.meteorclient.systems.viaversion.platform.VFLoader;
import meteordevelopment.meteorclient.systems.viaversion.protocol.HostnameParserProtocol;
import meteordevelopment.meteorclient.systems.viaversion.util.JLoggerToLog4j;
import meteordevelopment.meteorclient.utils.ReflectInit;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.VectorUtils;
import meteordevelopment.meteorclient.utils.misc.MeteorIdentifier;
import meteordevelopment.meteorclient.utils.misc.Version;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import meteordevelopment.meteorclient.utils.network.OnlinePlayers;
import meteordevelopment.meteorclient.utils.world.seeds.Seeds;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.command.CommandSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.ref.Cleaner;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class MeteorClient implements ClientModInitializer {
    public static final String MOD_ID = "meteor-client";
    public static final Marker MARKER = MarkerManager.getMarker("Meteor-GL Core");
    public static final ModMetadata MOD_META = FabricLoader.getInstance().getModContainer(MOD_ID).get().getMetadata();
    public final static Version VERSION;
    public final static String DEV_BUILD;
    public static boolean disableShaders = false;
    public static boolean vanillaFont = false;
    public static boolean DEVMODE = true;
    public static final MinecraftClient client = MinecraftClient.getInstance();
    public static MeteorAddon ADDON;
    public static final java.util.logging.Logger JLOGGER = new JLoggerToLog4j(LogManager.getLogger("Meteor-Via"));
    public static final ExecutorService ASYNC_EXECUTOR;
    public static final EventLoop EVENT_LOOP;
    public static final CompletableFuture<Void> INIT_FUTURE = new CompletableFuture<>();
    public static VFConfig config;
    public static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger("Meteor Client-GL Render");
    private static final Cleaner cleaner = Cleaner.create();

    public static final ArrayList<SeedCrackerAPI> entrypoints = new ArrayList<>();
   /* private final DataStorage dataStorage = new DataStorage();*/
    public static MinecraftClient mc;
    public static MeteorClient INSTANCE;
    public static final IEventBus EVENT_BUS = new EventBus();
    public static final File FOLDER = new File(FabricLoader.getInstance().getGameDir().toString(), MOD_ID);
    public static final File SOUNDS_FOLODER = new File(FOLDER,"sounds");
    public static final Logger LOG = LoggerFactory.getLogger("Meteor Client");

    private final ManagedShaderEffect motionblur = ShaderEffectManager.getInstance().manage(new MeteorIdentifier("shaders/post/motion_blur.json"),
            shader -> shader.setUniformValue("BlendFactor", getBlur()));

    private float currentBlur;

    public void register() {
        MotionBlur motion = Modules.get().get(MotionBlur.class);

        ShaderEffectRenderCallback.EVENT.register((deltaTick) -> {
            if (motion.blurAmount.get() != 0 && motion.isActive()) {
                if(currentBlur!=getBlur()){
                    motionblur.setUniformValue("BlendFactor", getBlur());
                    currentBlur=getBlur();
                }
                motionblur.render(deltaTick);
            }
        });
    }

    public float getBlur() {
        MotionBlur motion = Modules.get().get(MotionBlur.class);
        return Math.min(motion.blurAmount.get(), 99)/100F;
    }

    public MeteorClient() {
    }

    static {
        String versionString = MOD_META.getVersion().getFriendlyString();
        if (versionString.contains("-")) versionString = versionString.split("-")[0];
        VERSION = new Version(versionString);
        DEV_BUILD = MOD_META.getCustomValue(MeteorClient.MOD_ID + ":devbuild").getAsString();
        ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("Meteor-Via-%d").build();
        ASYNC_EXECUTOR = Executors.newFixedThreadPool(8, factory);
        EVENT_LOOP = new DefaultEventLoop(factory);
        EVENT_LOOP.submit(INIT_FUTURE::join);
    }

    public static MeteorClient get() {
        return INSTANCE;
    }

    public static <S extends CommandSource> LiteralArgumentBuilder<S> command(String commandName) {
        return LiteralArgumentBuilder.<S>literal(commandName)
            .then(
                RequiredArgumentBuilder
                    .<S, String>argument("args", StringArgumentType.greedyString())
                    .executes(((VRCommandHandler) Via.getManager().getCommandHandler())::execute)
                    .suggests(((VRCommandHandler) Via.getManager().getCommandHandler())::suggestion)
            )
            .executes(((VRCommandHandler) Via.getManager().getCommandHandler())::execute);
    }
/*
    private void initSeedCrack() {
        meteordevelopment.meteorclient.systems.seedcrack.config.Config.load();
        Features.init(meteordevelopment.meteorclient.systems.seedcrack.config.Config.get().getVersion());
        FabricLoader.getInstance().getEntrypointContainers("seedcrackerx", SeedCrackerAPI.class).forEach(entrypoint ->
            entrypoints.add(entrypoint.getEntrypoint()));
    }

    public DataStorage getDataStorage() {
        return this.dataStorage;
    }

    public void reset() {
        getDataStorage().clear();
        FinderQueue.get().finderControl.deleteFinders();
    }*/

    private void initViaVersion() {
        FabricPlatform platform = new FabricPlatform();

        Via.init(ViaManagerImpl.builder()
            .injector(new FabricInjector())
            .loader(new VFLoader())
            .commandHandler(new VRCommandHandler())
            .platform(platform).build());

        platform.init();

        FabricLoader.getInstance().getModContainer("viabackwards").ifPresent(mod -> MappingDataLoader.enableMappingsCache());

        ((ViaManagerImpl) Via.getManager()).init();

        Via.getManager().getProtocolManager().registerBaseProtocol(HostnameParserProtocol.INSTANCE, Range.lessThan(Integer.MIN_VALUE));
        ProtocolVersion.register(-2, "AUTO");

        FabricLoader.getInstance().getEntrypoints("viafabric:via_api_initialized", Runnable.class).forEach(Runnable::run);

        File viaFolder = new File(FOLDER, "via-version");

        config = new VFConfig(new File(viaFolder, "meteor-via.yml"));

        INIT_FUTURE.complete(null);
    }

    private void spawnMode() {
        File targetFile = new File(FOLDER,"mode.txt");
        try {
            final PrintWriter pr = new PrintWriter(new FileWriter(targetFile));
            pr.println("// set the options to start.");
            pr.println("DisableShaders:"+(disableShaders ? "true" : "false"));
            pr.println("VanillaFont:"+(vanillaFont ? "true" : "false"));
            pr.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean parseBoolean(String s) {
        return Boolean.parseBoolean(s);
    }

    private void readMode() {
        File targetFile = new File(FOLDER,"mode.txt");
        if (!targetFile.exists()) {
            spawnMode();
            return;
        }

        try {
            final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(targetFile), StandardCharsets.UTF_8));
            String str;
            while ((str = in.readLine()) != null) {
               String[] strings = str.split(":");
               switch (strings[0]) {
                   case "DisableShaders" -> {
                       boolean mode = parseBoolean(strings[1]);
                       disableShaders = mode;
                   }
                   case "VanillaFont" -> {
                       boolean mode = parseBoolean(strings[1]);
                       vanillaFont = mode;
                   }
               }
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInitializeClient() {
        if (INSTANCE == null) {
            INSTANCE = this;
            return;
        }

        readMode();

        LOG.info("Initializing Meteor Client | Options:" + "DisableShaders:"+disableShaders+",VanillaFont:"+vanillaFont);

        // Global minecraft client accessor
        mc = MinecraftClient.getInstance();

        // Pre-load
        if (!FOLDER.exists()) {
            FOLDER.getParentFile().mkdirs();
            FOLDER.mkdir();
            Systems.addPreLoadTask(() -> Modules.get().get(DiscordPresence.class).toggle());
        }

        // Register addons
        AddonManager.init();

        // Register event handlers
        EVENT_BUS.registerLambdaFactory(ADDON.getPackage(), (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        AddonManager.ADDONS.forEach(addon -> EVENT_BUS.registerLambdaFactory(addon.getPackage(), (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup())));

        // Register init classes
        ReflectInit.registerPackages();

        // Pre init
        ReflectInit.preInit();

        VectorUtils.init();

        // Register module categories
        Categories.init();

        // Load systems
        Systems.init();

        // Subscribe after systems are loaded
        EVENT_BUS.subscribe(this);

        // Initialise addons
        AddonManager.ADDONS.forEach(MeteorAddon::onInitialize);

        // Sort modules after addons have added their own
        Modules.get().sortModules();

        // Load configs
        Systems.load();

        register();

        initViaVersion();
       // initSeedCrack();

        // Post init
        ReflectInit.postInit();

        VectorUtils.postInit();

       // new AdvancementInfo().onInitialize();

        Seeds.get().setSeeds(SeedConfigs.load());

        // Save on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            OnlinePlayers.leave();
            Systems.save();
            GuiThemes.save();
        }));
    }

    public static void saveAndShutdown() {
        SeedConfigs.save();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.currentScreen == null && mc.getOverlay() == null && KeyBinds.OPEN_COMMANDS.wasPressed()) {
            mc.setScreen(new ChatScreen(Config.get().prefix.get()));
        }
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (event.action == KeyAction.Press && KeyBinds.OPEN_GUI.matchesKey(event.key, 0)) {
            openGui();
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action == KeyAction.Press && KeyBinds.OPEN_GUI.matchesMouse(event.button)) {
            openGui();
        }
    }

    private void openGui() {
        if (Utils.canOpenGui()) {
            Tabs.get().get(0).openScreen(GuiThemes.get());
        }
    }


    // Hide HUD

    private boolean wasWidgetScreen, wasHudHiddenRoot;

    @EventHandler(priority = EventPriority.LOWEST)
    private void onOpenScreen(OpenScreenEvent event) {
        boolean hideHud = GuiThemes.get().hideHUD();

        if (hideHud) {
            if (!wasWidgetScreen) wasHudHiddenRoot = mc.options.hudHidden;

            if (event.screen instanceof WidgetScreen) mc.options.hudHidden = true;
            else if (!wasHudHiddenRoot) mc.options.hudHidden = false;
        }

        wasWidgetScreen = event.screen instanceof WidgetScreen;
    }

    @Nonnull
    public static Cleaner.Cleanable registerCleanup(@Nonnull Object target, @Nonnull Runnable action) {
        return cleaner.register(target, action);
    }
}
