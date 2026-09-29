package com.example.client;

import com.example.BoardRenderer;
import com.example.ModEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/** Client entrypoint for renderer and other client-only registration. */
public class ExampleModClient implements ClientModInitializer {
	/** Runs client-only initialization after the client is ready to load the mod. */
	@Override
	public void onInitializeClient() {
		BlockEntityRenderers.register(ModEntityTypes.CHALK_BOARD_ENTITY, BoardRenderer::new);
	}
}