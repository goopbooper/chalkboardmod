package com.example;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class BoardRenderer implements BlockEntityRenderer<ChalkBoardEntity, BoardRenderState> {
    
    private static final Identifier FRAME_TEXTURE = Identifier.fromNamespaceAndPath("chalkboard", "textures/block/chalkboard_frame.png");

    public BoardRenderer(BlockEntityRendererProvider.Context context) {
        ExampleMod.LOGGER.info("[DEBUG] BoardRenderer initialized!");
    }

    @Override
    public BoardRenderState createRenderState() {
        return new BoardRenderState();
    }

    @Override
    public void extractRenderState(ChalkBoardEntity entity, BoardRenderState state, float partialTick,
            Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPosition, crumblingOverlay);

        Identifier tex = BoardTextureManager.getTexture(entity);
        state.activeTexture = (tex != null) ? tex : Identifier.fromNamespaceAndPath("chalkboard", "textures/block/chalk_board.png");
        
        state.facing = entity.getBlockState().getValue(BoardBlock.FACING);
        state.partIndex = entity.getBlockState().getValue(BoardBlock.INDEX);
        
        // Pull actual dynamic size from the block entity or its controller structure
        // (Adjust these method names to match whatever methods your ChalkBoardEntity provides)
        state.boardWidth = entity.getBoardWidth();   
        state.boardHeight = entity.getBoardHeight(); 
        
        ExampleMod.LOGGER.info("[DEBUG] extractRenderState pos: {}, index: {}, size: {}x{}", 
            entity.getBlockPos(), state.partIndex, state.boardWidth, state.boardHeight);
    }

    @Override
    public void submit(BoardRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        ExampleMod.LOGGER.info("[DEBUG] submit called for texture: {}, index: {}", state.activeTexture, state.partIndex);
        
        if (state.activeTexture == null) {
            ExampleMod.LOGGER.error("[DEBUG] ABORTING SUBMIT: activeTexture is null!");
            return;
        }

        poses.pushPose();

        poses.translate(0.5f, 0.5f, 0.5f);

        float yRot = switch (state.facing) {
            case NORTH -> 0f;
            case SOUTH -> 180f;
            case WEST -> 90f;
            case EAST -> -90f;
            default -> 0f;
        };
        poses.rotateDegrees(Axis.YP, yRot);
        poses.translate(-0.5f, -0.5f, -0.4375f);

        // 1. Submit chalkboard face geometry (using dynamic width/height for UV mapping if needed)
        collector.submitCustomGeometry(poses, RenderTypes.entityTranslucent(state.activeTexture), (pose, consumer) -> {
            renderBoard(state, pose, consumer);
        });

        // 2. Submit 3D frame prisms based on actual dynamic dimensions
        collector.submitCustomGeometry(poses, RenderTypes.entityTranslucent(FRAME_TEXTURE), (pose, consumer) -> {
            renderOuterBorderPrisms(state, pose, consumer);
        });

        poses.popPose();
    }

    private void renderBoard(BoardRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        ExampleMod.LOGGER.info("[DEBUG] renderBoard executing geometry submission for index: {}", state.partIndex);
        
        int width = Math.max(1, state.boardWidth);
        int height = Math.max(1, state.boardHeight);

        int index = state.partIndex;
        int col = index % width;
        int row = index / width;

        float uStep = 1.0f / width;
        float vStep = 1.0f / height;

        float minU = col * uStep;
        float maxU = (col + 1) * uStep;
        float minV = row * vStep;
        float maxV = (row + 1) * vStep;

        Matrix4f matrix = pose.pose();

        // Front Face
        addVertex(consumer, matrix, 0.0f, 0.0f, maxU, maxV, 0.0f, 0.0f, 1.0f, 0.0625f);
        addVertex(consumer, matrix, 1.0f, 0.0f, minU, maxV, 0.0f, 0.0f, 1.0f, 0.0625f);
        addVertex(consumer, matrix, 1.0f, 1.0f, minU, minV, 0.0f, 0.0f, 1.0f, 0.0625f);
        addVertex(consumer, matrix, 0.0f, 1.0f, maxU, minV, 0.0f, 0.0f, 1.0f, 0.0625f);

        // Back Face
        addVertex(consumer, matrix, 0.0f, 1.0f, maxU, minV, 0.0f, 0.0f, -1.0f, 0.0f);
        addVertex(consumer, matrix, 1.0f, 1.0f, minU, minV, 0.0f, 0.0f, -1.0f, 0.0f);
        addVertex(consumer, matrix, 1.0f, 0.0f, minU, maxV, 0.0f, 0.0f, -1.0f, 0.0f);
        addVertex(consumer, matrix, 0.0f, 0.0f, maxU, maxV, 0.0f, 0.0f, -1.0f, 0.0f);
    }

    private void renderOuterBorderPrisms(BoardRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        ExampleMod.LOGGER.info("[DEBUG] renderOuterBorderPrisms executing for index: {}", state.partIndex);
        
        int width = Math.max(1, state.boardWidth);
        int height = Math.max(1, state.boardHeight);

        int index = state.partIndex;
        int col = index % width;
        int row = index / width;

        boolean isBottom = (row == 0);
        boolean isTop = (row == height - 1);
        boolean isLeft = (col == 0);
        boolean isRight = (col == width - 1);

        Matrix4f matrix = pose.pose();
        float frameDepth = 0.2f;   
        float frameWidth = 0.125f; 

        if (isTop) {
            renderBox(consumer, matrix, 0.0f, 1.0f - frameWidth, 1.0f, 1.0f, frameDepth);
        }
        if (isBottom) {
            renderBox(consumer, matrix, 0.0f, 0.0f, 1.0f, frameWidth, frameDepth);
        }
        if (isLeft) {
            renderBox(consumer, matrix, 0.0f, 0.0f, frameWidth, 1.0f, frameDepth);
        }
        if (isRight) {
            renderBox(consumer, matrix, 1.0f - frameWidth, 0.0f, 1.0f, 1.0f, frameDepth);
        }
    }

    private void renderBox(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float x1, float y1, float zDepth) {
        float z0 = -0.045f;
        float z1 = 0.045f + zDepth;

        // Front Face
        addQuad(consumer, matrix, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1);
        // Back Face
        addQuad(consumer, matrix, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1);
        // Top Face
        addQuad(consumer, matrix, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0);
        // Bottom Face
        addQuad(consumer, matrix, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0);
        // Left Face
        addQuad(consumer, matrix, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0);
        // Right Face
        addQuad(consumer, matrix, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0);
    }

    private void addQuad(VertexConsumer consumer, Matrix4f matrix,
                         float ax, float ay, float az,
                         float bx, float by, float bz,
                         float cx, float cy, float cz,
                         float dx, float dy, float dz,
                         float nx, float ny, float nz) {
        addVertexPoint(consumer, matrix, ax, ay, az, 0, 1, nx, ny, nz);
        addVertexPoint(consumer, matrix, bx, by, bz, 1, 1, nx, ny, nz);
        addVertexPoint(consumer, matrix, cx, cy, cz, 1, 0, nx, ny, nz);
        addVertexPoint(consumer, matrix, dx, dy, dz, 0, 0, nx, ny, nz);
    }

    private void addVertexPoint(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(100, 60, 30, 255)
                .setUv(u, v)
                .setUv2(15, 15)
                .setOverlay(0)
                .setNormal(nx, ny, nz);
    }

    private void addVertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x,
            float y,
            float u,
            float v,
            float nx,
            float ny,
            float nz,
            float zLevel
    ) {
        consumer.addVertex(matrix, x, y, zLevel)
                .setColor(0, 255, 0, 255)
                .setUv(u, v)
                .setUv2(15, 15)
                .setOverlay(0)
                .setNormal(nx, ny, nz);
    }
}