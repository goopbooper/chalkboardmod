package com.example.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BoardUpdatePayload(BlockPos controllerPos, int width, int height, int[] pixels) implements CustomPacketPayload {
    public static final Type<BoardUpdatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("chalkboard", "board_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoardUpdatePayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeBlockPos(payload.controllerPos);
            buf.writeInt(payload.width);
            buf.writeInt(payload.height);
            buf.writeVarIntArray(payload.pixels); // Use writeVarIntArray here
        },
        buf -> {
            BlockPos pos = buf.readBlockPos();
            int w = buf.readInt();
            int h = buf.readInt();
            int[] pix = buf.readVarIntArray(); // Use readVarIntArray here
            return new BoardUpdatePayload(pos, w, h, pix);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}