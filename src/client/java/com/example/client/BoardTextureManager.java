package com.example.client;

import com.example.ChalkBoardEntity;
import com.example.ExampleMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class BoardTextureManager {
    private static final String NAMESPACE = "chalkboard";
    private static final Map<BlockPos, TextureCacheEntry> CACHE = new HashMap<>();

    private BoardTextureManager() {}

    public static Identifier getTexture(ChalkBoardEntity board) {
        BlockPos pos = board.getBlockPos();
        int currentVersion = board.getTextureVersion();
        
        ExampleMod.LOGGER.info("[DEBUG] BoardTextureManager.getTexture called for pos: {}, version: {}", pos, currentVersion);
        
        TextureCacheEntry entry = CACHE.get(pos);

        // If it already exists and version hasn't changed, reuse the identifier immediately
        if (entry != null && entry.version == currentVersion) {
            ExampleMod.LOGGER.info("[DEBUG] Cache hit for pos: {} (version matches: {})", pos, currentVersion);
            return entry.identifier;
        }

        ExampleMod.LOGGER.info("[DEBUG] Cache miss or version update for pos: {} (cached version: {}, current version: {})", 
            pos, (entry != null ? entry.version : "null"), currentVersion);

        Identifier id = entry != null ? entry.identifier : createIdentifier(pos);
        DynamicTexture texture = entry != null ? entry.texture : null;

        NativeImage image = createImage(board);

        if (texture == null) {
            ExampleMod.LOGGER.info("[DEBUG] Creating brand new DynamicTexture for id: {}", id);
            // Create a new DynamicTexture and register it with Minecraft's TextureManager once
            texture = new DynamicTexture(() -> "Chalkboard texture " + pos, image);
            Minecraft.getInstance().getTextureManager().register(id, texture);
        } else {
            // If the texture already exists, safely update its backing pixels without re-registering
            NativeImage existingImage = texture.getPixels();
            if (existingImage != null && existingImage.getWidth() == image.getWidth() && existingImage.getHeight() == image.getHeight()) {
                ExampleMod.LOGGER.info("[DEBUG] Updating existing texture pixels in-place for pos: {}", pos);
                // Copy new pixels over the existing native buffer to prevent memory leaks
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        existingImage.setPixel(x, y, image.getPixel(x, y));
                    }
                }
                image.close(); // Close our temporary allocation
            } else {
                ExampleMod.LOGGER.info("[DEBUG] Dimensions changed; recreating DynamicTexture for pos: {}", pos);
                // Dimensions changed; swap out the texture safely
                texture.close();
                texture = new DynamicTexture(() -> "Chalkboard texture " + pos, image);
                Minecraft.getInstance().getTextureManager().register(id, texture);
            }
        }

        // Push changes to the GPU texture buffer
        texture.upload();
        ExampleMod.LOGGER.info("[DEBUG] Texture uploaded to GPU successfully for id: {}", id);

        CACHE.put(pos, new TextureCacheEntry(id, texture, currentVersion));
        return id;
    }

    private static NativeImage createImage(ChalkBoardEntity board) {
        int width = board.getTextureWidth();
        int height = board.getTextureHeight();
        int[] pixels = board.getTexturePixels();
        
        ExampleMod.LOGGER.info("[DEBUG] createImage dimensions: {}x{}, pixel array length: {}", width, height, (pixels != null ? pixels.length : 0));
        
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixel(x, y, pixels[y * width + x]);
            }
        }
        return image;
    }

    private static Identifier createIdentifier(BlockPos position) {
        Identifier id = Identifier.fromNamespaceAndPath(
                NAMESPACE,
                "dynamic/board_" + Long.toUnsignedString(position.asLong())
        );
        ExampleMod.LOGGER.info("[DEBUG] Created new dynamic Identifier: {}", id);
        return id;
    }

    private record TextureCacheEntry(Identifier identifier, DynamicTexture texture, int version) {}
}