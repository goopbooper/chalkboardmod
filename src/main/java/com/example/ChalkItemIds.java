package com.example;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Registry keys for standalone chalk items. */
public class ChalkItemIds {
    /** Registry key for the chalkboard item. */
    public static final ResourceKey<Item> CHALK_BOARD = create("chalk_board");

	/** Creates an item key in the mod namespace. */
	public static ResourceKey<Item> create(String name) {
		// Create the item key.
		return ResourceKey.create(Registries.ITEM, ExampleMod.id(name));
	}
	
}
