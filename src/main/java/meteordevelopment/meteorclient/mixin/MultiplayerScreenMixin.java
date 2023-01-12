/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.NameProtect;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.proxies.Proxy;
import meteordevelopment.meteorclient.systems.viaversion.util.ProtocolUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin extends Screen {
    private int textColor1;
    private int textColor2;

    private String loggedInAs;
    private int loggedInAsLength;
    private static CompletableFuture<Void> latestProtocolSave;
    private TextFieldWidget protocolVersion;

    public MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        textColor1 = Color.fromRGBA(255, 255, 255, 255);
        textColor2 = Color.fromRGBA(175, 175, 175, 255);

        loggedInAs = "Logged in as ";
        loggedInAsLength = textRenderer.getWidth(loggedInAs);

        addDrawableChild(new ButtonWidget(this.width / 2 + 4 + 76+5+75, this.height - 28, 75, 20, Text.literal("Accounts"), button -> {
            client.setScreen(GuiThemes.get().accountsScreen());
        }));

        addDrawableChild(new ButtonWidget(this.width / 2 + 4 + 76+5+75, this.height - 52, 75, 20, Text.literal("Proxies"), button -> {
            client.setScreen(GuiThemes.get().proxiesScreen());
        }));

        protocolVersion = new TextFieldWidget(this.textRenderer,this.width - 75 - 3,3, 75, 20, Text.translatable("gui.protocol_version_field.name"));
        protocolVersion.setTextPredicate(ProtocolUtils::isStartOfProtocolText);
        protocolVersion.setChangedListener(this::onChangeVersionField);
        int clientSideVersion = MeteorClient.config.getClientSideVersion();
        protocolVersion.setText(ProtocolUtils.getProtocolName(clientSideVersion));
        this.addDrawableChild(protocolVersion);
    }


    private void onChangeVersionField(String text) {
        protocolVersion.setSuggestion(null);
        int newVersion = MeteorClient.config.getClientSideVersion();

        Integer parsed = ProtocolUtils.parseProtocolId(text);
        boolean validProtocol;

        if (parsed == null) {
            validProtocol = false;
            String[] suggestions = ProtocolUtils.getProtocolSuggestions(text);
            if (suggestions.length == 1) {
                protocolVersion.setSuggestion(suggestions[0].substring(text.length()));
            }
        } else {
            newVersion = parsed;
            validProtocol = true;
        }

        protocolVersion.setEditableColor(
            getProtocolTextColor(ProtocolUtils.isSupportedClientSide(newVersion),
                validProtocol));

        int finalNewVersion = newVersion;
        if (latestProtocolSave == null) latestProtocolSave = CompletableFuture.completedFuture(null);
        MeteorClient.config.setClientSideVersion(finalNewVersion);
        latestProtocolSave = latestProtocolSave.thenRunAsync(MeteorClient.config::saveConfig, MeteorClient.ASYNC_EXECUTOR);
        MeteorClient.config.saveConfig();
    }

    private int getProtocolTextColor(boolean valid, boolean supported) {
        if (!valid) {
            return 0xff0000; // Red
        } else if (!supported) {
            return 0xFFA500; // Orange
        }
        return 0xE0E0E0; // Default
    }

    @Inject(method = "tick",at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        if (protocolVersion != null) {
            protocolVersion.tick();
        }
        }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        float x = 3;
        float y = 3;

        // Logged in as
        textRenderer.drawWithShadow(matrices, loggedInAs, x, y, textColor1);
        textRenderer.drawWithShadow(matrices, Modules.get().get(NameProtect.class).getName(client.getSession().getUsername()), x + loggedInAsLength, y, textColor2);

        // Meteor Portal:
        float yeee;
        float awa;

        String textmeteor = "Meteor Portal:";

        awa = textRenderer.fontHeight;
        yeee = textRenderer.getWidth(textmeteor);

        textRenderer.drawWithShadow(matrices,"Meteor Portal: ",this.width - 75 - 5 - yeee,awa,textColor2);

        y += textRenderer.fontHeight + 2;

        // Proxy
        Proxy proxy = Proxies.get().getEnabled();

        String left = proxy != null ? "Using proxy " : "Not using a proxy";
        String right = proxy != null ? (proxy.name.get() != null && !proxy.name.get().isEmpty() ? "(" + proxy.name.get() + ") " : "") + proxy.address.get() + ":" + proxy.port.get() : null;

        textRenderer.drawWithShadow(matrices, left, x, y, textColor1);
        if (right != null) textRenderer.drawWithShadow(matrices, right, x + textRenderer.getWidth(left), y, textColor2);
    }
}
