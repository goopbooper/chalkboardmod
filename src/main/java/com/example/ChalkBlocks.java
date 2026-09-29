package com.example;


import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;

/** Registers the chalkboard block and its corresponding inventory item. */
public class ChalkBlocks {

	


	/** The registered chalkboard block. */
	public static final Block CHALK_BOARD = register(
		ChalkBlockItemIds.CHALK_BOARD,
		BoardBlock::new,
		BlockBehaviour.Properties.of()
        .instabreak()        // Replaces your manual tripwire copy; breaks instantly with one punch
        .noCollision()       // Entities walk completely through it like a painting
        .noOcclusion()
		.sound(SoundType.STONE)
);

	/** Registers a block and creates the item used to place it. */
	private static Block register(BlockItemId id, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties properties) {
		// Create the block instance
		Block block = register(id.block(), blockFactory, properties);

		// Create the block item instance
		BlockItem blockItem = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(id.item()));
		Registry.register(BuiltInRegistries.ITEM, id.item(), blockItem);

		return block;
	}

	/** Creates and registers the block portion of a block-item registration. */
	private static Block register(ResourceKey<Block> id, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties properties) {
		// Create the block instance
		Block block = blockFactory.apply(properties.setId(id));

		return Registry.register(BuiltInRegistries.BLOCK, id, block);
	}

	/** Adds the chalkboard item to the building blocks creative tab. */
	public static void initialize() {
		// Get the event for modifying entries in the ingredients group.
        // And register an event handler that adds our suspicious item to the ingredients group.

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register((creativeTab) -> {
		creativeTab.accept(ChalkBlocks.CHALK_BOARD.asItem());
});
	}
}

