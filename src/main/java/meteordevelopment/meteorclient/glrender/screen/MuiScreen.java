package meteordevelopment.meteorclient.glrender.screen;

import meteordevelopment.meteorclient.glrender.fragment.Fragment;
import meteordevelopment.meteorclient.gui.screens.NewJelloScreen;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Common interface between MenuScreen and SimpleScreen
 */
public interface MuiScreen {
    /**
     * @return the main fragment
     */
    @Nonnull
    Fragment getFragment();
}
