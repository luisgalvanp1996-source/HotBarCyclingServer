package dev.server404.hotbar;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;

/** Stores each player's virtual groups and preferences in a UUID-named YAML file. */
public final class PlayerDataManager {
    private final JavaPlugin plugin;
    private final File playerFolder;

    public PlayerDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerFolder = new File(plugin.getDataFolder(), "players");
        if (!playerFolder.exists() && !playerFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create player data directory.");
        }
    }

    public PlayerHotbarData load(UUID uuid) {
        PlayerHotbarData data = new PlayerHotbarData(uuid);
        File file = fileFor(uuid);
        if (!file.exists()) return data;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        data.setSkipEmpty(yaml.getBoolean("settings.skip-empty", true));
        data.setReverseRotation(yaml.getBoolean("settings.reverse-rotation", false));
        data.setEditorOpen(yaml.getBoolean("editor.open", false),
                yaml.getInt("editor.hotbar-slot", -1));

        for (int groupIndex = 0; groupIndex < 9; groupIndex++) {
            String path = "groups." + groupIndex;
            ConfigurationSection section = yaml.getConfigurationSection(path);
            if (section == null) continue;

            HotbarGroup group = data.getGroup(groupIndex);
            group.setActiveIndex(clamp(section.getInt("active", 0)));

            for (int itemIndex = 0; itemIndex < 9; itemIndex++) {
                String encoded = section.getString("items." + itemIndex);
                if (encoded == null || encoded.isBlank()) continue;
                try {
                    byte[] bytes = Base64.getDecoder().decode(encoded);
                    group.set(itemIndex, ItemStack.deserializeBytes(bytes));
                } catch (Exception exception) {
                    plugin.getLogger().log(Level.WARNING,
                            "Could not load item " + itemIndex + " for " + uuid, exception);
                }
            }
        }
        return data;
    }

    public void save(PlayerHotbarData data) {
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("settings.skip-empty", data.isSkipEmpty());
        yaml.set("settings.reverse-rotation", data.isReverseRotation());
        yaml.set("editor.open", data.isEditorOpen());
        yaml.set("editor.hotbar-slot", data.getEditorHotbarSlot());

        for (int groupIndex = 0; groupIndex < 9; groupIndex++) {
            HotbarGroup group = data.getGroup(groupIndex);
            String path = "groups." + groupIndex;
            yaml.set(path + ".active", group.getActiveIndex());

            for (int itemIndex = 0; itemIndex < 9; itemIndex++) {
                ItemStack item = group.get(itemIndex);
                if (item == null || item.getType().isAir()) continue;

                String encoded = Base64.getEncoder().encodeToString(item.serializeAsBytes());
                yaml.set(path + ".items." + itemIndex, encoded);
            }
        }

        try {
            yaml.save(fileFor(data.getPlayerId()));
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not save Hotbar Cycling data for " + data.getPlayerId(), exception);
        }
    }

    private File fileFor(UUID uuid) {
        return new File(playerFolder, uuid + ".yml");
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(8, value));
    }
}
