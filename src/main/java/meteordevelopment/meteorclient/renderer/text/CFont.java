/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import meteordevelopment.meteorclient.utils.render.FontUtils;

public class CFont {
    public static TTFFontRender jelloThin = getFontByName("JelloThin");
    public static TTFFontRender jelloRegular = getFontByName("JelloRegular");
    public static TTFFontRender jelloLight = getFontByName("JelloLight");
    public static TTFFontRender jelloMedium = getFontByName("JelloMedium");
    public static TTFFontRender googleSans = getFontByName("GoogleSans");


    public static FontFace getFontFace(String fontName) {
        FontInfo fontInfo = FontUtils.getBuiltinFontInfo(fontName);
        if (fontInfo == null) {
            System.out.print("Error load font: " + fontName + ".ttf");
            return null;
        }

        return new BuiltinFontFace(fontInfo, fontName);
    }

    public static TTFFontRender getFontByName(String fontName) {
        return new TTFFontRender(getFontFace(fontName));
    }
}
