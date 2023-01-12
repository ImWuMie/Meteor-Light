package meteordevelopment.meteorclient.glrender.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.viaversion.viaversion.libs.kyori.adventure.text.Component;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.glrender.*;
import meteordevelopment.meteorclient.glrender.fragment.*;
import meteordevelopment.meteorclient.glrender.lifecycle.*;
import meteordevelopment.meteorclient.glrender.math.Matrix4;
import meteordevelopment.meteorclient.glrender.math.Rect;
import meteordevelopment.meteorclient.glrender.opengl.GLCore;
import meteordevelopment.meteorclient.glrender.opengl.GLFramebuffer;
import meteordevelopment.meteorclient.glrender.opengl.GLSurfaceCanvas;
import meteordevelopment.meteorclient.glrender.opengl.GLTexture;
import meteordevelopment.meteorclient.glrender.text.Editable;
import meteordevelopment.meteorclient.glrender.text.Selection;
import meteordevelopment.meteorclient.glrender.text.TextUtils;
import meteordevelopment.meteorclient.glrender.view.*;
import meteordevelopment.meteorclient.glrender.view.menu.ContextMenuBuilder;
import meteordevelopment.meteorclient.glrender.view.menu.MenuHelper;
import meteordevelopment.meteorclient.glrender.widget.CoordinatorLayout;
import meteordevelopment.meteorclient.glrender.widget.EditText;
import meteordevelopment.meteorclient.gui.screens.NewJelloScreen;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashReport;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import static meteordevelopment.meteorclient.glrender.MeteorGL.*;
import static meteordevelopment.meteorclient.glrender.opengl.GLCore.*;
import static org.lwjgl.glfw.GLFW.*;

@ApiStatus.Internal
public final class UIManager implements LifecycleOwner {

    // the logger marker
    static final Marker MARKER = MarkerManager.getMarker("UIManager");

    // configs
    static volatile boolean sPlaySoundOnLoaded;

    // the global instance, lazily init
    private static volatile UIManager sInstance;

    private static final int fragment_container = 0x01020007;

    // minecraft
    private final MinecraftClient minecraft = MinecraftClient.getInstance();

    // minecraft window
    private final Window mWindow = minecraft.getWindow();

    private final MatrixStack mEmptyPoseStack = new MatrixStack();

    // the UI thread
    private final Thread mUiThread;
    private volatile Looper mLooper;
    private volatile boolean mRunning;

    // the view root impl
    private volatile ViewRootImpl mRoot;

    // the top-level view of the window
    private CoordinatorLayout mDecor;
    private FragmentContainerView mFragmentContainerView;


    /// Task Handling \\\

    // elapsed time from a screen open in milliseconds, Render thread
    private long mElapsedTimeMillis;

    // time for drawing, Render thread
    private long mFrameTimeNanos;


    /// Rendering \\\

    // the UI framebuffer
    private GLFramebuffer mFramebuffer;
    GLSurfaceCanvas mCanvas;
    private final Matrix4 mProjectionMatrix = new Matrix4();


    /// User Interface \\\

    // indicates the current Modern UI screen, updated on main thread
    @Nullable
    volatile MuiScreen mScreen;

    private boolean mFirstScreenOpened = false;


    /// Lifecycle \\\

    LifecycleRegistry mFragmentLifecycleRegistry;
    private final OnBackPressedDispatcher mOnBackPressedDispatcher =
            new OnBackPressedDispatcher(() -> minecraft.send(this::onBackPressed));

    private ViewModelStore mViewModelStore;
    volatile FragmentController mFragmentController;


    /// Input Event \\\

    private int mButtonState;

    public GLSurfaceCanvas getmCanvas() {
        return mCanvas;
    }

    private UIManager() {
        mUiThread = new Thread(this::run, "UI thread");
        mUiThread.start();

        mRunning = true;
    }

    public static void initialize() {
        Core.checkRenderThread();
        assert sInstance == null;
        sInstance = new UIManager();
        LOGGER.info(MARKER, "UI manager initialized");
    }

    public static void initializeRenderer() {
        Core.checkRenderThread();
        assert sInstance != null;
        sInstance.mCanvas = GLSurfaceCanvas.initialize();
        //glEnable(GL_MULTISAMPLE);
        GLFramebuffer framebuffer = new GLFramebuffer(4);
        if (sInstance.mCanvas == null) {
            LOGGER.info(MARKER, "Disabled UI renderer");
        } else {
            framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT0, GL_RGBA8);
            framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT1, GL_RGBA8);
            framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT2, GL_RGBA8);
            framebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT3, GL_RGBA8);
            // no depth buffer
            framebuffer.addRenderbufferAttachment(GL_STENCIL_ATTACHMENT, GL_STENCIL_INDEX8);
            framebuffer.setDrawBuffer(GL_COLOR_ATTACHMENT0);
        }
        sInstance.mFramebuffer = framebuffer;
    }

    @Nonnull
   public static UIManager getInstance() {
        // Do not push into stack, since it's lazily init
        if (sInstance == null)
            throw new IllegalStateException("UI manager was never initialized. " +
                    "Please check whether FML threw an exception before.");
        return sInstance;
    }

    private void run() {
        init();
        while (mRunning) {
            try {
                Looper.loop();
            } catch (Throwable e) {
                LOGGER.error(MARKER, "An error occurred on UI thread", e);
                // dev can add breakpoints
                if (mRunning) {
                    continue;
                } else {
                    minecraft.send(this::dump);
                    minecraft.send(() -> MinecraftClient.printCrashReport(
                            CrashReport.create(e, "Exception on UI thread")));
                }
            }
            break;
        }
        LOGGER.info(MARKER, "Quited UI thread");
    }


    void open(@Nonnull Fragment fragment) {
        if (!minecraft.isOnThread()) {
            throw new IllegalStateException("Not called from main thread");
        }
        minecraft.setScreen(new SimpleScreen(this, fragment));
    }

    void onBackPressed() {
        final MuiScreen screen = mScreen;
        if (screen == null)
            return;

        minecraft.setScreen(null);
    }

    /**
     * Get elapsed time in UI, update every frame. Internal use only.
     *
     * @return drawing time in milliseconds
     */
    static long getElapsedTime() {
        if (sInstance == null) {
            return Core.timeMillis();
        }
        return sInstance.mElapsedTimeMillis;
    }

    /**
     * Get synced frame time, update every frame
     *
     * @return frame time in nanoseconds
     */
    static long getFrameTimeNanos() {
        if (sInstance == null) {
            return Core.timeNanos();
        }
        return sInstance.mFrameTimeNanos;
    }

    CoordinatorLayout getDecorView() {
        return mDecor;
    }

    @Nonnull
    @Override
    public Lifecycle getLifecycle() {
        // STRONG reference "this"
        return mFragmentLifecycleRegistry;
    }

    // Called when open a screen from Modern UI, or back to the screen
    void initScreen(@Nonnull MuiScreen screen) {
        if (mScreen != screen) {
            if (mScreen != null) {
                LOGGER.warn(MARKER, "You cannot set multiple screens.");
                removed();
            }
            mRoot.mHandler.post(this::suppressLayoutTransition);
            mFragmentController.getFragmentManager().beginTransaction()
                    .add(fragment_container, screen.getFragment(), "main")
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
            mRoot.mHandler.post(this::restoreLayoutTransition);
        }
        mScreen = screen;
        // ensure it's resized
        resize();
    }

    void suppressLayoutTransition() {
        LayoutTransition transition = mDecor.getLayoutTransition();
        transition.disableTransitionType(LayoutTransition.APPEARING);
        transition.disableTransitionType(LayoutTransition.DISAPPEARING);
    }

    void restoreLayoutTransition() {
        LayoutTransition transition = mDecor.getLayoutTransition();
        transition.enableTransitionType(LayoutTransition.APPEARING);
        transition.enableTransitionType(LayoutTransition.DISAPPEARING);
    }

    Screen createCapsErrorScreen() {
        final String glRenderer = glGetString(GL_RENDERER);
        final String glVersion = glGetString(GL_VERSION);
        String extensions = String.join(", ", GLCore.getUnsupportedList());
        return new NewJelloScreen();
    }

    private void init() {
        long startTime = System.nanoTime();
        mLooper = Core.initUiThread();

        mRoot = this.new ViewRootImpl();

        mDecor = new CoordinatorLayout();
        // make the root view clickable through, so that views can lose focus
        mDecor.setClickable(true);
        mDecor.setFocusableInTouchMode(true);
        mDecor.setWillNotDraw(true);
        mDecor.setId(R.id.content);
        updateLayoutDir(false);

        mFragmentContainerView = new FragmentContainerView();
        mFragmentContainerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mFragmentContainerView.setWillNotDraw(true);
        mFragmentContainerView.setId(fragment_container);
        mDecor.addView(mFragmentContainerView);

        mDecor.setLayoutTransition(new LayoutTransition());

        mRoot.setView(mDecor);
        resize();

        mDecor.getViewTreeObserver().addOnScrollChangedListener(() -> onHoverMove(false));

        mFragmentLifecycleRegistry = new LifecycleRegistry(this);
        mViewModelStore = new ViewModelStore();
        mFragmentController = FragmentController.createController(this.new HostCallbacks());

        mFragmentController.attachHost(null);

        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mFragmentController.dispatchCreate();

        mFragmentController.dispatchActivityCreated();
        mFragmentController.execPendingActions();

        mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mFragmentController.dispatchStart();

        LOGGER.info(MARKER, "UI thread initialized in {}ms", (System.nanoTime() - startTime) / 1000000);

        // test stuff
        /*Paint paint = Paint.take();
        paint.setStrokeWidth(6);
        int c = (int) mElapsedTimeMillis / 300;
        c = Math.min(c, 8);
        float[] pts = new float[c * 2 + 2];
        pts[0] = 90;
        pts[1] = 30;
        for (int i = 0; i < c; i++) {
            pts[2 + i * 2] = Math.min((i + 2) * 60, mElapsedTimeMillis / 5) + 30;
            if ((i & 1) == 0) {
                if (mElapsedTimeMillis >= (i + 2) * 300) {
                    pts[3 + i * 2] = 90;
                } else {
                    pts[3 + i * 2] = 30 + (mElapsedTimeMillis % 300) / 5f;
                }
            } else {
                if (mElapsedTimeMillis >= (i + 2) * 300) {
                    pts[3 + i * 2] = 30;
                } else {
                    pts[3 + i * 2] = 90 - (mElapsedTimeMillis % 300) / 5f;
                }
            }
        }
        mCanvas.drawStripLines(pts, paint);

        paint.setRGBA(255, 180, 100, 255);
        mCanvas.drawCircle(90, 30, 6, paint);
        mCanvas.drawCircle(150, 90, 6, paint);
        mCanvas.drawCircle(210, 30, 6, paint);
        mCanvas.drawCircle(270, 90, 6, paint);
        mCanvas.drawCircle(330, 30, 6, paint);
        mCanvas.drawCircle(390, 90, 6, paint);
        mCanvas.drawCircle(450, 30, 6, paint);
        mCanvas.drawCircle(510, 90, 6, paint);
        mCanvas.drawCircle(570, 30, 6, paint);*/
    }

 //   @EventHandler
    private void finish(/*TickEvent.Pre e*/) {
     //   if (minecraft.getWindow().shouldClose()) {
            LOGGER.info(MARKER, "Quiting UI thread");

            mFragmentController.dispatchStop();
            mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

            mFragmentController.dispatchDestroy();
            mFragmentLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

            // must delay, some messages are not enqueued
            // currently it is a bit longer than a game tick
            mRoot.mHandler.postDelayed(mLooper::quitSafely, 60);
    //    }
    }


    void onHoverMove(boolean natural) {
        final long now = Core.timeNanos();
        float x = (float) (minecraft.mouse.getX() *
                mWindow.getWidth() / mWindow.getScaledWidth());
        float y = (float) (minecraft.mouse.getY() *
                mWindow.getHeight() / mWindow.getFramebufferHeight());
        MotionEvent event = MotionEvent.obtain(now, MotionEvent.ACTION_HOVER_MOVE,
                x, y, 0);
        mRoot.enqueueInputEvent(event);
        //mPendingRepostCursorEvent = false;
        if (natural && mButtonState > 0) {
            event = MotionEvent.obtain(now, MotionEvent.ACTION_MOVE, 0, x, y, 0, mButtonState, 0);
            mRoot.enqueueInputEvent(event);
        }
    }


    void onPostMouseInput(@Nonnull MouseButtonEvent event) {
        // We should ensure (overlay == null && screen != null)
        // and the screen must be a mui screen
        if (minecraft.getOverlay() == null && mScreen != null) {
            //ModernUI.LOGGER.info(MARKER, "Button: {} {} {}", event.getButton(), event.getAction(), event.getMods());
            final long now = Core.timeNanos();
            float x = (float) (minecraft.mouse.getX() *
                    mWindow.getWidth() / mWindow.getScaledWidth());
            float y = (float) (minecraft.mouse.getY() *
                    mWindow.getHeight() / mWindow.getScaledHeight());
            int buttonState = 0;
            for (int i = 0; i < 5; i++) {
                if (glfwGetMouseButton(mWindow.getHandle(), i) == GLFW_PRESS) {
                    buttonState |= 1 << i;
                }
            }
            mButtonState = buttonState;
            int action = event.action == KeyAction.Press ?
                    MotionEvent.ACTION_BUTTON_PRESS : MotionEvent.ACTION_BUTTON_RELEASE;
            int touchAction = event.action == KeyAction.Press ?
                    MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP;
            int actionButton = 1 << event.button;
            MotionEvent ev = MotionEvent.obtain(now, action, actionButton,
                    x, y, 0, buttonState, 0);
            mRoot.enqueueInputEvent(ev);
            if ((touchAction == MotionEvent.ACTION_DOWN && (buttonState ^ actionButton) == 0)
                    || (touchAction == MotionEvent.ACTION_UP && buttonState == 0)) {
                ev = MotionEvent.obtain(now, touchAction, actionButton,
                        x, y, 0, buttonState, 0);
                mRoot.enqueueInputEvent(ev);
                //LOGGER.info("Enqueue mouse event: {}", ev);
            }
        }
    }

    // Hook method, DO NOT CALL
    private void onScroll(double scrollX, double scrollY) {
        if (mScreen != null) {
            final long now = Core.timeNanos();
            final Window window = mWindow;
            final Mouse mouseHandler = minecraft.mouse;
            float x = (float) (mouseHandler.getX()) *
                    window.getWidth() / window.getScaledWidth();
            float y = (float) (mouseHandler.getY() *
                    window.getHeight() / window.getScaledHeight());
            MotionEvent event = MotionEvent.obtain(now, MotionEvent.ACTION_SCROLL,
                    x, y, 0);
            event.setAxisValue(MotionEvent.AXIS_HSCROLL, (float) scrollX);
            event.setAxisValue(MotionEvent.AXIS_VSCROLL, (float) scrollY);
            mRoot.enqueueInputEvent(event);
        }
    }

    void onPostKeyInput(@Nonnull meteordevelopment.meteorclient.glrender.view.KeyEvent event) {
        if (mScreen != null) {
            int action = event.getAction() == GLFW_RELEASE ? meteordevelopment.meteorclient.glrender.view.KeyEvent.ACTION_UP : meteordevelopment.meteorclient.glrender.view.KeyEvent.ACTION_DOWN;
            meteordevelopment.meteorclient.glrender.view.KeyEvent keyEvent = meteordevelopment.meteorclient.glrender.view.KeyEvent.obtain(Core.timeNanos(), action, event.getKeyCode(), 0,
                    event.getModifiers(), event.getScanCode(), 0);
            mRoot.enqueueInputEvent(keyEvent);
        }
    }

    void dump() {
        StringBuilder builder = new StringBuilder();
        try (var w = new PrintWriter(new StringBuilderWriter(builder))) {
            dump(w);
        }
        String str = builder.toString();
        LOGGER.info(MARKER, str);
    }

    private void dump(@Nonnull PrintWriter pw) {
    }

    boolean onCharTyped(char ch) {
        /*if (popup != null) {
            return popup.charTyped(codePoint, modifiers);
        }*/
        /*if (mKeyboard != null) {
            return mKeyboard.onCharTyped(codePoint, modifiers);
        }*/
        Message msg = Message.obtain(mRoot.mHandler, () -> {
            if (mDecor.findFocus() instanceof EditText text) {
                final Editable content = text.getText();
                int selStart = text.getSelectionStart();
                int selEnd = text.getSelectionEnd();
                if (selStart >= 0 && selEnd >= 0) {
                    Selection.setSelection(content, Math.max(selStart, selEnd));
                    content.replace(Math.min(selStart, selEnd), Math.max(selStart, selEnd), String.valueOf(ch));
                }
            }
        });
        msg.setAsynchronous(true);
        msg.sendToTarget();
        return true;//root.charTyped(codePoint, modifiers);
    }

    void render() {
        if (mCanvas == null) {
            if (mScreen != null) {
                String error = "";
                int x = (mWindow.getScaledWidth() - minecraft.textRenderer.getWidth(error)) / 2;
                int y = (mWindow.getScaledHeight() - 8) / 2;
                minecraft.textRenderer.draw(mEmptyPoseStack, error, x, y, 0xFFFF0000);
            }
            return;
        }
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.activeTexture(GL_TEXTURE0);
        RenderSystem.disableDepthTest();

        // blend alpha correctly, since the Minecraft.mainRenderTarget has no alpha (always 1)
        // and our framebuffer is always a transparent layer
        RenderSystem.blendFuncSeparate(GL_ONE, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        final int oldVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        final int oldProgram = glGetInteger(GL_CURRENT_PROGRAM);

        // TODO need multiple canvas instances, tooltip shares this now, but different thread; remove Z transform
        mCanvas.setProjection(mProjectionMatrix.setOrthographic(
                mWindow.getWidth(), mWindow.getHeight(), 0, meteordevelopment.meteorclient.glrender.Window.LAST_SYSTEM_WINDOW * 2 + 1,
                true));

        glBindVertexArray(oldVertexArray);
        glUseProgram(oldProgram);

        RenderSystem.defaultBlendFunc();
        // force changing Blaze3D state
        RenderSystem.bindTexture(DEFAULT_TEXTURE);
    }

    private final Runnable mResizeRunnable = () -> mRoot.setFrame(mWindow.getWidth(), mWindow.getHeight());


    void resize() {
        if (mRoot != null) {
            mRoot.mHandler.post(mResizeRunnable);
        }
    }

    void updateLayoutDir(boolean forceRTL) {
        if (mDecor == null) {
            return;
        }
        boolean layoutRtl = forceRTL ||
                TextUtils.getLayoutDirectionFromLocale(MeteorGL.getSelectedLocale()) == View.LAYOUT_DIRECTION_RTL;
        mDecor.setLayoutDirection(layoutRtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LOCALE);
        mDecor.requestLayout();
        TooltipRenderer.sLayoutRTL = layoutRtl;
    }

    void removed() {
        MuiScreen screen = mScreen;
        if (screen == null) {
            return;
        }
        mRoot.mHandler.post(this::suppressLayoutTransition);
        mFragmentController.getFragmentManager().beginTransaction()
                .remove(screen.getFragment())
                .runOnCommit(() -> mFragmentContainerView.removeAllViews())
                .commit();
        mRoot.mHandler.post(this::restoreLayoutTransition);
        mScreen = null;
        glfwSetCursor(mWindow.getHandle(), MemoryUtil.NULL);
    }

    class ViewRootImpl extends ViewRoot {

        private final Rect mGlobalRect = new Rect();

        ContextMenuBuilder mContextMenu;
        MenuHelper mContextMenuHelper;

        @Override
        protected Canvas beginRecording(int width, int height) {
            if (mCanvas != null) {
                mCanvas.reset(width, height);
            }
            return mCanvas;
        }

        @Override
        protected boolean dispatchTouchEvent(MotionEvent event) {
            if (mScreen != null && event.getAction() == MotionEvent.ACTION_DOWN) {
                View v = mDecor.findFocus();
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
        protected void onKeyEvent(meteordevelopment.meteorclient.glrender.view.KeyEvent event) {
            final MuiScreen screen = mScreen;
            if (screen != null && event.getAction() == meteordevelopment.meteorclient.glrender.view.KeyEvent.ACTION_DOWN) {
                boolean back = false;
            }
        }

        @Override
        public void playSoundEffect(int effectId) {}

        @Override
        public boolean performHapticFeedback(int effectId, boolean always) {
            return false;
        }

        protected void applyPointerIcon(int pointerType) {
            minecraft.send(() -> glfwSetCursor(mWindow.getHandle(),
                    PointerIcon.getSystemIcon(pointerType).getHandle()));
        }

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

            /*if (helper != null) {
                helper.setPresenterCallback(callback);
            }*/

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
            return mFragmentLifecycleRegistry;
        }
    }
}

