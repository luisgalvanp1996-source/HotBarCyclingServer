package dev.server404.hotbar;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class HotbarEditorHolder implements InventoryHolder {
    private final UUID playerId;
    private final int hotbarSlot;
    private final int activeIndex;
    private Inventory inventory;

    public HotbarEditorHolder(UUID playerId, int hotbarSlot, int activeIndex) {
        this.playerId = playerId;
        this.hotbarSlot = hotbarSlot;
        this.activeIndex = activeIndex;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getHotbarSlot() {
        return hotbarSlot;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
