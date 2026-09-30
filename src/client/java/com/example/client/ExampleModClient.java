package com.example.client;


import com.example.BoardBlock;
import com.example.ChalkBoardEntity;
import com.example.ModEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.chat.Component;



/** Client entrypoint for renderer and other client-only registration. */
public class ExampleModClient implements ClientModInitializer {
	/** Runs client-only initialization after the client is ready to load the mod. */
	@Override
	public void onInitializeClient() {
		BlockEntityRenderers.register(ModEntityTypes.CHALK_BOARD_ENTITY, BoardRenderer::new);
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClientSide()) {
				BlockPos pos = hitResult.getBlockPos();
				
				if (world.getBlockState(pos).getBlock() instanceof BoardBlock) {
					BlockEntity blockEntity = world.getBlockEntity(pos);
					
					if (blockEntity instanceof ChalkBoardEntity boardEntity) {
						BlockPos controllerPos = boardEntity.getControllerPos();
						int width = boardEntity.getBoardWidth();
						int height = boardEntity.getBoardHeight();
						
						net.minecraft.core.Direction growthDirection = boardEntity.getBlockState().getValue(BoardBlock.FACING).getClockWise();

						ChalkBoardEntity[] boardEntitys = new ChalkBoardEntity[width * height];
						int index = 0;

						for (int row = 0; row < height; row++) {
							for (int column = 0; column < width; column++) {
								BlockPos partPos = controllerPos.offset(
									growthDirection.getStepX() * column,
									growthDirection.getStepY() * column + row, // + row is correct for bottom-left extending up
									growthDirection.getStepZ() * column
								);

								BlockEntity be = world.getBlockEntity(partPos);
								if (be instanceof ChalkBoardEntity partEntity) {
									boardEntitys[index] = partEntity;
								}
								index++;
							}
						}

						Minecraft.getInstance().gui.setScreen(new BoardScreen(Component.empty(), boardEntitys));
						return InteractionResult.SUCCESS;
					}
				}
			}
			return InteractionResult.PASS;
		});
	}
}