/*
 * Copyright (c) 2022 Coffee Client, 0x150 and contributors.
 * Some rights reserved, refer to LICENSE file.
 */

package meteordevelopment.meteorclient.renderer.font;

import meteordevelopment.meteorclient.renderer.font.adapter.FontAdapter;
import meteordevelopment.meteorclient.renderer.font.adapter.impl.QuickFontAdapter;
import meteordevelopment.meteorclient.renderer.font.renderer.FontRenderer;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FontRenderers {
    private static final List<QuickFontAdapter> fontRenderers = new ArrayList<>();
    private static FontAdapter normal;
    private static FontAdapter mono;
    public static FontAdapter jelloFont = getJelloLight(20);

    public static FontAdapter getRenderer() {
        return normal;
    }

    public static void setRenderer(FontAdapter normal) {
        FontRenderers.normal = normal;
    }

    public static FontAdapter getJello() {
        FontAdapter font;
            int fsize = 18 * 2;
            try {
                font = (new QuickFontAdapter(new FontRenderer(Font.createFont(Font.TRUETYPE_FONT,
                    Objects.requireNonNull(FontRenderers.class.getResourceAsStream("/assets/meteor-client/fonts/JelloRegular.ttf"))).deriveFont(Font.PLAIN, fsize), fsize)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        return font;
    }

    public static FontAdapter getJello(int fsize) {
            FontAdapter font;
            try {
                font = (new QuickFontAdapter(new FontRenderer(Font.createFont(Font.TRUETYPE_FONT,
                        Objects.requireNonNull(FontRenderers.class.getResourceAsStream("/assets/meteor-client/fonts/JelloRegular.ttf"))).deriveFont(Font.PLAIN, fsize), fsize)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        return font;
    }

    public static FontAdapter getJelloLight() {
        FontAdapter font;
        int fsize = 18 * 2;
        try {
            font = (new QuickFontAdapter(new FontRenderer(Font.createFont(Font.TRUETYPE_FONT,
                    Objects.requireNonNull(FontRenderers.class.getResourceAsStream("/assets/meteor-client/fonts/JelloLight.ttf"))).deriveFont(Font.PLAIN, fsize), fsize)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return font;
    }

    public static FontAdapter getJelloLight(int fsize) {
        FontAdapter font;
        try {
            font = (new QuickFontAdapter(new FontRenderer(Font.createFont(Font.TRUETYPE_FONT,
                    Objects.requireNonNull(FontRenderers.class.getResourceAsStream("/assets/meteor-client/fonts/JelloLight.ttf"))).deriveFont(Font.PLAIN, fsize), fsize)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return font;
    }

    public static QuickFontAdapter getMonoSize(int size) {
        int size1 = size;
        size1 *= 2;
        for (QuickFontAdapter fontRenderer : fontRenderers) {
            if (fontRenderer.getSize() == size1) {
                return fontRenderer;
            }
        }
        int fsize = size1;
        try {
            QuickFontAdapter bruhAdapter = (new QuickFontAdapter(new FontRenderer(Font.createFont(Font.TRUETYPE_FONT,
                Objects.requireNonNull(FontRenderers.class.getResourceAsStream("Mono.ttf"))).deriveFont(Font.PLAIN, fsize), fsize)));
            fontRenderers.add(bruhAdapter);
            return bruhAdapter;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static QuickFontAdapter getCustomSize(int size) {
        int size1 = size;
        size1 *= 2;
        for (QuickFontAdapter fontRenderer : fontRenderers) {
            if (fontRenderer.getSize() == size1) {
                return fontRenderer;
            }
        }
        int fsize = size1;
        try {
            QuickFontAdapter bruhAdapter = (new QuickFontAdapter(new FontRenderer(Font.createFont(Font.TRUETYPE_FONT,
                Objects.requireNonNull(FontRenderers.class.getResourceAsStream("Font.ttf"))).deriveFont(Font.PLAIN, fsize), fsize)));
            fontRenderers.add(bruhAdapter);
            return bruhAdapter;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
