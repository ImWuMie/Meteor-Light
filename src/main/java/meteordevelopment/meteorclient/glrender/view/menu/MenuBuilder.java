/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package meteordevelopment.meteorclient.glrender.view.menu;

import meteordevelopment.meteorclient.glrender.drawable.Drawable;
import meteordevelopment.meteorclient.glrender.view.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of the {@link Menu} interface for creating a
 * standard menu UI.
 */
public class MenuBuilder implements Menu {

    private static final int[] sCategoryToOrder = new int[]{
            1, /* No category */
            4, /* CONTAINER */
            5, /* SYSTEM */
            3, /* SECONDARY */
            2, /* ALTERNATIVE */
            0, /* SELECTED_ALTERNATIVE */
    };

    /**
     * Whether the shortcuts should be qwerty-accessible. Use isQwertyMode()
     * instead of accessing this directly.
     */
    private boolean mQwertyMode;

    /**
     * Whether the shortcuts should be visible on menus. Use isShortcutsVisible()
     * instead of accessing this directly.
     */
    private boolean mShortcutsVisible;

    /**
     * Callback that will receive the various menu-related events generated by
     * this class. Use getCallback to get a reference to the callback.
     */
    private Callback mCallback;

    /**
     * Contains all of the items for this menu
     */
    private final ArrayList<MenuItemImpl> mItems;

    /**
     * Contains only the items that are currently visible.  This will be created/refreshed from
     * {@link #getVisibleItems()}
     */
    private final ArrayList<MenuItemImpl> mVisibleItems;
    /**
     * Whether or not the items (or any one item's shown state) has changed since it was last
     * fetched from {@link #getVisibleItems()}
     */
    private boolean mIsVisibleItemsStale;

    /**
     * Contains only the items that should appear in the Action Bar, if present.
     */
    private final ArrayList<MenuItemImpl> mActionItems;
    /**
     * Contains items that should NOT appear in the Action Bar, if present.
     */
    private final ArrayList<MenuItemImpl> mNonActionItems;

    /**
     * Whether or not the items (or any one item's action state) has changed since it was
     * last fetched.
     */
    private boolean mIsActionItemsStale;

    /**
     * Default value for how added items should show in the action list.
     */
    private int mDefaultShowAsAction = MenuItem.SHOW_AS_ACTION_NEVER;

    /**
     * Current use case is Context Menus: As Views populate the context menu, each one has
     * extra information that should be passed along.  This is the current menu info that
     * should be set on all items added to this menu.
     */
    private Object mCurrentMenuInfo;

    /**
     * Header title for menu types that have a header (context and submenus)
     */
    CharSequence mHeaderTitle;
    /**
     * Header icon for menu types that have a header and support icons (context)
     */
    Drawable mHeaderIcon;
    /**
     * Header custom view for menu types that have a header and support custom views (context)
     */
    View mHeaderView;

    /**
     * Prevents onItemsChanged from doing its junk, useful for batching commands
     * that may individually call onItemsChanged.
     */
    private boolean mPreventDispatchingItemsChanged = false;
    private boolean mItemsChangedWhileDispatchPrevented = false;

    private boolean mOptionalIconsVisible = false;

    private boolean mIsClosing = false;

    private final ArrayList<MenuItemImpl> mTempShortcutItemList = new ArrayList<>();

    private final CopyOnWriteArrayList<WeakReference<MenuPresenter>> mPresenters = new CopyOnWriteArrayList<>();

    /**
     * Currently expanded menu item; must be collapsed when we clear.
     */
    private MenuItemImpl mExpandedItem;

    /**
     * Whether group dividers are enabled.
     */
    private boolean mGroupDividerEnabled = false;

    /**
     * Called by menu to notify of close and selection changes.
     */
    public interface Callback {

        /**
         * Called when a menu item is selected.
         *
         * @param menu The menu that is the parent of the item
         * @param item The menu item that is selected
         * @return whether the menu item selection was handled
         */
        boolean onMenuItemSelected(MenuBuilder menu, MenuItem item);

        /**
         * Called when the mode of the menu changes (for example, from icon to expanded).
         *
         * @param menu the menu that has changed modes
         */
        void onMenuModeChange(MenuBuilder menu);
    }

    /**
     * Called by menu items to execute their associated action
     */
    public interface ItemInvoker {
        boolean invokeItem(MenuItemImpl item);
    }

    public MenuBuilder() {
        mItems = new ArrayList<>();

        mVisibleItems = new ArrayList<>();
        mIsVisibleItemsStale = true;

        mActionItems = new ArrayList<>();
        mNonActionItems = new ArrayList<>();
        mIsActionItemsStale = true;

        setShortcutsVisibleInner(true);
    }

    public MenuBuilder setDefaultShowAsAction(int defaultShowAsAction) {
        mDefaultShowAsAction = defaultShowAsAction;
        return this;
    }

    /**
     * Add a presenter to this menu. This will only hold a WeakReference;
     * you do not need to explicitly remove a presenter, but you can using
     * {@link #removeMenuPresenter(MenuPresenter)}.
     *
     * @param presenter The presenter to add
     */
    public void addMenuPresenter(@Nonnull MenuPresenter presenter) {
        mPresenters.add(new WeakReference<>(presenter));
        presenter.initForMenu(this);
        mIsActionItemsStale = true;
    }

    /**
     * Remove a presenter from this menu. That presenter will no longer
     * receive notifications of updates to this menu's data.
     *
     * @param presenter The presenter to remove
     */
    public void removeMenuPresenter(MenuPresenter presenter) {
        for (WeakReference<MenuPresenter> ref : mPresenters) {
            final MenuPresenter item = ref.get();
            if (item == null || item == presenter) {
                mPresenters.remove(ref);
            }
        }
    }

    private void dispatchPresenterUpdate(boolean cleared) {
        if (mPresenters.isEmpty()) return;

        stopDispatchingItemsChanged();
        for (WeakReference<MenuPresenter> ref : mPresenters) {
            final MenuPresenter presenter = ref.get();
            if (presenter == null) {
                mPresenters.remove(ref);
            } else {
                presenter.updateMenuView(cleared);
            }
        }
        startDispatchingItemsChanged();
    }

    private boolean dispatchSubMenuSelected(SubMenuBuilder subMenu,
                                            MenuPresenter preferredPresenter) {
        if (mPresenters.isEmpty()) return false;

        boolean result = false;

        // Try the preferred presenter first.
        if (preferredPresenter != null) {
            result = preferredPresenter.onSubMenuSelected(subMenu);
        }

        for (WeakReference<MenuPresenter> ref : mPresenters) {
            final MenuPresenter presenter = ref.get();
            if (presenter == null) {
                mPresenters.remove(ref);
            } else if (!result) {
                result = presenter.onSubMenuSelected(subMenu);
            }
        }
        return result;
    }

    public void setCallback(Callback cb) {
        mCallback = cb;
    }

    /**
     * Adds an item to the menu.  The other add methods funnel to this.
     */
    @Nonnull
    private MenuItemImpl addInternal(int group, int id, int categoryOrder, @Nullable CharSequence title) {
        final int ordering = getOrdering(categoryOrder);

        final MenuItemImpl item = new MenuItemImpl(this, group, id, categoryOrder, ordering, title,
                mDefaultShowAsAction);

        if (mCurrentMenuInfo != null) {
            // Pass along the current menu info
            item.setMenuInfo(mCurrentMenuInfo);
        }

        mItems.add(findInsertIndex(mItems, ordering), item);
        onItemsChanged(true);

        return item;
    }

    @Nonnull
    @Override
    public MenuItem add(@Nullable CharSequence title) {
        return addInternal(NONE, NONE, NONE, title);
    }

    @Nonnull
    @Override
    public MenuItem add(int group, int id, int categoryOrder, @Nullable CharSequence title) {
        return addInternal(group, id, categoryOrder, title);
    }

    @Nonnull
    @Override
    public SubMenu addSubMenu(@Nullable CharSequence title) {
        return addSubMenu(NONE, NONE, NONE, title);
    }

    @Nonnull
    @Override
    public SubMenu addSubMenu(int group, int id, int categoryOrder, @Nullable CharSequence title) {
        final MenuItemImpl item = addInternal(group, id, categoryOrder, title);
        final SubMenuBuilder subMenu = new SubMenuBuilder(this, item);
        item.setSubMenu(subMenu);

        return subMenu;
    }

    @Override
    public void setGroupDividerEnabled(boolean groupDividerEnabled) {
        mGroupDividerEnabled = groupDividerEnabled;
    }

    public boolean isGroupDividerEnabled() {
        return mGroupDividerEnabled;
    }

    @Override
    public void removeItem(int id) {
        removeItemAtInt(findItemIndex(id), true);
    }

    @Override
    public void removeGroup(int group) {
        final int i = findGroupIndex(group);

        if (i >= 0) {
            final int maxRemovable = mItems.size() - i;
            int numRemoved = 0;
            while ((numRemoved++ < maxRemovable) && (mItems.get(i).getGroupId() == group)) {
                // Don't force update for each one, this method will do it at the end
                removeItemAtInt(i, false);
            }

            // Notify menu views
            onItemsChanged(true);
        }
    }

    /**
     * Remove the item at the given index and optionally forces menu views to
     * update.
     *
     * @param index                     The index of the item to be removed. If this index is
     *                                  invalid an exception is thrown.
     * @param updateChildrenOnMenuViews Whether to force update on menu views.
     *                                  Please make sure you eventually call this after your batch of
     *                                  removals.
     */
    private void removeItemAtInt(int index, boolean updateChildrenOnMenuViews) {
        if ((index < 0) || (index >= mItems.size())) return;

        mItems.remove(index);

        if (updateChildrenOnMenuViews) onItemsChanged(true);
    }

    public void removeItemAt(int index) {
        removeItemAtInt(index, true);
    }

    public void clearAll() {
        mPreventDispatchingItemsChanged = true;
        clear();
        clearHeader();
        mPresenters.clear();
        mPreventDispatchingItemsChanged = false;
        mItemsChangedWhileDispatchPrevented = false;
        onItemsChanged(true);
    }

    @Override
    public void clear() {
        if (mExpandedItem != null) {
            collapseItemActionView(mExpandedItem);
        }
        mItems.clear();

        onItemsChanged(true);
    }

    void setExclusiveItemChecked(@Nonnull MenuItem item) {
        final int group = item.getGroupId();

        for (MenuItemImpl curItem : mItems) {
            if (curItem.getGroupId() == group) {
                if (!curItem.isExclusiveCheckable()) continue;
                if (!curItem.isCheckable()) continue;

                // Check the item meant to be checked, uncheck the others (that are in the group)
                curItem.setCheckedInt(curItem == item);
            }
        }
    }

    @Override
    public void setGroupCheckable(int group, boolean checkable, boolean exclusive) {
        for (MenuItemImpl item : mItems) {
            if (item.getGroupId() == group) {
                item.setExclusiveCheckable(exclusive);
                item.setCheckable(checkable);
            }
        }
    }

    @Override
    public void setGroupVisible(int group, boolean visible) {
        // We handle the notification of items being changed ourselves, so we use setVisibleInt rather
        // than setVisible and at the end notify of items being changed

        boolean changedAtLeastOneItem = false;
        for (MenuItemImpl item : mItems) {
            if (item.getGroupId() == group) {
                if (item.setVisibleInt(visible)) changedAtLeastOneItem = true;
            }
        }

        if (changedAtLeastOneItem) onItemsChanged(true);
    }

    @Override
    public void setGroupEnabled(int group, boolean enabled) {
        for (MenuItemImpl item : mItems) {
            if (item.getGroupId() == group) {
                item.setEnabled(enabled);
            }
        }
    }

    @Override
    public boolean hasVisibleItems() {
        final int size = size();

        for (int i = 0; i < size; i++) {
            MenuItemImpl item = mItems.get(i);
            if (item.isVisible()) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    @Override
    public MenuItem findItem(int id) {
        final int size = size();
        for (int i = 0; i < size; i++) {
            MenuItemImpl item = mItems.get(i);
            if (item.getItemId() == id) {
                return item;
            } else if (item.hasSubMenu()) {
                MenuItem possibleItem = item.getSubMenu().findItem(id);

                if (possibleItem != null) {
                    return possibleItem;
                }
            }
        }

        return null;
    }

    public int findItemIndex(int id) {
        final int size = size();

        for (int i = 0; i < size; i++) {
            MenuItemImpl item = mItems.get(i);
            if (item.getItemId() == id) {
                return i;
            }
        }

        return -1;
    }

    public int findGroupIndex(int group) {
        return findGroupIndex(group, 0);
    }

    public int findGroupIndex(int group, int start) {
        final int size = size();

        if (start < 0) {
            start = 0;
        }

        for (int i = start; i < size; i++) {
            final MenuItemImpl item = mItems.get(i);

            if (item.getGroupId() == group) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public int size() {
        return mItems.size();
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public MenuItem getItem(int index) {
        return mItems.get(index);
    }

    @Override
    public boolean isShortcutKey(int keyCode, @Nonnull KeyEvent event) {
        return findItemWithShortcutForKey(event) != null;
    }

    @Override
    public void setQwertyMode(boolean isQwerty) {
        mQwertyMode = isQwerty;

        onItemsChanged(false);
    }

    /**
     * Returns the ordering across all items. This will grab the category from
     * the upper bits, find out how to order the category with respect to other
     * categories, and combine it with the lower bits.
     *
     * @param categoryOrder The category order for a particular item (if it has
     *                      not been or/add with a category, the default category is
     *                      assumed).
     * @return An ordering integer that can be used to order this item across
     * all the items (even from other categories).
     */
    private static int getOrdering(int categoryOrder) {
        final int index = (categoryOrder & CATEGORY_MASK) >> CATEGORY_SHIFT;

        if (index < 0 || index >= sCategoryToOrder.length) {
            throw new IllegalArgumentException("order does not contain a valid category.");
        }

        return (sCategoryToOrder[index] << CATEGORY_SHIFT) | (categoryOrder & USER_MASK);
    }

    /**
     * @return whether the menu shortcuts are in qwerty mode or not
     */
    boolean isQwertyMode() {
        return mQwertyMode;
    }

    /**
     * Sets whether the shortcuts should be visible on menus.  Devices without hardware
     * key input will never make shortcuts visible even if this method is passed 'true'.
     *
     * @param shortcutsVisible Whether shortcuts should be visible (if true and a
     *                         menu item does not have a shortcut defined, that item will
     *                         still NOT show a shortcut)
     */
    public void setShortcutsVisible(boolean shortcutsVisible) {
        if (mShortcutsVisible == shortcutsVisible) return;

        setShortcutsVisibleInner(shortcutsVisible);
        onItemsChanged(false);
    }

    private void setShortcutsVisibleInner(boolean shortcutsVisible) {
        mShortcutsVisible = shortcutsVisible;
    }

    /**
     * @return Whether shortcuts should be visible on menus.
     */
    public boolean isShortcutsVisible() {
        return mShortcutsVisible;
    }

    boolean dispatchMenuItemSelected(MenuBuilder menu, MenuItem item) {
        return mCallback != null && mCallback.onMenuItemSelected(menu, item);
    }

    /**
     * Dispatch a mode change event to this menu's callback.
     */
    public void changeMenuMode() {
        if (mCallback != null) {
            mCallback.onMenuModeChange(this);
        }
    }

    private static int findInsertIndex(@Nonnull ArrayList<MenuItemImpl> items, int ordering) {
        for (int i = items.size() - 1; i >= 0; i--) {
            MenuItemImpl item = items.get(i);
            if (item.getOrdering() <= ordering) {
                return i + 1;
            }
        }

        return 0;
    }

    @Override
    public boolean performShortcut(int keyCode, @Nonnull KeyEvent event, int flags) {
        final MenuItemImpl item = findItemWithShortcutForKey(event);

        boolean handled = false;

        if (item != null) {
            handled = performItemAction(item, flags);
        }

        if ((flags & FLAG_ALWAYS_PERFORM_CLOSE) != 0) {
            close(true /* closeAllMenus */);
        }

        return handled;
    }

    /*
     * This function will return all the menu and sub-menu items that can
     * be directly (the shortcut directly corresponds) and indirectly
     * (the ALT-enabled char corresponds to the shortcut) associated
     * with the keyCode.
     */
    void findItemsWithShortcutForKey(List<MenuItemImpl> items, @Nonnull KeyEvent event) {
        final boolean qwerty = isQwertyMode();
        final int modifierState = event.getModifiers();
        final char possibleChar = event.getMappedChar();
        // The delete key is not mapped to '\b' so we treat it specially
        if (possibleChar == 0) {
            return;
        }

        // Look for an item whose shortcut is this key.
        for (MenuItemImpl item : mItems) {
            if (item.hasSubMenu()) {
                ((MenuBuilder) item.getSubMenu()).findItemsWithShortcutForKey(items, event);
            }
            final char shortcutChar =
                    qwerty ? item.getAlphabeticShortcut() : item.getNumericShortcut();
            final int shortcutModifiers =
                    qwerty ? item.getAlphabeticModifiers() : item.getNumericModifiers();
            final boolean isModifiersExactMatch = (modifierState & SUPPORTED_MODIFIERS_MASK)
                    == (shortcutModifiers & SUPPORTED_MODIFIERS_MASK);
            if (isModifiersExactMatch && (shortcutChar != 0) &&
                    (shortcutChar == possibleChar) && item.isEnabled()) {
                items.add(item);
            }
        }
    }

    /*
     * We want to return the menu item associated with the key, but if there is no
     * ambiguity (i.e. there is only one menu item corresponding to the key) we want
     * to return it even if it's not an exact match; this allow the user to
     * _not_ use the ALT key for example, making the use of shortcuts slightly more
     * user-friendly. An example is on the G1, '!' and '1' are on the same key, and
     * in Gmail, Menu+1 will trigger Menu+! (the actual shortcut).
     *
     * On the other hand, if two (or more) shortcuts corresponds to the same key,
     * we have to only return the exact match.
     */
    MenuItemImpl findItemWithShortcutForKey(KeyEvent event) {
        // Get all items that can be associated directly or indirectly with the keyCode
        ArrayList<MenuItemImpl> items = mTempShortcutItemList;
        items.clear();
        findItemsWithShortcutForKey(items, event);

        if (items.isEmpty()) {
            return null;
        }
        return items.get(0);
    }

    @Override
    public boolean performIdentifierAction(int id, int flags) {
        // Look for an item whose identifier is the id.
        return performItemAction(findItem(id), flags);
    }

    public boolean performItemAction(MenuItem item, int flags) {
        return performItemAction(item, null, flags);
    }

    public boolean performItemAction(MenuItem item, MenuPresenter preferredPresenter, int flags) {
        MenuItemImpl itemImpl = (MenuItemImpl) item;

        if (itemImpl == null || !itemImpl.isEnabled()) {
            return false;
        }

        boolean invoked = itemImpl.invoke();

        final ActionProvider provider = item.getActionProvider();
        final boolean providerHasSubMenu = provider != null && provider.hasSubMenu();
        if (itemImpl.hasCollapsibleActionView()) {
            invoked |= itemImpl.expandActionView();
            if (invoked) {
                close(true /* closeAllMenus */);
            }
        } else if (itemImpl.hasSubMenu() || providerHasSubMenu) {
            if (!itemImpl.hasSubMenu()) {
                itemImpl.setSubMenu(new SubMenuBuilder(this, itemImpl));
            }

            final SubMenuBuilder subMenu = (SubMenuBuilder) itemImpl.getSubMenu();
            if (providerHasSubMenu) {
                provider.onPrepareSubMenu(subMenu);
            }
            invoked |= dispatchSubMenuSelected(subMenu, preferredPresenter);
            if (!invoked) {
                close(true /* closeAllMenus */);
            }
        } else {
            if ((flags & FLAG_PERFORM_NO_CLOSE) == 0) {
                close(true /* closeAllMenus */);
            }
        }

        return invoked;
    }

    /**
     * Closes the menu.
     *
     * @param closeAllMenus {@code true} if all displayed menus and submenus
     *                      should be completely closed (as when a menu item is
     *                      selected) or {@code false} if only this menu should
     *                      be closed
     */
    public final void close(boolean closeAllMenus) {
        if (mIsClosing) return;

        mIsClosing = true;
        for (WeakReference<MenuPresenter> ref : mPresenters) {
            final MenuPresenter presenter = ref.get();
            if (presenter == null) {
                mPresenters.remove(ref);
            } else {
                presenter.onCloseMenu(this, closeAllMenus);
            }
        }
        mIsClosing = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        close(true /* closeAllMenus */);
    }

    /**
     * Called when an item is added or removed.
     *
     * @param structureChanged true if the menu structure changed,
     *                         false if only item properties changed.
     *                         (Visibility is a structural property since it affects layout.)
     */
    public void onItemsChanged(boolean structureChanged) {
        if (!mPreventDispatchingItemsChanged) {
            if (structureChanged) {
                mIsVisibleItemsStale = true;
                mIsActionItemsStale = true;
            }

            dispatchPresenterUpdate(structureChanged);
        } else {
            mItemsChangedWhileDispatchPrevented = true;
        }
    }

    /**
     * Stop dispatching item changed events to presenters until
     * {@link #startDispatchingItemsChanged()} is called. Useful when
     * many menu operations are going to be performed as a batch.
     */
    public void stopDispatchingItemsChanged() {
        if (!mPreventDispatchingItemsChanged) {
            mPreventDispatchingItemsChanged = true;
            mItemsChangedWhileDispatchPrevented = false;
        }
    }

    public void startDispatchingItemsChanged() {
        mPreventDispatchingItemsChanged = false;

        if (mItemsChangedWhileDispatchPrevented) {
            mItemsChangedWhileDispatchPrevented = false;
            onItemsChanged(true);
        }
    }

    /**
     * Called by {@link MenuItemImpl} when its visible flag is changed.
     *
     * @param item The item that has gone through a visibility change.
     */
    void onItemVisibleChanged(MenuItemImpl item) {
        // Notify of items being changed
        mIsVisibleItemsStale = true;
        onItemsChanged(true);
    }

    /**
     * Called by {@link MenuItemImpl} when its action request status is changed.
     *
     * @param item The item that has gone through a change in action request status.
     */
    void onItemActionRequestChanged(MenuItemImpl item) {
        // Notify of items being changed
        mIsActionItemsStale = true;
        onItemsChanged(true);
    }

    @Nonnull
    public ArrayList<MenuItemImpl> getVisibleItems() {
        if (!mIsVisibleItemsStale) return mVisibleItems;

        // Refresh the visible items
        mVisibleItems.clear();

        for (MenuItemImpl item : mItems) {
            if (item.isVisible())
                mVisibleItems.add(item);
        }

        mIsVisibleItemsStale = false;
        mIsActionItemsStale = true;

        return mVisibleItems;
    }

    /**
     * This method determines which menu items get to be 'action items' that will appear
     * in an action bar and which items should be 'overflow items' in a secondary menu.
     * The rules are as follows:
     *
     * <p>Items are considered for inclusion in the order specified within the menu.
     * There is a limit of mMaxActionItems as a total count, optionally including the overflow
     * menu button itself. This is a soft limit; if an item shares a group ID with an item
     * previously included as an action item, the new item will stay with its group and become
     * an action item itself even if it breaks the max item count limit. This is done to
     * limit the conceptual complexity of the items presented within an action bar. Only a few
     * unrelated concepts should be presented to the user in this space, and groups are treated
     * as a single concept.
     *
     * <p>There is also a hard limit of consumed measurable space: mActionWidthLimit. This
     * limit may be broken by a single item that exceeds the remaining space, but no further
     * items may be added. If an item that is part of a group cannot fit within the remaining
     * measured width, the entire group will be demoted to overflow. This is done to ensure room
     * for navigation and other affordances in the action bar as well as reduce general UI clutter.
     *
     * <p>The space freed by demoting a full group cannot be consumed by future menu items.
     * Once items begin to overflow, all future items become overflow items as well. This is
     * to avoid inadvertent reordering that may break the app's intended design.
     */
    public void flagActionItems() {
        // Important side effect: if getVisibleItems is stale it may refresh,
        // which can affect action items staleness.
        final ArrayList<MenuItemImpl> visibleItems = getVisibleItems();

        if (!mIsActionItemsStale) {
            return;
        }

        // Presenters flag action items as needed.
        boolean flagged = false;
        for (WeakReference<MenuPresenter> ref : mPresenters) {
            final MenuPresenter presenter = ref.get();
            if (presenter == null) {
                mPresenters.remove(ref);
            } else {
                flagged |= presenter.flagActionItems();
            }
        }

        mActionItems.clear();
        mNonActionItems.clear();
        if (flagged) {
            for (MenuItemImpl item : visibleItems) {
                if (item.isActionButton()) {
                    mActionItems.add(item);
                } else {
                    mNonActionItems.add(item);
                }
            }
        } else {
            // Nobody flagged anything, everything is a non-action item.
            // (This happens during a first pass with no action-item presenters.)
            mNonActionItems.addAll(getVisibleItems());
        }
        mIsActionItemsStale = false;
    }

    public ArrayList<MenuItemImpl> getActionItems() {
        flagActionItems();
        return mActionItems;
    }

    public ArrayList<MenuItemImpl> getNonActionItems() {
        flagActionItems();
        return mNonActionItems;
    }

    public void clearHeader() {
        mHeaderIcon = null;
        mHeaderTitle = null;
        mHeaderView = null;

        onItemsChanged(false);
    }

    private void setHeaderInternal(@Nullable CharSequence title, @Nullable Drawable icon, @Nullable View view) {
        if (view != null) {
            mHeaderView = view;

            // If using a custom view, then the title and icon aren't used
            mHeaderTitle = null;
            mHeaderIcon = null;
        } else {
            if (title != null) {
                mHeaderTitle = title;
            }

            if (icon != null) {
                mHeaderIcon = icon;
            }

            // If using the title or icon, then a custom view isn't used
            mHeaderView = null;
        }

        // Notify of change
        onItemsChanged(false);
    }

    /**
     * Sets the header's title. This replaces the header view. Called by the
     * builder-style methods of subclasses.
     *
     * @param title The new title.
     * @return This MenuBuilder so additional setters can be called.
     */
    protected MenuBuilder setHeaderTitleInt(CharSequence title) {
        setHeaderInternal(title, null, null);
        return this;
    }

    /**
     * Sets the header's icon. This replaces the header view. Called by the
     * builder-style methods of subclasses.
     *
     * @param icon The new icon.
     * @return This MenuBuilder so additional setters can be called.
     */
    protected MenuBuilder setHeaderIconInt(Drawable icon) {
        setHeaderInternal(null, icon, null);
        return this;
    }

    /**
     * Sets the header's view. This replaces the title and icon. Called by the
     * builder-style methods of subclasses.
     *
     * @param view The new view.
     * @return This MenuBuilder so additional setters can be called.
     */
    protected MenuBuilder setHeaderViewInt(View view) {
        setHeaderInternal(null, null, view);
        return this;
    }

    public CharSequence getHeaderTitle() {
        return mHeaderTitle;
    }

    public Drawable getHeaderIcon() {
        return mHeaderIcon;
    }

    public View getHeaderView() {
        return mHeaderView;
    }

    /**
     * Gets the root menu (if this is a submenu, find its root menu).
     *
     * @return The root menu.
     */
    public MenuBuilder getRootMenu() {
        return this;
    }

    /**
     * Sets the current menu info that is set on all items added to this menu
     * (until this is called again with different menu info, in which case that
     * one will be added to all subsequent item additions).
     *
     * @param menuInfo The extra menu information to add.
     */
    public void setCurrentMenuInfo(Object menuInfo) {
        mCurrentMenuInfo = menuInfo;
    }

    void setOptionalIconsVisible(boolean visible) {
        mOptionalIconsVisible = visible;
    }

    boolean getOptionalIconsVisible() {
        return mOptionalIconsVisible;
    }

    public boolean expandItemActionView(MenuItemImpl item) {
        if (mPresenters.isEmpty()) return false;

        boolean expanded = false;

        stopDispatchingItemsChanged();
        for (WeakReference<MenuPresenter> ref : mPresenters) {
            final MenuPresenter presenter = ref.get();
            if (presenter == null) {
                mPresenters.remove(ref);
            } else {
                expanded = presenter.expandItemActionView(this, item);
                if (expanded) {
                    break;
                }
            }
        }
        startDispatchingItemsChanged();

        if (expanded) {
            mExpandedItem = item;
        }
        return expanded;
    }

    public boolean collapseItemActionView(MenuItemImpl item) {
        if (mPresenters.isEmpty() || mExpandedItem != item) return false;

        boolean collapsed = false;

        stopDispatchingItemsChanged();
        for (WeakReference<MenuPresenter> ref : mPresenters) {
            final MenuPresenter presenter = ref.get();
            if (presenter == null) {
                mPresenters.remove(ref);
            } else {
                collapsed = presenter.collapseItemActionView(this, item);
                if (collapsed) {
                    break;
                }
            }
        }
        startDispatchingItemsChanged();

        if (collapsed) {
            mExpandedItem = null;
        }
        return collapsed;
    }

    public MenuItemImpl getExpandedItem() {
        return mExpandedItem;
    }
}
