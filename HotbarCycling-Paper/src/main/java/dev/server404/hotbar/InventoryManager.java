package dev.server404.hotbar;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Rotates the virtual 3x3 group associated with the currently selected hotbar slot.
 * The currently active virtual cell is represented by the real hotbar slot.
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
        int nextIndex = group.nextIndex();

        ItemStack current = cloneOrNull(player.getInventory().getItem(hotbarSlot));
        ItemStack next = group.get(nextIndex);

        // Return the currently held stack to its virtual cell.
        group.set(activeIndex, current);

        // Remove the next stack from virtual storage before placing it in the hotbar.
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
