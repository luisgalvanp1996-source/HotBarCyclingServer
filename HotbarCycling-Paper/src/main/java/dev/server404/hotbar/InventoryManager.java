package dev.server404.hotbar;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Rotates the virtual group associated with the selected hotbar slot.
 * All inventory operations must run on Paper's main server thread.
 */
public final class InventoryManager {
    private final PlayerDataManager dataManager;

    public InventoryManager(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public boolean rotateSelectedGroup(Player player, PlayerHotbarData data) {
        int hotbarSlot = player.getInventory().getHeldItemSlot();
        HotbarGroup group = data.getGroup(hotbarSlot);

        int activeIndex = group.getActiveIndex();
        int nextIndex;

        if (data.isSkipEmpty()) {
            nextIndex = group.nextOccupiedIndex(data.isReverseRotation());
        } else {
            int direction = data.isReverseRotation() ? -1 : 1;
            nextIndex = Math.floorMod(activeIndex + direction, HotbarGroup.SIZE);
        }

        if (nextIndex < 0) {
            return false;
        }

        ItemStack current = cloneOrNull(player.getInventory().getItem(hotbarSlot));
        ItemStack next = group.get(nextIndex);

        group.set(activeIndex, current);
        group.set(nextIndex, null);
        player.getInventory().setItem(hotbarSlot, cloneOrNull(next));
        group.setActiveIndex(nextIndex);

        player.updateInventory();
        dataManager.save(data);
        return true;
    }

    private static ItemStack cloneOrNull(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0
                ? null
                : item.clone();
    }
}
