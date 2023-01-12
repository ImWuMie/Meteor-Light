package meteordevelopment.meteorclient.glrender;

import com.seedfinding.mcmath.util.Mth;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.glrender.drawable.Drawable;
import meteordevelopment.meteorclient.glrender.drawable.ImageDrawable;
import meteordevelopment.meteorclient.glrender.font.FontFamily;
import meteordevelopment.meteorclient.glrender.fragment.*;
import meteordevelopment.meteorclient.glrender.lifecycle.*;
import meteordevelopment.meteorclient.glrender.math.Matrix4;
import meteordevelopment.meteorclient.glrender.math.Rect;
import meteordevelopment.meteorclient.glrender.opengl.*;
import meteordevelopment.meteorclient.glrender.text.Typeface;
import meteordevelopment.meteorclient.glrender.view.*;
import meteordevelopment.meteorclient.glrender.view.menu.ContextMenuBuilder;
import meteordevelopment.meteorclient.glrender.view.menu.MenuHelper;
import meteordevelopment.meteorclient.glrender.widget.CoordinatorLayout;
import meteordevelopment.meteorclient.glrender.widget.EditText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.VideoMode;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.glfw.GLFWMonitorCallback;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.MOD_ID;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.glrender.opengl.GLCore.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;

public class MeteorGL implements AutoCloseable, LifecycleOwner {

    public static final String ID = MOD_ID; // as well as the namespace
    public static GLSurfaceCanvas CanvasRender;
    public static final String NAME_CPT = "Meteor Client";

    public static final Logger LOGGER = MeteorClient.LOGGER;
    public static final Marker MARKER = MeteorClient.MARKER;

    private static volatile MeteorGL sInstance;

    private static final Cleaner sCleaner = Cleaner.create();

    private static final int fragment_container = 0x01020007;

    static {
        if (Runtime.version().feature() < 17) {
            throw new RuntimeException("JRE 17 or above is required");
        }
    }

    private ViewRootImpl mRoot;
    private CoordinatorLayout mDecor;
    private FragmentContainerView mFragmentContainerView;

    private LifecycleRegistry mLifecycleRegistry;
    private OnBackPressedDispatcher mOnBackPressedDispatcher;

    private ViewModelStore mViewModelStore;
    private FragmentController mFragmentController;

    private Typeface mDefaultTypeface;

    private volatile Looper mUiLooper;
    private volatile Thread mUiThread;
    private volatile Thread mRenderThread;

    public MeteorGL() {
        synchronized (MeteorGL.class) {
            if (sInstance == null) {
                sInstance = this;
            } else {
                throw new RuntimeException("Multiple instances");
            }
        }
    }

    /**
     * Get Modern UI instance.
     *
     * @return the Modern UI
     */
    public static MeteorGL getInstance() {
        return sInstance;
    }

    /**
     * Registers a target and a cleaning action to run when the target becomes phantom
     * reachable. It will be registered with the global cleaner shared across Modern UI.
     * The action object should never hold any reference to the target object.
     *
     * @param target the target to monitor
     * @param action a {@code Runnable} to invoke when the target becomes phantom reachable
     * @return a {@code Cleanable} instance for explicit cleaning
     */
    @Nonnull
    public static Cleaner.Cleanable registerCleanup(@Nonnull Object target, @Nonnull Runnable action) {
        return sCleaner.register(target, action);
    }

    /**
     * Runs the Modern UI with the default application setups.
     * This method is only called by the <code>main()</code> on the main thread.
     */
    public void run(@Nonnull Fragment fragment) {
        //Thread.currentThread().setName("Main-Thread");
        // should be true
        LOGGER.info(MARKER, "AWT headless: {}", GraphicsEnvironment.isHeadless());
        //Core.initMainThread();
        Core.initialize();

       LOGGER.info(MARKER, "Initializing window system");
        loadDefaultTypeface();

        LOGGER.info(MARKER, "Preparing threads");
        Looper.prepare(MinecraftClient.getInstance().getWindow());

        mRenderThread = Thread.currentThread();
        runRender();
        mUiThread = Thread.currentThread();
        runUI(fragment);

        try (InputStream i16 = getResourceStream(ID,"icon16x.png");
             InputStream i32 = getResourceStream(ID,"icon32x.png")) {
            MinecraftClient.getInstance().getWindow().setIcon(i16, i32);
        } catch (IOException e) {
            e.printStackTrace();
        }

        LOGGER.info(MARKER, "Looping main thread");
        Looper.loop();
    }

    private void runRender() {
        LOGGER.info(MARKER, "Initializing render thread");

        //glfwMakeContextCurrent(MinecraftClient.getInstance().getWindow().getHandle());
        Core.initOpenGL();
        GLCore.showCapsErrorDialog();

        final GLSurfaceCanvas canvas = GLSurfaceCanvas.initialize();
        ShaderManager.getInstance().reload();

        glEnable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_STENCIL_TEST);
        glEnable(GL_MULTISAMPLE);

        final GLFramebuffer framebuffer = new GLFramebuffer(4);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT0, GL_RGBA8);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT1, GL_RGBA8);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT2, GL_RGBA8);
        framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT3, GL_RGBA8);
        framebuffer.addRenderbufferAttachment(GL_STENCIL_ATTACHMENT, GL_STENCIL_INDEX8);
        framebuffer.setDrawBuffer(GL_COLOR_ATTACHMENT0);

        final Matrix4 projection = new Matrix4();

        LOGGER.info(MARKER, "Looping render thread");

        while (!MinecraftClient.getInstance().getWindow().shouldClose()) {
            int width = MinecraftClient.getInstance().getWindow().getWidth(), height = MinecraftClient.getInstance().getWindow().getHeight();
            glBindFramebuffer(GL_FRAMEBUFFER, DEFAULT_FRAMEBUFFER);
            glDisable(GL_CULL_FACE);
            resetFrame(MinecraftClient.getInstance().getWindow());
            if (mRoot != null) {
                canvas.setProjection(projection.setOrthographic(width, height, 0, Window.LAST_SYSTEM_WINDOW * 2 + 1,
                        true));
                mRoot.flushDrawCommands(canvas, framebuffer);
            }
            if (framebuffer.getAttachment(GL_COLOR_ATTACHMENT0).getWidth() > 0) {
                glBlitNamedFramebuffer(framebuffer.get(), DEFAULT_FRAMEBUFFER, 0, 0,
                        width, height, 0, 0,
                        width, height, GL_COLOR_BUFFER_BIT, GL_LINEAR);
            }
            if (mRoot != null) {
                mRoot.mChoreographer.scheduleFrameAsync(Core.timeNanos());
            }
            //MinecraftClient.getInstance().getWindow().swapBuffers();
        }
        LOGGER.info(MARKER, "Quited render thread");
    }

    private void runUI(@Nonnull Fragment fragment) {
        LOGGER.info(MARKER, "Initializing UI thread");
        mUiLooper = Core.initUiThread();

        ViewConfiguration.get().setViewScale(2);

        mRoot = new ViewRootImpl();

        mDecor = new CoordinatorLayout();
        mDecor.setClickable(true);
        mDecor.setFocusableInTouchMode(true);
        mDecor.setWillNotDraw(true);
        mDecor.setId(R.id.content);

        mFragmentContainerView = new FragmentContainerView();
        mFragmentContainerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mFragmentContainerView.setWillNotDraw(true);
        mFragmentContainerView.setId(fragment_container);
        mDecor.addView(mFragmentContainerView);
        mDecor.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);

        try {
            GLTexture texture = TextureManager.getInstance().create(
                    FileChannel.open(Path.of("F:", "eromanga.png"), StandardOpenOption.READ), true);
            Image image = new Image(texture);
            Drawable drawable = new ImageDrawable(image);
            drawable.setTint(0xFF808080);
            mDecor.setBackground(drawable);
        } catch (IOException ignored) {
        }

        mRoot.setView(mDecor);

        LOGGER.info(MARKER, "Installing view protocol");
        mRoot.setFrame(MinecraftClient.getInstance().getWindow().getScaledWidth(), MinecraftClient.getInstance().getWindow().getScaledHeight());

        mLifecycleRegistry = new LifecycleRegistry(this);
        mOnBackPressedDispatcher = new OnBackPressedDispatcher(() -> glfwSetWindowShouldClose(MinecraftClient.getInstance().getWindow().getHandle(), true));
        mViewModelStore = new ViewModelStore();
        mFragmentController = FragmentController.createController(new HostCallbacks());

        mFragmentController.attachHost(null);

        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mFragmentController.dispatchCreate();

        mFragmentController.dispatchActivityCreated();
        mFragmentController.execPendingActions();

        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mFragmentController.dispatchStart();

        LOGGER.info(MARKER, "Starting main fragment");

        mFragmentController.getFragmentManager().beginTransaction()
                .add(fragment_container, fragment, "main")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack("main")
                .commit();

        //Core.executeOnMainThread(mWindow::show);

        LOGGER.info(MARKER, "Looping UI thread");

        Looper.loop();

        mFragmentController.dispatchStop();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

        mFragmentController.dispatchDestroy();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

        LOGGER.info(MARKER, "Quited UI thread");
    }

    private void loadDefaultTypeface() {
        Set<FontFamily> set = new LinkedHashSet<>();

        try (InputStream stream = MeteorGL.class.getResourceAsStream("/assets/"+ID+"/fonts/GoogleSans.ttf")) {
            Font f = Font.createFont(Font.TRUETYPE_FONT, stream);
            set.add(new FontFamily(f));
        } catch (FontFormatException | IOException ignored) {
        }

        for (FontFamily family : FontFamily.getSystemFontMap().values()) {
            String name = family.getFamilyName();
            if (name.startsWith("Calibri") ||
                    name.startsWith("Microsoft YaHei UI") ||
                    name.startsWith("STHeiti") ||
                    name.startsWith("Segoe UI") ||
                    name.startsWith("SimHei")) {
                set.add(family);
            }
        }

        set.add(Objects.requireNonNull(FontFamily.getSystemFontMap().get(Font.SANS_SERIF)));

        mDefaultTypeface = Typeface.createTypeface(set.toArray(new FontFamily[0]));
    }

    @Nonnull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    /**
     * Get the default or preferred locale set by user.
     *
     * @return the selected locale
     */
    protected Locale onGetSelectedLocale() {
        return Locale.getDefault();
    }

    /**
     * Get the default or preferred locale set by user.
     *
     * @return the selected locale
     */
    @Nonnull
    public static Locale getSelectedLocale() {
        return sInstance == null ? Locale.getDefault() : sInstance.onGetSelectedLocale();
    }

    public static int calcGuiScales() {
        return calcGuiScales(mc.getWindow());
    }

    public static int calcGuiScales(@Nonnull net.minecraft.client.util.Window window) {
        return calcGuiScales(window.getWidth(), window.getHeight());
    }

    // V3
    public static int calcGuiScales(int framebufferWidth, int framebufferHeight) {
        int w = framebufferWidth / 16;
        int h = framebufferHeight / 9;
        int base = Math.min(w, h);

        int min;
        int max = Mth.clamp(Math.min(framebufferWidth / 12, h) / 26, 1, 9);
        if (max > 1) {
            min = Mth.clamp(base / 64, 2, 9);
        } else {
            min = 1;
        }

        int auto;
        if (min > 1) {
            double step = base > 150 ? 40. : base > 100 ? 36. : 32.;
            int i = (int) (base / step);
            int j = (int) (Math.max(w, h) / step);
            double v1 = base / (i * 32.);
            if (v1 > 40 / 32. || j > i) {
                auto = Mth.clamp(i + 1, min, max);
            } else {
                auto = Mth.clamp(i, min, max);
            }
        } else {
            auto = 1;
        }
        assert min <= auto && auto <= max;
        return min << 8 | auto << 4 | max;
    }

    /**
     * Get the default or preferred typeface set by user.
     *
     * @return the selected typeface
     */
    @Nonnull
    protected Typeface onGetSelectedTypeface() {
        return Objects.requireNonNullElse(mDefaultTypeface, Typeface.SANS_SERIF);
    }

    /**
     * Get the default or preferred typeface set by user.
     *
     * @return the selected typeface
     */
    @Nonnull
    public static Typeface getSelectedTypeface() {
        return sInstance == null ? Typeface.SANS_SERIF : sInstance.onGetSelectedTypeface();
    }

    /**
     * Whether to enable RTL support, it should always be true.
     *
     * @return whether RTL is supported
     */
    @ApiStatus.Experimental
    public boolean hasRtlSupport() {
        return true;
    }

    @ApiStatus.Experimental
    @Nonnull
    public InputStream getResourceStream(@Nonnull String res, @Nonnull String path) throws IOException {
        InputStream stream = MeteorGL.class.getResourceAsStream("/assets/" + res + "/" + path);
        if (stream == null) {
            throw new FileNotFoundException();
        }
        return stream;
    }

    @ApiStatus.Experimental
    @Nonnull
    public ReadableByteChannel getResourceChannel(@Nonnull String res, @Nonnull String path) throws IOException {
        return Channels.newChannel(getResourceStream(res, path));
    }

    /**
     * Get the view manager of the application window (i.e. main window).
     *
     * @return window view manager
     */
    @ApiStatus.Internal
    public ViewManager getViewManager() {
        return mDecor;
    }

    @Override
    public void close() {
        try {
            synchronized (Core.class) {
                if (mUiThread != null) {
                    LOGGER.info(MARKER, "Quiting UI thread");
                    try {
                        Core.getUiHandlerAsync().post(() -> mUiLooper.quitSafely());
                        mUiThread.join(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (mRenderThread != null && mRenderThread.isAlive()) {
                    LOGGER.info(MARKER, "Quiting render thread");
                    try {
                        mRenderThread.join(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (MinecraftClient.getInstance().getWindow() != null) {
                MinecraftClient.getInstance().getWindow().close();
                LOGGER.info(MARKER, "Closed main window");
            }
            GLFWMonitorCallback cb = glfwSetMonitorCallback(null);
            if (cb != null) {
                cb.free();
            }
        } finally {
            Core.terminate();
        }
        LOGGER.info(MARKER, "Stopped");
    }

    class ViewRootImpl extends ViewRoot {

        private final Rect mGlobalRect = new Rect();

        @Nonnull
        @Override
        protected Canvas beginRecording(int width, int height) {
            GLSurfaceCanvas canvas = GLSurfaceCanvas.getInstance();
            canvas.reset(width, height);
            return canvas;
        }

        @Override
        protected boolean dispatchTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View v = mView.findFocus();
                if (v instanceof EditText) {
                    v.getGlobalVisibleRect(mGlobalRect);
                    if (!mGlobalRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                        v.clearFocus();
                    }
                }
            }
            return super.dispatchTouchEvent(event);
        }

        @Override
        protected void onKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getKeyCode() == KeyEvent.KEY_ESCAPE) {
                    View v = mView.findFocus();
                    if (v instanceof EditText) {
                        mView.requestFocus();
                    } else {
                        mOnBackPressedDispatcher.onBackPressed();
                    }
                }
            }
        }

        private void flushDrawCommands(GLSurfaceCanvas canvas, GLFramebuffer framebuffer) {
            synchronized (mRenderLock) {
                if (mRedrawn) {
                    mRedrawn = false;
                    canvas.draw(framebuffer);
                }
            }
        }

        @Override
        public void playSoundEffect(int effectId) {
        }

        @Override
        public boolean performHapticFeedback(int effectId, boolean always) {
            return false;
        }

        @Override
        protected void applyPointerIcon(int pointerType) {
            Core.executeOnMainThread(() -> glfwSetCursor(MinecraftClient.getInstance().getWindow().getHandle(),
                    PointerIcon.getSystemIcon(pointerType).getHandle()));
        }

        ContextMenuBuilder mContextMenu;
        MenuHelper mContextMenuHelper;

        @Override
        public boolean showContextMenuForChild(View originalView, float x, float y) {
            if (mContextMenuHelper != null) {
                mContextMenuHelper.dismiss();
                mContextMenuHelper = null;
            }

            if (mContextMenu == null) {
                mContextMenu = new ContextMenuBuilder();
                //mContextMenu.setCallback(callback);
            } else {
                mContextMenu.clearAll();
            }

            final MenuHelper helper;
            final boolean isPopup = !Float.isNaN(x) && !Float.isNaN(y);
            if (isPopup) {
                helper = mContextMenu.showPopup(originalView, x, y);
            } else {
                helper = mContextMenu.showPopup(originalView, 0, 0);
            }

            if (helper != null) {
                //helper.setPresenterCallback(callback);
            }

            mContextMenuHelper = helper;
            return helper != null;
        }
    }

    class HostCallbacks extends FragmentHostCallback<Object> implements
            ViewModelStoreOwner,
            OnBackPressedDispatcherOwner {
        HostCallbacks() {
            super(new Handler(Looper.myLooper()));
            assert Core.isOnUiThread();
        }

        @Nullable
        @Override
        public Object onGetHost() {
            // intentionally null
            return null;
        }

        @Nullable
        @Override
        public View onFindViewById(int id) {
            return mDecor.findViewById(id);
        }

        @Nonnull
        @Override
        public ViewModelStore getViewModelStore() {
            return mViewModelStore;
        }

        @Nonnull
        @Override
        public OnBackPressedDispatcher getOnBackPressedDispatcher() {
            return mOnBackPressedDispatcher;
        }

        @Nonnull
        @Override
        public Lifecycle getLifecycle() {
            return mLifecycleRegistry;
        }
    }
}

