package dev.server404.hotbar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Paper endpoint for Hotbar Cycling.
 *
 * C2S [1,0] = rotate selected group.
 * C2S [5,0] = open selected group's 3x3 editor.
 * S2C [2,index] = rotation accepted; [3,0] = permission denied;
 * S2C [4,0] = no other non-empty item available.
 */
public final class HotbarCyclingPlugin extends JavaPlugin implements Listener, PluginMessageListener {
    public static final String CHANNEL = "hotbarcycling:main";

    private static final byte OP_ROTATE = 1;
    private static final byte OP_ACK = 2;
    private static final byte OP_REJECT = 3;
    private static final byte OP_NO_ITEMS = 4;
    private static final byte OP_OPEN_EDITOR = 5;
    private static final byte OP_SET_SETTINGS = 6;
    private static final long MIN_REQUEST_INTERVAL_NANOS = 75_000_000L;

    private final Map<UUID, PlayerHotbarData> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastRequest = new ConcurrentHashMap<>();

    private PlayerDataManager dataManager;
    private InventoryManager inventoryManager;

    @Override
    public void onEnable() {
        dataManager = new PlayerDataManager(this);
        inventoryManager = new InventoryManager(dataManager);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);

        for (Player player : getServer().getOnlinePlayers()) {
            PlayerHotbarData data = dataManager.load(player.getUniqueId());
            sessions.put(player.getUniqueId(), data);
            recoverInterruptedEditor(player, data);
        }

        getLogger().info("HotbarCycling " + getDescription().getVersion() + " enabled on " + CHANNEL);
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof HotbarEditorHolder) {
                player.closeInventory();
            }
        }
        for (PlayerHotbarData data : sessions.values()) {
            dataManager.save(data);
        }
        sessions.clear();
        lastRequest.clear();

        getServer().getMessenger().unregisterIncomingPluginChannel(this, CHANNEL, this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerHotbarData data = dataManager.load(player.getUniqueId());
        sessions.put(player.getUniqueId(), data);
        recoverInterruptedEditor(player, data);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerHotbarData data = sessions.get(uuid);
        if (data != null) dataManager.save(data);
        lastRequest.remove(uuid);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel) || message == null || message.length != 2) return;

        // Client-local preferences are synchronized on join and whenever the Mod Menu screen changes them.
        if (message[0] == OP_SET_SETTINGS) {
            int flags = message[1] & 0xFF;
            if (flags > 3 || !player.hasPermission("hotbarcycling.use")) return;
            PlayerHotbarData settingsData = sessions.computeIfAbsent(
                    player.getUniqueId(), id -> dataManager.load(id));
            settingsData.setSkipEmpty((flags & 1) != 0);
            settingsData.setReverseRotation((flags & 2) != 0);
            dataManager.save(settingsData);
            return;
        }

        if (message[1] != 0) return;
        if (message[0] != OP_ROTATE && message[0] != OP_OPEN_EDITOR) return;

        if (!player.hasPermission("hotbarcycling.use")) {
            sendReply(player, OP_REJECT, 0);
            return;
        }

        long now = System.nanoTime();
        Long previous = lastRequest.put(player.getUniqueId(), now);
        if (previous != null && now - previous < MIN_REQUEST_INTERVAL_NANOS) return;

        PlayerHotbarData data = sessions.computeIfAbsent(
                player.getUniqueId(), id -> dataManager.load(id));

        if (message[0] == OP_OPEN_EDITOR) {
            openEditor(player, data);
            return;
        }

        boolean rotated = inventoryManager.rotateSelectedGroup(player, data);
        if (rotated) {
            int activeIndex = data.getGroup(player.getInventory().getHeldItemSlot()).getActiveIndex();
            sendReply(player, OP_ACK, activeIndex);
        } else {
            sendReply(player, OP_NO_ITEMS, 0);
        }
    }

    private void openEditor(Player player, PlayerHotbarData data) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof HotbarEditorHolder) {
            return;
        }
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof HotbarSettingsHolder) {
            player.closeInventory();
        }

        int hotbarSlot = player.getInventory().getHeldItemSlot();
        HotbarGroup group = data.getGroup(hotbarSlot);
        int activeIndex = group.getActiveIndex();

        HotbarEditorHolder holder = new HotbarEditorHolder(
                player.getUniqueId(), hotbarSlot, activeIndex);
        Inventory inventory = Bukkit.createInventory(
                holder, InventoryType.DISPENSER,
                ChatColor.DARK_GRAY + "Hotbar Cycling | Grupo " + (hotbarSlot + 1));
        holder.setInventory(inventory);

        for (int i = 0; i < HotbarGroup.SIZE; i++) {
            ItemStack item = i == activeIndex
                    ? cloneOrNull(player.getInventory().getItem(hotbarSlot))
                    : group.get(i);
            inventory.setItem(i, item);
            // While editing, the complete 3x3 contents are held in the YAML-backed group.
            group.set(i, item);
        }

        data.setEditorOpen(true, hotbarSlot);
        // Persist the editor snapshot before removing the real hotbar item.
        dataManager.save(data);
        player.getInventory().setItem(hotbarSlot, null);
        player.updateInventory();
        player.openInventory(inventory);
    }

    private void openSettings(Player player, PlayerHotbarData data) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof HotbarEditorHolder
                || player.getOpenInventory().getTopInventory().getHolder() instanceof HotbarSettingsHolder) {
            player.closeInventory();
        }

        HotbarSettingsHolder holder = new HotbarSettingsHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(
                holder, InventoryType.DISPENSER,
                ChatColor.DARK_GRAY + "Hotbar Cycling | Configuración");
        holder.setInventory(inventory);

        inventory.setItem(1, menuItem(Material.HOPPER,
                ChatColor.YELLOW + "Saltar espacios vacíos",
                data.isSkipEmpty() ? ChatColor.GREEN + "Activado" : ChatColor.RED + "Desactivado",
                ChatColor.GRAY + "Clic para cambiar"));
        inventory.setItem(4, menuItem(Material.COMPASS,
                ChatColor.YELLOW + "Dirección de rotación",
                data.isReverseRotation()
                        ? ChatColor.AQUA + "Hacia atrás"
                        : ChatColor.AQUA + "Hacia adelante",
                ChatColor.GRAY + "Clic para cambiar"));
        inventory.setItem(7, menuItem(Material.BARRIER,
                ChatColor.RED + "Cerrar configuración",
                ChatColor.GRAY + "Volver al juego"));
        player.openInventory(inventory);
    }

    private ItemStack menuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        Object holder = top.getHolder();

        if (holder instanceof HotbarSettingsHolder settingsHolder) {
            event.setCancelled(true);
            if (!settingsHolder.getPlayerId().equals(player.getUniqueId())) return;

            PlayerHotbarData data = sessions.computeIfAbsent(
                    player.getUniqueId(), id -> dataManager.load(id));
            int slot = event.getRawSlot();

            if (slot == 1) {
                data.setSkipEmpty(!data.isSkipEmpty());
                dataManager.save(data);
                Bukkit.getScheduler().runTask(this, () -> {
                    if (player.isOnline()) openSettings(player, data);
                });
            } else if (slot == 4) {
                data.setReverseRotation(!data.isReverseRotation());
                dataManager.save(data);
                Bukkit.getScheduler().runTask(this, () -> {
                    if (player.isOnline()) openSettings(player, data);
                });
            } else if (slot == 7) {
                player.closeInventory();
            }
            return;
        }

        if (holder instanceof HotbarEditorHolder editorHolder) {
            if (!editorHolder.getPlayerId().equals(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }

            int protectedHotbarRawSlot = 36 + editorHolder.getHotbarSlot();
            if (event.getRawSlot() == protectedHotbarRawSlot
                    || event.getHotbarButton() == editorHolder.getHotbarSlot()
                    && event.getClick().name().equals("NUMBER_KEY")) {
                event.setCancelled(true);
                return;
            }

            Bukkit.getScheduler().runTask(this, () -> syncEditor(player, editorHolder));
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Object holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof HotbarEditorHolder editorHolder)) return;

        int protectedHotbarRawSlot = 36 + editorHolder.getHotbarSlot();
        if (event.getRawSlots().contains(protectedHotbarRawSlot)) {
            event.setCancelled(true);
            return;
        }

        Bukkit.getScheduler().runTask(this, () -> syncEditor(player, editorHolder));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getHolder() instanceof HotbarEditorHolder editorHolder) {
            finishEditor(player, event.getInventory(), editorHolder);
        }
    }

    private void syncEditor(Player player, HotbarEditorHolder holder) {
        if (!player.isOnline()) return;
        if (player.getOpenInventory().getTopInventory().getHolder() != holder) return;
        PlayerHotbarData data = sessions.get(player.getUniqueId());
        if (data == null) return;

        HotbarGroup group = data.getGroup(holder.getHotbarSlot());
        Inventory inventory = holder.getInventory();
        for (int i = 0; i < HotbarGroup.SIZE; i++) {
            group.set(i, inventory.getItem(i));
        }
        dataManager.save(data);
    }

    private void finishEditor(Player player, Inventory inventory, HotbarEditorHolder holder) {
        PlayerHotbarData data = sessions.get(player.getUniqueId());
        if (data == null) return;

        HotbarGroup group = data.getGroup(holder.getHotbarSlot());
        for (int i = 0; i < HotbarGroup.SIZE; i++) {
            group.set(i, inventory.getItem(i));
        }

        ItemStack activeItem = group.get(holder.getActiveIndex());
        group.set(holder.getActiveIndex(), null);

        int destination = holder.getHotbarSlot();
        ItemStack existing = player.getInventory().getItem(destination);
        if (isEmpty(existing)) {
            player.getInventory().setItem(destination, activeItem);
        } else if (!isEmpty(activeItem)) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(activeItem);
            leftovers.values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item));
        }

        data.setEditorOpen(false, -1);
        player.updateInventory();
        dataManager.save(data);
    }

    private void recoverInterruptedEditor(Player player, PlayerHotbarData data) {
        if (!data.isEditorOpen()) return;

        int hotbarSlot = data.getEditorHotbarSlot();
        if (hotbarSlot < 0 || hotbarSlot >= 9) {
            data.setEditorOpen(false, -1);
            dataManager.save(data);
            return;
        }

        HotbarGroup group = data.getGroup(hotbarSlot);
        int activeIndex = group.getActiveIndex();
        ItemStack savedActive = group.get(activeIndex);
        ItemStack existing = player.getInventory().getItem(hotbarSlot);

        if (!isEmpty(savedActive)) {
            if (isEmpty(existing)) {
                player.getInventory().setItem(hotbarSlot, savedActive);
            } else if (existing.isSimilar(savedActive)) {
                // If the server stopped after the YAML snapshot was saved but
                // before the hotbar slot was cleared, the same stack exists in
                // both places. Keep the player inventory copy only.
                if (existing.getAmount() < savedActive.getAmount()) {
                    ItemStack remainder = savedActive.clone();
                    remainder.setAmount(savedActive.getAmount() - existing.getAmount());
                    Map<Integer, ItemStack> leftovers = player.getInventory().addItem(remainder);
                    leftovers.values().forEach(item ->
                            player.getWorld().dropItemNaturally(player.getLocation(), item));
                }
            } else {
                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(savedActive);
                leftovers.values().forEach(item ->
                        player.getWorld().dropItemNaturally(player.getLocation(), item));
            }
        }

        group.set(activeIndex, null);
        data.setEditorOpen(false, -1);
        player.updateInventory();
        dataManager.save(data);
    }

    private void sendReply(Player player, byte opcode, int value) {
        player.sendPluginMessage(this, CHANNEL, new byte[]{opcode, (byte) value});
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    private static ItemStack cloneOrNull(ItemStack item) {
        return isEmpty(item) ? null : item.clone();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        if (!player.hasPermission("hotbarcycling.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        PlayerHotbarData data = sessions.computeIfAbsent(
                player.getUniqueId(), id -> dataManager.load(id));

        if (args.length == 1 && args[0].equalsIgnoreCase("rotate")) {
            boolean rotated = inventoryManager.rotateSelectedGroup(player, data);
            if (rotated) {
                player.sendMessage(ChatColor.GREEN + "Rotated group for hotbar slot "
                        + (player.getInventory().getHeldItemSlot() + 1) + ".");
            } else {
                player.sendMessage(ChatColor.YELLOW + "No other non-empty item in this group.");
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("settings")) {
            openSettings(player, data);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            int slot = player.getInventory().getHeldItemSlot();
            HotbarGroup group = data.getGroup(slot);
            player.sendMessage(ChatColor.GOLD + "Hotbar Cycling"
                    + ChatColor.GRAY + " | Group " + (slot + 1)
                    + " | Active cell " + (group.getActiveIndex() + 1) + "/9"
                    + " | Skip empty: " + data.isSkipEmpty()
                    + " | Reverse: " + data.isReverseRotation());
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "Usage: /hc <status|rotate|settings>");
        return true;
    }
}
