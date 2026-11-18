package dev.server404.hotbar;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.network.chat.Component;

/** Vanilla configuration screen opened from Mod Menu. */
public final class HotbarCyclingConfigScreen extends Screen {
    private final Screen parent;

    public HotbarCyclingConfigScreen(Screen parent) {
        super(Component.translatable("screen.hotbarcycling.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 110;
        int y = this.height / 4;

        addRenderableWidget(Button.builder(skipEmptyLabel(), button -> {
            HotbarCyclingConfig.setSkipEmpty(!HotbarCyclingConfig.skipEmpty());
            button.setMessage(skipEmptyLabel());
            HotbarCyclingClient.syncSettings();
        }).bounds(x, y, 220, 20).build());

        addRenderableWidget(Button.builder(reverseLabel(), button -> {
            HotbarCyclingConfig.setReverseRotation(!HotbarCyclingConfig.reverseRotation());
            button.setMessage(reverseLabel());
            HotbarCyclingClient.syncSettings();
        }).bounds(x, y + 26, 220, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("screen.hotbarcycling.config.controls"),
                button -> this.minecraft.setScreenAndShow(new ControlsScreen(this, this.minecraft.options))
        ).bounds(x, y + 58, 220, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"), button -> onClose()
        ).bounds(x, y + 90, 220, 20).build());
    }

    private static Component skipEmptyLabel() {
        return Component.translatable("screen.hotbarcycling.config.skip_empty",
                Component.translatable(HotbarCyclingConfig.skipEmpty() ? "options.on" : "options.off"));
    }

    private static Component reverseLabel() {
        return Component.translatable("screen.hotbarcycling.config.reverse",
                Component.translatable(HotbarCyclingConfig.reverseRotation() ? "screen.hotbarcycling.config.backward" : "screen.hotbarcycling.config.forward"));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(parent);
    }
}
