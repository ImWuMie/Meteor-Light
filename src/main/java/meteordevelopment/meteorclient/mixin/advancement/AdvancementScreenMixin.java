/*
package meteordevelopment.meteorclient.mixin.advancement;

import meteordevelopment.meteorclient.advancement.*;
import net.minecraft.advancement.Advancement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static meteordevelopment.meteorclient.advancement.AdvancementInfo.*;

@Mixin(AdvancementsScreen.class)
public abstract class AdvancementScreenMixin extends Screen implements AdvancementScreenAccessor {
    public AdvancementScreenMixin() {
        super(null);
    }

    private int scrollPos;
    private TextFieldWidget search;
    @Shadow
    @Final
    private ClientAdvancementManager advancementHandler;

    @Shadow
    abstract AdvancementTab getTab(Advancement advancement);

    @ModifyConstant(method = "render", constant = @Constant(intValue = 252), require = 1)
    private int getRenderLeft(int constant) {
        if (!active) return constant;
        return width - AI_spaceX * 2;
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 140), require = 1)
    private int getRenderTop(int orig) {
        if (!active) return orig;
        return height - AI_spaceY * 2;
    }

    @ModifyConstant(method = "mouseClicked", constant = @Constant(intValue = 252), require = 1)
    private int getMouseLeft(int orig) {
        if (!active) return orig;
        return width - AI_spaceX * 2;
    }

    @ModifyConstant(method = "mouseClicked", constant = @Constant(intValue = 140), require = 1)
    private int getMouseTop(int orig) {
        if (!active) return orig;
        return height - AI_spaceY * 2;
    }

    @ModifyConstant(method = "drawAdvancementTree", constant = @Constant(intValue = 234), require = 1)
    private int getAdvTreeXSize(int orig) {
        if (!active) return orig;
        return width - AI_spaceX * 2 - 2 * 9 - AdvancementInfo.AI_infoWidth;
    }

    @ModifyConstant(method = "drawAdvancementTree", constant = @Constant(intValue = 113), require = 1)
    private int getAdvTreeYSize(int orig) {
        if (!active) return orig;
        return height - AI_spaceY * 2 - 3 * 9;
    }

    /* Make it so that drawWidgets only draws the left top corner ... */
/*
    @ModifyConstant(method = "drawWindow", constant = @Constant(intValue = 252), require = 1)
    private int getWidgetsLeft(int orig) // { return width - AI_spaceX*2; }
    {
        if (!active) return orig;
        return 126;
    }

    @ModifyConstant(method = "drawWindow", constant = @Constant(intValue = 140), require = 1)
    private int getWidgetsTop(int orig) // { return height - AI_spaceY*2; }
    {
        if (!active) return orig;
        return 70;
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void initSeachField(CallbackInfo ci) {
        if (!active) return;
        this.search = new TextFieldWidget(textRenderer, width - AI_spaceX - AdvancementInfo.AI_infoWidth + 10, AI_spaceY + 20, AdvancementInfo.AI_infoWidth - 20, 18, Text.literal(""));
    }


    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementsScreen;drawWindow(Lnet/minecraft/client/util/math/MatrixStack;II)V"))
    public void renderRightFrameBackground(MatrixStack stack, int x, int y, float delta, CallbackInfo ci) {
        if (!active) return;

        fill(stack,
                width - AI_spaceX - AdvancementInfo.AI_infoWidth + 4, AI_spaceY + 4,
                width - AI_spaceX - 4, height - AI_spaceY - 4, 0xffc0c0c0);
    }

    @Inject(method = "drawWindow", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/advancement/AdvancementsScreen;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V"))
    public void renderFrames(MatrixStack stack, int x, int y, CallbackInfo ci) {
        if (!active) return;
        int iw = AdvancementInfo.AI_infoWidth;

        drawTexture(stack, width - AI_spaceX - 126 - iw, y, 127, 0, 126, 70);
        drawTexture(stack, x, height - AI_spaceY - 70, 0, 71, 126, 70);
        drawTexture(stack, width - AI_spaceX - 126 - iw, height - AI_spaceY - 70, 127, 71, 126, 70);

        iterate(126, width - AI_spaceX - 126 - iw, 200, (pos, len) -> drawTexture(stack, pos, y, 15, 0, len, 70));
        iterate(126, width - AI_spaceX - 126 - iw, 200, (pos, len) -> drawTexture(stack, pos, height - AI_spaceY - 70, 15, 71, len, 70));
        iterate(70, height - AI_spaceY - 70, 100, (pos, len) -> drawTexture(stack, x, pos, 0, 25, 70, len));
        iterate(70, height - AI_spaceY - 70, 100, (pos, len) -> drawTexture(stack, width - AI_spaceX - 126 - iw, pos, 127, 25, 126, len));


        drawTexture(stack, width - AI_spaceX - iw, y, 0, 0, iw / 2, 70);
        drawTexture(stack, width - AI_spaceX - iw / 2, y, 252 - iw / 2, 0, iw / 2 + 1, 70);
        drawTexture(stack, width - AI_spaceX - iw, height - AI_spaceY - 70, 0, 71, iw / 2, 70);
        drawTexture(stack, width - AI_spaceX - iw / 2, height - AI_spaceY - 70, 252 - iw / 2, 71, iw / 2 + 1, 70);
        iterate(70, height - AI_spaceY - 70, 100, (pos, len) -> drawTexture(stack, width - AI_spaceX - iw, pos, 0, 25, iw / 2, len));
        iterate(70, height - AI_spaceY - 70, 100, (pos, len) -> drawTexture(stack, width - AI_spaceX - iw / 2, pos, 252 - iw / 2, 25, iw / 2, len));
    }

    private void iterate(int start, int end, int maxstep, IteratorReceiver func) {
        int size;
        for (int i = start; i < end; i += maxstep) {
            size = maxstep;
            if (i + size > end) {
                size = end - i;
            }
            func.accept(i, size);
        }
    }

    @Inject(method = "drawWindow", at = @At("RETURN"))
    public void renderRightFrameTitle(MatrixStack stack, int x, int y, CallbackInfo ci) {
        if (!active) return;
        textRenderer.draw(stack, I18n.translate("advancementinfo.infopane"), width - AI_spaceX - AdvancementInfo.AI_infoWidth + 8, y + 6, 4210752);
        search.renderButton(stack, x, y, 0);

        if (AdvancementInfo.mouseClicked != null) {
            renderCriteria(stack, AdvancementInfo.mouseClicked);
        } else if (AdvancementInfo.mouseOver != null || AdvancementInfo.cachedClickList != null) {
            renderCriteria(stack, AdvancementInfo.mouseOver);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void rememberClickedWidget(double x, double y, int button, CallbackInfoReturnable cir) {
        if (!active) return;
        if (search.mouseClicked(x, y, button)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
        if (x >= width - AI_spaceX - AdvancementInfo.AI_infoWidth) {
            // later: handle click on search results here
            return;
        }
        AdvancementInfo.mouseClicked = AdvancementInfo.mouseOver;
        scrollPos = 0;
        if (AdvancementInfo.mouseClicked == null) {
            AdvancementInfo.cachedClickList = null;
            AdvancementInfo.cachedClickListLineCount = 0;
        } else {
            AdvancementInfo.cachedClickList = AdvancementInfo.getSteps((AdvancementWidgetAccessor) AdvancementInfo.mouseClicked);
            AdvancementInfo.cachedClickListLineCount = AdvancementInfo.cachedClickList.size();
        }
    }


    // @Inject(method="mouseScrolled", at=@At("HEAD"), cancellable = true)
    @Override
    /*
    public boolean mouseScrolled(double X, double Y, double amount /*, CallbackInfoReturnable cir *//*) {*/
      /*  if (!active) return super.mouseScrolled(X, Y, amount);
        if (amount > 0 && scrollPos > 0) {
            scrollPos--;
        } else if (amount < 0 && AdvancementInfo.cachedClickList != null
                && scrollPos < AdvancementInfo.cachedClickListLineCount - ((height - 2 * AI_spaceY - 45) / textRenderer.fontHeight - 1)) {
            scrollPos++;
        }
        System.out.println("scrollpos is now " + scrollPos + ", needed lines " + AdvancementInfo.cachedClickListLineCount + ", shown " + ((height - 2 * AI_spaceY - 45) / textRenderer.fontHeight - 1));
        return false;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void redirectKeysToSearch(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable cir) {
        if (!active) return;
        if (search.isActive()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                AdvancementInfo.setMatchingFrom((AdvancementsScreen) (Object) this, search.getText());
            }
            search.keyPressed(keyCode, scanCode, modifiers);
            // Only let ESCAPE end the screen, we don't want the keybind ('L')
            // to terminate the screen when we're typing text
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        if (!active) return super.charTyped(chr, keyCode);
        if (search.isActive()) {
            return search.charTyped(chr, keyCode);
        }
        return false;
    }

    private void renderCriteria(MatrixStack stack, AdvancementWidget widget) {
        if (!active) return;
        int y = AI_spaceY + 20 + 25;
        int skip;
        List<AdvancementStep> list;
        if (widget == AdvancementInfo.mouseClicked) {
            list = AdvancementInfo.cachedClickList;
            skip = scrollPos;
        } else {
            list = AdvancementInfo.getSteps((AdvancementWidgetAccessor) widget);
            skip = 0;
        }
        if (list == null) {
            return;
        }
        for (AdvancementStep entry : list) {
            if (skip-- <= 0) {
                textRenderer.draw(stack,
                        textRenderer.trimToWidth(entry.getName(), AdvancementInfo.AI_infoWidth - 24),
                        width - AI_spaceX - AdvancementInfo.AI_infoWidth + 12, y,
                        entry.getObtained() ? 0x00ff00 : 0xff0000);
                y += textRenderer.fontHeight;
                if (y > height - AI_spaceY - textRenderer.fontHeight * 2) {
                    return;
                }
            }

            if (entry.getDetails() != null) {
                for (String detail : entry.getDetails()) {
                    if (skip-- <= 0) {
                        textRenderer.draw(stack,
                                textRenderer.trimToWidth(detail, AdvancementInfo.AI_infoWidth - 34),
                                width - AI_spaceX - AdvancementInfo.AI_infoWidth + 22, y,
                                0x000000);
                        y += textRenderer.fontHeight;
                        if (y > height - AI_spaceY - textRenderer.fontHeight * 2) {
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public ClientAdvancementManager getAdvancementHandler() {
        return advancementHandler;
    }

    public AdvancementTab myGetTab(Advancement advancement) {
        return getTab(advancement);
    }
}
*/