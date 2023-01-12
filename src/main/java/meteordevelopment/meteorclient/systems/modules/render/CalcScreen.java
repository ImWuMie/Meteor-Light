/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.jellogui.screens.CalcGui;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;

public class CalcScreen extends Module {
    public CalcScreen() {
        super(Categories.Render, "Calc", "");
    }

    @Override
    public void onActivate() {
        mc.setScreen(GuiThemes.get().calcScreen());
        toggle();
        super.onActivate();
    }
}
