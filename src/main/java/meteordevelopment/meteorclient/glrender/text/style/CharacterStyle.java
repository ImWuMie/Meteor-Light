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

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package meteordevelopment.meteorclient.glrender.text.style;

import meteordevelopment.meteorclient.glrender.text.TextPaint;

import javax.annotation.Nonnull;

/**
 * The classes that affect character-level text formatting extend this
 * class.  Most extend its subclass {@link MetricAffectingSpan}, but simple
 * ones may just implement {@link UpdateAppearance}.
 */
public abstract class CharacterStyle {

    public abstract void updateDrawState(@Nonnull TextPaint paint);

    /**
     * A given CharacterStyle can only applied to a single region of a given
     * Spanned.  If you need to attach the same CharacterStyle to multiple
     * regions, you can use this method to wrap it with a new object that
     * will have the same effect but be a distinct object so that it can
     * also be attached without conflict.
     */
    @Nonnull
    public static CharacterStyle wrap(CharacterStyle cs) {
        // Added: Reduce stack size
        cs = cs.getUnderlying();
        if (cs instanceof MetricAffectingSpan) {
            return new MetricAffectingSpan.Passthrough((MetricAffectingSpan) cs);
        } else {
            return new Passthrough(cs);
        }
    }

    /**
     * Returns "this" for most CharacterStyles, but for CharacterStyles
     * that were generated by {@link #wrap}, returns the original
     * CharacterStyle.
     */
    public CharacterStyle getUnderlying() {
        return this;
    }

    /**
     * A Passthrough CharacterStyle is one that
     * passes {@link #updateDrawState} calls through to the
     * specified CharacterStyle while still being a distinct object,
     * and is therefore able to be attached to the same Spannable
     * to which the specified CharacterStyle is already attached.
     */
    private static class Passthrough extends CharacterStyle {

        private final CharacterStyle mStyle;

        /**
         * Creates a new Passthrough of the specified CharacterStyle.
         */
        private Passthrough(CharacterStyle cs) {
            mStyle = cs;
        }

        /**
         * Passes updateDrawState through to the underlying CharacterStyle.
         */
        @Override
        public void updateDrawState(@Nonnull TextPaint paint) {
            mStyle.updateDrawState(paint);
        }

        /**
         * Returns the CharacterStyle underlying this one, or the one
         * underlying it if it too is a Passthrough.
         */
        @Override
        public CharacterStyle getUnderlying() {
            return mStyle.getUnderlying();
        }
    }
}
