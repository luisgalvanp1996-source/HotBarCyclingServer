package dev.server404.hotbar;

import java.util.UUID;

/** Nine groups per player, one group for each hotbar slot. */
public final class PlayerHotbarData {
    public static final int GROUP_COUNT = 9;

    private final UUID playerId;
    private final HotbarGroup[] groups = new HotbarGroup[GROUP_COUNT];

    public PlayerHotbarData(UUID playerId) {
        this.playerId = playerId;
        for (int i = 0; i < GROUP_COUNT; i++) {
            groups[i] = new HotbarGroup();
        }
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public HotbarGroup getGroup(int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot >= GROUP_COUNT) {
            throw new IndexOutOfBoundsException("Hotbar slot must be between 0 and 8");
        }
        return groups[hotbarSlot];
    }
}
