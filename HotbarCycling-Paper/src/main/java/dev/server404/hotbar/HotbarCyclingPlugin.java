package dev.server404.hotbar;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paper endpoint for the Hotbar Cycling client.
 *
 * Wire protocol on hotbarcycling:main:
 * C2S [1, 0] = rotate the group for the server-selected hotbar slot.
 * S2C [2, activeIndex] = rotation accepted.
 * S2C [3, 0] = permission denied.
 * S2C [4, 0] = no other non-empty cell is available.
 */
public final class HotbarCyclingPlugin extends JavaPlugin implements Listener, PluginMessageListener {
    public static final String CHANNEL = "hotbarcycling:main";

    private static final byte OP_ROTATE = 1;
    private static final byte OP_ACK = 2;
    private static final byte OP_REJECT = 3;
    private static final byte OP_NO_ITEMS = 4;
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
            sessions.put(player.getUniqueId(), dataManager.load(player.getUniqueId()));
        }

        getLogger().info("HotbarCycling 0.3.0 enabled on " + CHANNEL);
    }

    @Override
    public void onDisable() {
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
        sessions.put(player.getUniqueId(), dataManager.load(player.getUniqueId()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerHotbarData data = sessions.remove(uuid);
        if (data != null) dataManager.save(data);
        lastRequest.remove(uuid);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel) || message == null || message.length != 2) return;
        if (message[0] != OP_ROTATE || message[1] != 0) return;

        if (!player.hasPermission("hotbarcycling.use")) {
            sendReply(player, OP_REJECT, 0);
            return;
        }

        long now = System.nanoTime();
        Long previous = lastRequest.put(player.getUniqueId(), now);
        if (previous != null && now - previous < MIN_REQUEST_INTERVAL_NANOS) {
            return;
        }

        // Do not accept a hotbar slot from the client. Read the authoritative
        // selected slot from the server-side player inventory.
        PlayerHotbarData data = sessions.computeIfAbsent(
                player.getUniqueId(), id -> dataManager.load(id));

        boolean rotated = inventoryManager.rotateSelectedGroup(player, data);
        if (rotated) {
            int activeIndex = data.getGroup(player.getInventory().getHeldItemSlot()).getActiveIndex();
            sendReply(player, OP_ACK, activeIndex);
        } else {
            sendReply(player, OP_NO_ITEMS, 0);
        }
    }

    private void sendReply(Player player, byte opcode, int value) {
        player.sendPluginMessage(this, CHANNEL, new byte[]{opcode, (byte) value});
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

        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            int slot = player.getInventory().getHeldItemSlot();
            HotbarGroup group = data.getGroup(slot);
            player.sendMessage(ChatColor.GOLD + "Hotbar Cycling"
                    + ChatColor.GRAY + " | Group " + (slot + 1)
                    + " | Active cell " + (group.getActiveIndex() + 1) + "/9");
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "Usage: /hc <status|rotate>");
        return true;
    }
}
