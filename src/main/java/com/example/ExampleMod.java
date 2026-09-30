package com.example;

import com.example.network.BoardUpdatePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction; // <-- Added missing import
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main server/common entrypoint for the chalkboard mod. */
public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "chalkboard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ChalkItems.initialize();
        ChalkBlocks.initialize();
        ModEntityTypes.initialize();
        LOGGER.info("Hello Fabric world!");

        // Register client-to-server payload using serverboundPlay()
        PayloadTypeRegistry.serverboundPlay().register(BoardUpdatePayload.TYPE, BoardUpdatePayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(BoardUpdatePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerLevel level = (ServerLevel) context.player().level(); // Fixed level retrieval method
                BlockPos controllerPos = payload.controllerPos();
                int width = payload.width();
                int height = payload.height();
                int[] masterPixels = payload.pixels();

                Direction growthDirection = level.getBlockState(controllerPos).getValue(BoardBlock.FACING).getClockWise();
                int pWidth = 32;
                int pHeight = 32;

                for (int row = 0; row < height; row++) {
                    for (int col = 0; col < width; col++) {
                        BlockPos partPos = controllerPos.offset(
                            growthDirection.getStepX() * col,
                            row,
                            growthDirection.getStepZ() * col
                        );

                        if (level.getBlockEntity(partPos) instanceof ChalkBoardEntity partEntity) {
                            int[] partPixels = new int[pWidth * pHeight];

                            for (int py = 0; py < pHeight; py++) {
                                for (int px = 0; px < pWidth; px++) {
                                    int targetX = col * pWidth + px;
                                    int targetY = (height - 1 - row) * pHeight + py;
                                    int targetIdx = targetY * (width * pWidth) + targetX;
                                    int srcIdx = py * pWidth + px;

                                    if (targetIdx < masterPixels.length && srcIdx < partPixels.length) {
                                        partPixels[srcIdx] = masterPixels[targetIdx];
                                    }
                                }
                            }

                            partEntity.setTexturePixels(pWidth, pHeight, partPixels);
                            partEntity.setChanged(); // Forces the server to save changes to disk!
                        }
                    }
                }
            });
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}