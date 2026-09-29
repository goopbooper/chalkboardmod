package com.example;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;

public class BoardRenderState extends BlockEntityRenderState {
    public Identifier activeTexture;
    public Direction facing;
    public int partIndex;
    public int boardWidth = 8;  // Dynamic width default
    public int boardHeight = 8; // Dynamic height default
}