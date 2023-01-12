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

package meteordevelopment.meteorclient.glrender.text.style;

/**
 * Paragraph affecting span that changes the position of the tab with respect to
 * the leading margin of the line. <code>TabStopSpan</code> will only affect the first tab
 * encountered on the first line of the text.
 */
public interface TabStopSpan extends ParagraphStyle {

    /**
     * Returns the offset of the tab stop from the leading margin of the line, in pixels.
     *
     * @return the offset, in pixels
     */
    int getTabStop();
}