package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;



/** Stores the board's ARGB pixels and synchronizes them to clients. */
public class ChalkBoardEntity extends BlockEntity {
    private static final int DEFAULT_WIDTH = 32;
    private static final int DEFAULT_HEIGHT = 32;
    private static final int DEFAULT_COLOR = 0xFF3B4B2E;

    /** Width of the in-memory pixel image. */
    private int textureWidth = DEFAULT_WIDTH;
    /** Height of the in-memory pixel image. */
    private int textureHeight = DEFAULT_HEIGHT;
    /** Pixels stored row-major as packed ARGB values. */
    private int[] texturePixels = createDefaultPixels();
    /** Changes whenever the client texture needs to be uploaded again. */
    private int textureVersion;
    /** Position of the controller block for this board. */
    private BlockPos controllerPos;
    /** Number of columns in the board. */
    private int boardWidth = 1;
    /** Number of rows in the board. */
    private int boardHeight = 1;

    /** Creates a board entity at a block position. */
    public ChalkBoardEntity(BlockPos pos, BlockState state) {
        super(ModEntityTypes.CHALK_BOARD_ENTITY, pos, state);
        this.controllerPos = pos;
        this.texturePixels = createStartingPixels(
            state.getValue(BoardBlock.INDEX),
            this.textureWidth,
            this.textureHeight
        );
    }

    /** Stores the shared layout metadata used by every board part. */
    public void setBoardLayout(BlockPos controllerPos, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Board dimensions must be positive");
        }

        this.controllerPos = controllerPos;
        this.boardWidth = width;
        this.boardHeight = height;
        this.setChanged();
    }

    /** @return the controller position shared by this board's parts */
    public BlockPos getControllerPos() {
        return this.controllerPos;
    }

    /** @return the board width in blocks */
    public int getBoardWidth() {
        return this.boardWidth;
    }

    /** @return the board height in blocks */
    public int getBoardHeight() {
        return this.boardHeight;
    }

    /** @return the pixel image width */
    public int getTextureWidth() {
        return this.textureWidth;
    }

    /** @return the pixel image height */
    public int getTextureHeight() {
        return this.textureHeight;
    }

    /** @return a defensive copy of the packed ARGB pixels */
    public int[] getTexturePixels() {
        return this.texturePixels.clone();
    }

    /** @return a value that changes whenever the pixel data changes */
    public int getTextureVersion() {
        return this.textureVersion;
    }

    /**
     * Replaces the board image and marks the block entity for synchronization.
     *
     * @param width image width in pixels
     * @param height image height in pixels
     * @param pixels row-major packed ARGB pixels
     */
    public void setTexturePixels(int width, int height, int[] pixels) {
        if (width <= 0 || height <= 0 || pixels.length != width * height) {
            throw new IllegalArgumentException("Pixel data dimensions do not match the pixel array");
        }

        this.textureWidth = width;
        this.textureHeight = height;
        this.texturePixels = pixels.clone();
        this.textureVersion++;
        this.setChanged();
    }

    /** Saves this entity's data for clients loading the containing chunk. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    /** Creates the packet used to synchronize changes to watching clients. */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Marks the entity dirty and broadcasts its changed data to clients. */
    @Override
    public void setChanged() {
        super.setChanged();

        if (this.level == null || this.level.isClientSide()) {
            return;
        }

        BlockState state = this.getBlockState();
        this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
    }
    
@Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("TextureWidth", this.textureWidth);
        output.putInt("TextureHeight", this.textureHeight);
        output.putInt("BoardWidth", this.boardWidth);
        output.putInt("BoardHeight", this.boardHeight);
        output.putLong("ControllerPos", this.controllerPos.asLong());
        
        // Directly save the int array using ValueOutput's built-in method
        output.putIntArray("TexturePixels", this.texturePixels);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        this.textureWidth = input.getIntOr("TextureWidth", DEFAULT_WIDTH);
        this.textureHeight = input.getIntOr("TextureHeight", DEFAULT_HEIGHT);
        this.boardWidth = input.getIntOr("BoardWidth", 1);
        this.boardHeight = input.getIntOr("BoardHeight", 1);
        
        // Use getIntOr with a default fallback block position coordinate (or the current position fallback)
        long defaultPos = this.getBlockPos() != null ? this.getBlockPos().asLong() : 0L;
        this.controllerPos = BlockPos.of(input.getLongOr("ControllerPos", defaultPos));

        // Safely fetch the int array using ValueInput's optional getter
        input.getIntArray("TexturePixels").ifPresent(pixels -> {
            if (this.textureWidth > 0 && this.textureHeight > 0 && pixels.length == this.textureWidth * this.textureHeight) {
                this.texturePixels = pixels;
                this.textureVersion++;
            }
        });
    }

    private static int[] createDefaultPixels() {
        return createStartingPixels(0, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Creates the initial flat pixel buffer for one board part.
     *
     * @param index the part's row-major board index
     * @param width the image width in pixels
     * @param height the image height in pixels
     * @return a newly allocated row-major ARGB pixel array
     */
    private static int[] createStartingPixels(int index, int width, int height) {
        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = DEFAULT_COLOR;
            }
        }

        return pixels;
    }
}
