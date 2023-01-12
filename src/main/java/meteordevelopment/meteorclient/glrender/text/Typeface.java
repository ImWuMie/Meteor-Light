/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package meteordevelopment.meteorclient.glrender.text;

import meteordevelopment.meteorclient.glrender.font.FontCollection;
import meteordevelopment.meteorclient.glrender.font.FontFamily;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The Typeface specifies a set of font families that can be used
 * in Paint. This determines how text appears when drawn and measured.
 */
@Immutable
public class Typeface {

    public static final Typeface SANS_SERIF;
    public static final Typeface SERIF;
    public static final Typeface MONOSPACED;

    private static final Map<String, Typeface> sSystemFontMap = new HashMap<>();

    static {
        FontFamily sansSerif = Objects.requireNonNull(FontFamily.getSystemFontMap().get(Font.SANS_SERIF));

        for (var e : FontFamily.getSystemFontMap().entrySet()) {
            if (e.getKey().equals(Font.SANS_SERIF)) {
                sSystemFontMap.putIfAbsent(e.getKey(), createTypeface(e.getValue()));
            } else {
                sSystemFontMap.putIfAbsent(e.getKey(), createTypeface(e.getValue(), sansSerif));
            }
        }

        SANS_SERIF = Objects.requireNonNull(sSystemFontMap.get(Font.SANS_SERIF));
        SERIF = Objects.requireNonNull(sSystemFontMap.get(Font.SERIF));
        MONOSPACED = Objects.requireNonNull(sSystemFontMap.get(Font.MONOSPACED));
    }

    /*@Deprecated
    private static void checkJava() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion == null) {
            MeteorClient.LOGGER.fatal(ModernUI.MARKER, "Java version is missing");
        } else {
            try {
                int majorNumber = Integer.parseInt(javaVersion.split("\\.")[0]);
                if (majorNumber < 11 && LocalStorage.checkOneTimeEvent(1)) {
                    ModernUI.get().warnSetup("warning.modernui.old_java", "11.0.9", javaVersion);
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                MeteorClient.LOGGER.warn(GlyphManager.MARKER, "Failed to check major java version: {}", javaVersion, e);
            }
            if (javaVersion.startsWith("1.8")) {
                try {
                    int update = Integer.parseInt(javaVersion.split("_")[1].split("-")[0]);
                    if (update < 201) {
                        sJavaTooOld = true;
                        MeteorClient.LOGGER.warn(GlyphManager.MARKER, "Java {} is too old to use external fonts",
                        javaVersion);
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    MeteorClient.LOGGER.warn(ModernUI.MARKER, "Failed to check java version update: {}", javaVersion, e);
                }
            }
        }
    }*/

    @Nonnull
    public static Typeface createTypeface(@Nonnull FontFamily... families) {
        if (families.length == 0) {
            return SANS_SERIF;
        }
        return new Typeface(new FontCollection(families));
    }

    @Nonnull
    public static Typeface getSystemFont(@Nonnull String familyName) {
        Typeface typeface = sSystemFontMap.get(familyName);
        return typeface == null ? SANS_SERIF : typeface;
    }

    @Nonnull
    final FontCollection mFontCollection;

    private Typeface(@Nonnull FontCollection fontCollection) {
        mFontCollection = fontCollection;
    }

    @Nonnull
    public FontCollection getFontCollection() {
        return mFontCollection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Typeface typeface = (Typeface) o;

        return mFontCollection.equals(typeface.mFontCollection);
    }

    @Override
    public int hashCode() {
        return mFontCollection.hashCode();
    }

    @Override
    public String toString() {
        return mFontCollection.toString();
    }
}
