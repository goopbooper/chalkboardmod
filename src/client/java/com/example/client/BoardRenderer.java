package com.example.client;

import com.example.BoardBlock;
import com.example.ChalkBoardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class BoardRenderer implements BlockEntityRenderer<ChalkBoardEntity, BoardRenderState> {

    private static final Identifier FRAME_TEXTURE =
            Identifier.fromNamespaceAndPath("chalkboard", "textures/block/chalkboard_frame.png");
    private static final Identifier FALLBACK_TEXTURE =
            Identifier.fromNamespaceAndPath("chalkboard", "textures/block/chalk_board.png");

    private static final float FACE_Z = 0.0625f;
    private static final float FRAME_WIDTH = 0.125f;
    private static final float FRAME_DEPTH = 0.2f;

    public BoardRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public BoardRenderState createRenderState() {
        return new BoardRenderState();
    }

    @Override
    public void extractRenderState(ChalkBoardEntity entity, BoardRenderState state, float partialTick,
            Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPosition, crumblingOverlay);

        Identifier tex = BoardTextureManager.getTexture(entity);
        state.activeTexture = tex != null ? tex : FALLBACK_TEXTURE;
        state.facing = entity.getBlockState().getValue(BoardBlock.FACING);
        state.partIndex = entity.getBlockState().getValue(BoardBlock.INDEX);
        state.boardWidth = Math.max(1, entity.getBoardWidth());
        state.boardHeight = Math.max(1, entity.getBoardHeight());
    }

    @Override
    public void submit(BoardRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose();

        poses.translate(0.5f, 0.5f, 0.5f);
        float yRot = switch (state.facing) {
            case SOUTH -> 180f;
            case WEST -> 90f;
            case EAST -> -90f;
            default -> 0f; // NORTH
        };
        poses.rotateDegrees(Axis.YP, yRot);
        poses.translate(-0.5f, -0.5f, -0.4375f);

        collector.submitCustomGeometry(poses, RenderTypes.entityTranslucent(state.activeTexture),
                (pose, consumer) -> renderFace(state, pose, consumer));

        collector.submitCustomGeometry(poses, RenderTypes.entityTranslucent(FRAME_TEXTURE),
                (pose, consumer) -> renderFrame(state, pose, consumer));

        poses.popPose();
    }

    // ---- Chalkboard face ----------------------------------------------------

    // ---- Chalkboard face ----------------------------------------------------

    private void renderFace(BoardRenderState state, PoseStack.Pose pose, VertexConsumer c) {
    // Each block entity manages its own independent 32x32 texture slice, 
    // so we map the full 0.0 to 1.0 UV range for every individual block part.
        float u0 = 0f, u1 = 1f;
        float v0 = 0f, v1 = 1f;

        Matrix4f m = pose.pose();
        int light = state.lightCoords;

        // Front (+Z)
        vertex(c, m, 0, 0, FACE_Z, u0, v1, 0, 0, 1, 255, 255, 255, light);
        vertex(c, m, 1, 0, FACE_Z, u1, v1, 0, 0, 1, 255, 255, 255, light);
        vertex(c, m, 1, 1, FACE_Z, u1, v0, 0, 0, 1, 255, 255, 255, light);
        vertex(c, m, 0, 1, FACE_Z, u0, v0, 0, 0, 1, 255, 255, 255, light);

        // Back (-Z)
        vertex(c, m, 1, 0, FACE_Z - 0.001f, u0, v1, 0, 0, -1, 255, 255, 255, light);
        vertex(c, m, 0, 0, FACE_Z - 0.001f, u1, v1, 0, 0, -1, 255, 255, 255, light);
        vertex(c, m, 0, 1, FACE_Z - 0.001f, u1, v0, 0, 0, -1, 255, 255, 255, light);
        vertex(c, m, 1, 1, FACE_Z - 0.001f, u0, v0, 0, 0, -1, 255, 255, 255, light);
    }

  // ---- Frame ---------------------------------------------------------------

private void renderFrame(BoardRenderState state, PoseStack.Pose pose, VertexConsumer c) {
    int col = state.partIndex % state.boardWidth;
    int row = state.partIndex / state.boardWidth;

    Matrix4f m = pose.pose();
    int light = state.lightCoords;

    // row = (boardHeight - 1) is the top block in the world
    if (row == state.boardHeight - 1) {
        box(c, m, light, 0, 1 - FRAME_WIDTH, 1, 1);
    }
    // row = 0 is the bottom block in the world
    if (row == 0) {
        box(c, m, light, 0, 0, 1, FRAME_WIDTH);
    }
    // Left edge block
    if (col == 0) {
        box(c, m, light, 0, 0, FRAME_WIDTH, 1);
    }
    // Right edge block
    if (col == state.boardWidth - 1) {
        box(c, m, light, 1 - FRAME_WIDTH, 0, 1, 1);
    }
}

    private void box(VertexConsumer c, Matrix4f m, int light, float x0, float y0, float x1, float y1) {
        float z0 = -0.045f;
        float z1 = 0.045f + FRAME_DEPTH;

        quad(c, m, light, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1,  0, 0, 1); // front
        quad(c, m, light, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0,  0, 0,-1); // back
        quad(c, m, light, x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1,  0, 1, 0); // top
        quad(c, m, light, x0,y0,z1, x1,y0,z1, x1,y0,z0, x0,y0,z0,  0,-1, 0); // bottom
        quad(c, m, light, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, -1, 0, 0); // left
        quad(c, m, light, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1,  1, 0, 0); // right
    }

    // ---- Helpers ---------------------------------------------------------------

    private void quad(VertexConsumer c, Matrix4f m, int light,
                      float ax, float ay, float az, float bx, float by, float bz,
                      float cx, float cy, float cz, float dx, float dy, float dz,
                      float nx, float ny, float nz) {
        vertex(c, m, ax, ay, az, 0, 1, nx, ny, nz, 255, 255, 255, light);
        vertex(c, m, bx, by, bz, 1, 1, nx, ny, nz, 255, 255, 255, light);
        vertex(c, m, cx, cy, cz, 1, 0, nx, ny, nz, 255, 255, 255, light);
        vertex(c, m, dx, dy, dz, 0, 0, nx, ny, nz, 255, 255, 255, light);
    }

    private void vertex(VertexConsumer c, Matrix4f m, float x, float y, float z, float u, float v,
                        float nx, float ny, float nz, int r, int g, int b, int light) {
        c.addVertex(m, x, y, z)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}