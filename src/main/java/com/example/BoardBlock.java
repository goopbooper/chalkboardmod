package com.example;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.redstone.Orientation;
//import net.minecraft.world.phys.BlockHitResult;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

//import com.example.client.BoardClientHelper;

/** A wall-mounted board block with a state for each structural board part. */
public class BoardBlock extends BaseEntityBlock {
    private static final int MAX_BOARD_WIDTH = 8;
    private static final int MAX_BOARD_HEIGHT = 8;
    /** Horizontal direction of the board's front face. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Stores the row-major position of this block within the board. */
    public static final IntegerProperty INDEX = IntegerProperty.create(
            "index",
            0,
            MAX_BOARD_WIDTH * MAX_BOARD_HEIGHT - 1
    );
    /**
     * Creates a board block with its default facing and controller part.
     *
     * @param settings the block properties supplied by registration
     */
    public BoardBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(
    this.stateDefinition.any()
        .setValue(FACING, Direction.NORTH)
        .setValue(INDEX, 0)
);
    }


   /*  @Override
public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
    if (world.isClientSide()) {
        // Safely call client code here, or call a separate helper method 
        // located in your client source set.
        BoardClientHelper.openScreen(pos);
    }
    return InteractionResult.SUCCESS;
} */

    /** Creates render state storage for every placed board part. */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChalkBoardEntity(pos, state);
    }

    /** Uses the custom block-entity renderer instead of the empty block model. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /**
     * Finds the largest currently supported board area at the placement site.
     *
     * @param ctx the placement context supplied by the player interaction
     * @return the controller state, or {@code null} when the backing wall is invalid
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level world = ctx.getLevel();
        BlockPos startPos = ctx.getClickedPos();
        Direction wallFacing = ctx.getHorizontalDirection(); // Backing wall vector
        Direction horizontalBuildDir = wallFacing.getClockWise(); // Growth direction (rightward)

        int[] dimensions = findValidDimensions(world, startPos, horizontalBuildDir, wallFacing);

        // If even the base 1x1 controller slot fails structural checks, abort placement
        if (dimensions[0] == 0 || dimensions[1] == 0) return null;

        // Code can now trigger a loop to place helper block pieces across the calculated (validWidth * validHeight) grid!
        return this.defaultBlockState().setValue(FACING, wallFacing).setValue(INDEX, 0);
    }


        @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);

        Direction wallFacing = state.getValue(FACING);
        Direction growthDirection = wallFacing.getClockWise();
        int[] dimensions = findValidDimensions(level, pos, growthDirection, wallFacing);

        createSurroundingBlocks(
            level,
            pos,
            growthDirection,
            wallFacing,
            dimensions[0],
            dimensions[1]
        );
    }

    /** Removes all directly connected board parts before one part is broken. */
    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        if (!level.isClientSide()) {
            removeConnectedParts(level, pos, state);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    private void removeConnectedParts(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof ChalkBoardEntity boardEntity
                && boardEntity.getBoardWidth() > 0
                && boardEntity.getBoardHeight() > 0) {
            removeStoredBoardParts(level, pos, state, boardEntity);
            return;
        }

        Direction growthDirection = state.getValue(FACING).getClockWise();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> boardParts = new HashSet<>();
        pending.add(pos);

        while (!pending.isEmpty()) {
            BlockPos partPos = pending.removeFirst();
            if (!visited.add(partPos)) {
                continue;
            }

            BlockState partState = level.getBlockState(partPos);
            if (!isMatchingBoardPart(partState, state)) {
                continue;
            }

            boardParts.add(partPos);
            pending.add(partPos.relative(growthDirection));
            pending.add(partPos.relative(growthDirection.getOpposite()));
            pending.add(partPos.above());
            pending.add(partPos.below());
        }

        for (BlockPos partPos : boardParts) {
            if (!partPos.equals(pos)) {
                level.setBlock(partPos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private void removeStoredBoardParts(
            Level level,
            BlockPos brokenPos,
            BlockState state,
            ChalkBoardEntity boardEntity
    ) {
        Direction growthDirection = state.getValue(FACING).getClockWise();
        BlockPos controllerPos = boardEntity.getControllerPos();

        for (int row = 0; row < boardEntity.getBoardHeight(); row++) {
            for (int column = 0; column < boardEntity.getBoardWidth(); column++) {
                BlockPos partPos = controllerPos.offset(
                        growthDirection.getStepX() * column,
                        growthDirection.getStepY() * column + row,
                        growthDirection.getStepZ() * column
                );

                if (!partPos.equals(brokenPos)
                        && isMatchingBoardPart(level.getBlockState(partPos), state)) {
                    level.setBlock(partPos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private boolean isMatchingBoardPart(BlockState candidate, BlockState origin) {
        return candidate.is(this) && candidate.getValue(FACING) == origin.getValue(FACING);
    }

    /** Removes the complete board when its supporting wall is broken. */
    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            Orientation orientation,
            boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);

        Direction backingWall = state.getValue(FACING);
        if (!level.getBlockState(pos.relative(backingWall)).isSolidRender()) {
            removeConnectedParts(level, pos, state);
        }
    }

    /**
     * Finds the largest rectangular area with clear space and a solid backing wall.
     * The controller position is accepted even after Minecraft has placed it.
     *
     * @return an array containing width at index {@code 0} and height at index {@code 1}
     */
    private int[] findValidDimensions(
            Level level,
            BlockPos startPos,
            Direction horizontalBuildDir,
            Direction wallFacing
    ) {
        int validWidth = MAX_BOARD_WIDTH;
        int validHeight = 0;

        for (int row = 0; row < MAX_BOARD_HEIGHT; row++) {
            int rowWidth = 0;

            for (int column = 0; column < MAX_BOARD_WIDTH; column++) {
                BlockPos offsetPos = startPos.offset(
                        horizontalBuildDir.getStepX() * column,
                        horizontalBuildDir.getStepY() * column + row,
                        horizontalBuildDir.getStepZ() * column
                );
                BlockPos backgroundPos = offsetPos.offset(wallFacing.getUnitVec3i());
                boolean isController = offsetPos.equals(startPos);
                boolean isSpaceClear = isController || level.getBlockState(offsetPos).isAir();
                boolean isWallPresent = level.getBlockState(backgroundPos).isSolidRender();

                if (isSpaceClear && isWallPresent) {
                    rowWidth++;
                } else {
                    break;
                }
            }

            if (rowWidth == 0) {
                break;
            }

            validWidth = Math.min(validWidth, rowWidth);
            validHeight++;
        }

        if (validHeight == 0) {
            validWidth = 0;
        }

        return new int[] {validWidth, validHeight};
    }

    /**
     * Placeholder for creating the surrounding sections after the controller
     * block has been placed.
     *
     * @param level the level where the surrounding blocks will be created
     * @param startPos the controller block's position
     * @param horizontalBuildDir the direction in which the board grows
     * @param wallFacing the direction toward the supporting wall
     * @param width the validated board width
     * @param height the validated board height
     */
    public void createSurroundingBlocks(
            Level level,
            BlockPos startPos,
            Direction horizontalBuildDir,
            Direction wallFacing,
            int width,
            int height
    ) {


        
        BlockState surroundingState = this.defaultBlockState().setValue(FACING, wallFacing);

        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int blockIndex = row * width + column;

                // The controller is already placed at the origin.
                if (blockIndex == 0) {
                    setBoardLayout(level, startPos, startPos, width, height);
                    continue;
                }

                BlockPos partPos = startPos.offset(
                    horizontalBuildDir.getStepX() * column,
                    row, // Y always steps up by the row index vertically
                    horizontalBuildDir.getStepZ() * column
                );
                level.setBlock(partPos, surroundingState.setValue(INDEX, blockIndex), 3);
                setBoardLayout(level, partPos, startPos, width, height);
            }
        }
    }

    private void setBoardLayout(
            Level level,
            BlockPos partPos,
            BlockPos controllerPos,
            int width,
            int height
    ) {
        if (level.getBlockEntity(partPos) instanceof ChalkBoardEntity boardEntity) {
            boardEntity.setBoardLayout(controllerPos, width, height);
        }
    }

    /** Removes the board when its supporting wall block is no longer solid. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        Direction backingWall = state.getValue(FACING);
        if (direction == backingWall && !neighborState.isSolidRender()) {
            return Blocks.AIR.defaultBlockState(); // Auto-destruct if the supporting wall collapses
        }
        return super.updateShape(state, level, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    /** Adds facing and structural-part properties to the block state definition. */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, INDEX);
    }
}
