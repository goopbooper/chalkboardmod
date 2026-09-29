package com.example;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import java.util.function.Function;

/** Registration utilities for chalkboard items. */
public class ChalkItems {
    
    //public static final Item CHALK_BOARD = register(ChalkItemIds.CHALK_BOARD, Item::new, new Item.Properties());
    
    /**
     * Creates and registers an item using the supplied factory and settings.
     *
     * @param itemKey the registry key for the item
     * @param itemFactory factory that creates the item
     * @param settings base item properties
     * @return the registered item
     */
	public static Item register(ResourceKey<Item> itemKey, Function<Item.Properties, Item> itemFactory, Item.Properties settings) {
		// Create the item instance.
		Item item = itemFactory.apply(settings.setId(itemKey));

		// Register the item.
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);

		return item;
	}
    
	/** Reserved initialization hook for item setup. */
    public static void initialize(){
    }
    

}
