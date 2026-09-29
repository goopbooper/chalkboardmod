package com.example;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;

/** Registers entity types and their common attributes for the mod. */
public class ModEntityTypes {
	/** Registered entity type used by the chalkboard entity. */
	public static final BlockEntityType<ChalkBoardEntity> CHALK_BOARD_ENTITY =
			register("chalk_board", ChalkBoardEntity::new, ChalkBlocks.CHALK_BOARD);

	private static <T extends BlockEntity> BlockEntityType<T> register(
			String name,
			FabricBlockEntityTypeBuilder.Factory<? extends T> entityFactory,
			Block... blocks
	) {
		Identifier id = ExampleMod.id(name);
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, FabricBlockEntityTypeBuilder.<T>create(entityFactory, blocks).build());
	}

	/** Forces entity-type class initialization and confirms registration. */
	public static void initialize() {
		ExampleMod.LOGGER.info("Registering EntityTypes for " + ExampleMod.MOD_ID);
	}
}
