package dev.server404.hotbar;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;


public final class HotbarCyclingClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("hotbarcycling", "main")
    );

    private static final KeyMapping ROTATE_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.hotbarcycling.rotate",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_G,
                    CATEGORY
            )
    );

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.serverboundPlay().register(
                HotbarCyclingPayload.TYPE, HotbarCyclingPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
                HotbarCyclingPayload.TYPE, HotbarCyclingPayload.CODEC
        );

        ClientPlayNetworking.registerGlobalReceiver(
                HotbarCyclingPayload.TYPE,
                (payload, context) -> {
                    var player = context.client().player;
                    if (player == null) return;

                    if (payload.opcode() == 2) {
                        player.sendSystemMessage(Component.literal(
                                "Hotbar Cycling: rotación completada · posición "
                                        + (payload.value() + 1) + "/9"
                        ));
                    } else if (payload.opcode() == 3) {
                        player.sendSystemMessage(Component.literal(
                                "Hotbar Cycling: no tienes permiso para usarlo."
                        ));
                    }
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
    while (ROTATE_KEY.consumeClick()) {
        if (client.player == null || client.level == null || client.gui.screen() != null) {
            continue;
        }

        if (!ClientPlayNetworking.canSend(HotbarCyclingPayload.TYPE)) {
            client.player.sendSystemMessage(Component.literal(
                    "Hotbar Cycling: el servidor no anuncia el canal del plugin."
            ));
            continue;
        }

        if (client.options.keyShift.isDown()) {
            // Shift + G: abrir editor del grupo seleccionado.
            ClientPlayNetworking.send(new HotbarCyclingPayload(5, 0));
        } else {
            // G: rotación inteligente.
            ClientPlayNetworking.send(new HotbarCyclingPayload(1, 0));
        }
    }
});
    }
}
