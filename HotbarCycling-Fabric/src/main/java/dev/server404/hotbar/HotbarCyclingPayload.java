package dev.server404.hotbar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Two-byte protocol shared with Paper:
 * C2S [1, 0] = rotate the server-side group for the selected hotbar slot.
 * S2C [2, activeIndex] = rotation accepted; [3, 0] = permission denied.
 */
public record HotbarCyclingPayload(int opcode, int value) implements CustomPacketPayload {
    public static final Identifier ID =
            Identifier.fromNamespaceAndPath("hotbarcycling", "main");

    public static final Type<HotbarCyclingPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, HotbarCyclingPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeByte(payload.opcode());
                        buf.writeByte(payload.value());
                    },
                    buf -> new HotbarCyclingPayload(
                            buf.readUnsignedByte(),
                            buf.readUnsignedByte()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
