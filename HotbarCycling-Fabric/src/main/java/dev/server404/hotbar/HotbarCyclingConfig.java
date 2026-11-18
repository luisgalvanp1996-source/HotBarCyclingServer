package dev.server404.hotbar;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Client-local preferences. Minecraft key mappings themselves are stored by Minecraft in options.txt. */
public final class HotbarCyclingConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hotbarcycling.properties");
    private static boolean skipEmpty = true;
    private static boolean reverseRotation = false;

    private HotbarCyclingConfig() {}

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(FILE)) {
            properties.load(input);
            skipEmpty = Boolean.parseBoolean(properties.getProperty("skip-empty", "true"));
            reverseRotation = Boolean.parseBoolean(properties.getProperty("reverse-rotation", "false"));
        } catch (IOException exception) {
            System.err.println("[Hotbar Cycling] Could not read client config: " + exception.getMessage());
        }
    }

    public static boolean skipEmpty() { return skipEmpty; }
    public static boolean reverseRotation() { return reverseRotation; }

    public static void setSkipEmpty(boolean value) {
        skipEmpty = value;
        save();
    }

    public static void setReverseRotation(boolean value) {
        reverseRotation = value;
        save();
    }

    public static int flags() {
        return (skipEmpty ? 1 : 0) | (reverseRotation ? 2 : 0);
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Properties properties = new Properties();
            properties.setProperty("skip-empty", Boolean.toString(skipEmpty));
            properties.setProperty("reverse-rotation", Boolean.toString(reverseRotation));
            try (OutputStream output = Files.newOutputStream(FILE)) {
                properties.store(output, "Hotbar Cycling client preferences");
            }
        } catch (IOException exception) {
            System.err.println("[Hotbar Cycling] Could not save client config: " + exception.getMessage());
        }
    }
}
