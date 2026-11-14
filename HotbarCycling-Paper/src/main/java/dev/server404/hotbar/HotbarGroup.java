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
     * Finds the next occupied virtual cell, wrapping around the 3x3 group.
     * The currently active cell is deliberately excluded because its item
     * lives in the player's real hotbar slot, not in virtual storage.
     * Returns -1 when no other item is available.
     */
    public int nextOccupiedIndex() {
        for (int step = 1; step < SIZE; step++) {
            int candidate = (activeIndex + step) % SIZE;
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
