package dev.server404.hotbar;

import org.bukkit.inventory.ItemStack;

/** One virtual 3x3 group associated with a single hotbar position. */
public final class HotbarGroup {
    public static final int SIZE = 9;

    private final ItemStack[] items = new ItemStack[SIZE];
    private int activeIndex;

    public HotbarGroup() {
        this.activeIndex = 0;
    }

    public ItemStack get(int index) {
        checkIndex(index);
        ItemStack item = items[index];
        return isEmpty(item) ? null : item.clone();
    }

    public void set(int index, ItemStack item) {
        checkIndex(index);
        items[index] = isEmpty(item) ? null : item.clone();
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        checkIndex(activeIndex);
        this.activeIndex = activeIndex;
    }

    /**
     * Finds the next occupied virtual cell, wrapping around the group.
     * Returns -1 if no other occupied cell exists.
     */
    public int nextOccupiedIndex(boolean reverse) {
        int direction = reverse ? -1 : 1;
        for (int step = 1; step < SIZE; step++) {
            int candidate = Math.floorMod(activeIndex + direction * step, SIZE);
            if (!isEmpty(items[candidate])) {
                return candidate;
            }
        }
        return -1;
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    private static void checkIndex(int index) {
        if (index < 0 || index >= SIZE) {
            throw new IndexOutOfBoundsException("Group index must be between 0 and 8: " + index);
        }
    }
}
