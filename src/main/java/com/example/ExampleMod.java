package com.example;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main server/common entrypoint for the chalkboard mod. */
public class ExampleMod implements ModInitializer {
	/** The namespace used for this mod's registered resources. */
	public static final String MOD_ID = "chalkboard";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Registers the mod's common items, blocks, and entity types. */
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ChalkItems.initialize();
		ChalkBlocks.initialize();
		ModEntityTypes.initialize();
		LOGGER.info("Hello Fabric world!");
	}

	/**
	 * Creates an identifier in this mod's namespace.
	 *
	 * @param path the path portion of the identifier
	 * @return an identifier owned by this mod
	 */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
	
}
