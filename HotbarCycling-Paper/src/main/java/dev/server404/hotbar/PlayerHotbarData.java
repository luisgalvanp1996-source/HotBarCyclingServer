package dev.server404.hotbar;

import java.util.UUID;

/** Nine virtual groups per player, one for each real hotbar slot. */
public final class PlayerHotbarData {
    public static final int GROUP_COUNT = 9;

    private final UUID playerId;
    private final HotbarGroup[] groups = new HotbarGroup[GROUP_COUNT];

    private boolean skipEmpty = true;
    private boolean reverseRotation;
    private boolean editorOpen;
    private int editorHotbarSlot = -1;

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

    public boolean isSkipEmpty() {
        return skipEmpty;
    }

    public void setSkipEmpty(boolean skipEmpty) {
        this.skipEmpty = skipEmpty;
    }

    public boolean isReverseRotation() {
        return reverseRotation;
    }

    public void setReverseRotation(boolean reverseRotation) {
        this.reverseRotation = reverseRotation;
    }

    public boolean isEditorOpen() {
        return editorOpen;
    }

    public int getEditorHotbarSlot() {
        return editorHotbarSlot;
    }

    public void setEditorOpen(boolean editorOpen, int hotbarSlot) {
        this.editorOpen = editorOpen;
        this.editorHotbarSlot = editorOpen ? hotbarSlot : -1;
    }
}
