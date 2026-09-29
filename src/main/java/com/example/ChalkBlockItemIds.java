package com.example;

import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;

/** Registry keys shared by the chalkboard block and item. */
public class ChalkBlockItemIds {
	/** Registry key pair for the chalkboard block and item. */
    public static final BlockItemId CHALK_BOARD = create("chalk_board");

	/** Creates a block-item key pair in the mod namespace. */
	private static BlockItemId create(String name) {
		Identifier id = ExampleMod.id(name);
		return BlockItemId.create(id, id);
	}
    
}
