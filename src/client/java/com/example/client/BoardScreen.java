package com.example.client;

import com.example.ChalkBoardEntity;
import com.example.network.BoardUpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;

public class BoardScreen extends Screen {
    private boolean ERASER = false;
    private int currentBrushSize = 5; // Default brush size
    private EditBox inputBox;
    private final ChalkBoardEntity[] board;
    private final int[] pixels;
    private final int totalWidth;
    private final int totalHeight;
    
    // Track previous mouse position for smooth line drawing during drags
    private int lastPixelX = -1;
    private int lastPixelY = -1;

    public BoardScreen(Component title, ChalkBoardEntity[] board) {
        super(title);
        this.board = board;

        if (board == null || board.length == 0 || board[0] == null) {
            this.pixels = new int[0];
            this.totalWidth = 1;
            this.totalHeight = 1;
            return;
        }

        // Get board layout and individual part texture dimensions
        int bWidth = board[0].getBoardWidth();
        int bHeight = board[0].getBoardHeight();
        int pWidth = board[0].getTextureWidth();   
        int pHeight = board[0].getTextureHeight(); 

        this.totalWidth = bWidth * pWidth;
        this.totalHeight = bHeight * pHeight;
        int[] combinedPixels = new int[this.totalWidth * this.totalHeight];

        // Stitch individual block entity textures into the master canvas on open
        for (int row = 0; row < bHeight; row++) {
            for (int col = 0; col < bWidth; col++) {
                int entityIndex = row * bWidth + col;
                if (entityIndex >= board.length || board[entityIndex] == null) continue;

                int[] partPixels = board[entityIndex].getTexturePixels();

                for (int py = 0; py < pHeight; py++) {
                    for (int px = 0; px < pWidth; px++) {
                        int srcIdx = py * pWidth + px;
                        int targetX = col * pWidth + px;
                        int targetY = (bHeight - 1 - row) * pHeight + py;
                        int targetIdx = targetY * this.totalWidth + targetX;

                        if (srcIdx < partPixels.length && targetIdx < combinedPixels.length) {
                            combinedPixels[targetIdx] = partPixels[srcIdx];
                        }
                    }
                }
            }
        }
        this.pixels = combinedPixels;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Dynamically calculates pixel size so the board scales to fit the screen perfectly. */
    private int getDynamicPixelSize() {
        int sidebarWidth = 170;
        int availableWidth = Math.max(100, this.width - sidebarWidth - 40);
        int availableHeight = Math.max(100, this.height - 50);

        int scaleX = availableWidth / Math.max(1, this.totalWidth);
        int scaleY = availableHeight / Math.max(1, this.totalHeight);
        
        // Pick the largest integer scale that fits on screen, with a minimum of 1
        return Math.max(1, Math.min(scaleX, scaleY));
    }

    private int getBoardStartX() {
        int pixelSize = getDynamicPixelSize();
        int boardRenderWidth = this.totalWidth * pixelSize;
        int sidebarWidth = 160;
        int remainingSpace = Math.max(0, this.width - sidebarWidth);
        return sidebarWidth + (remainingSpace - boardRenderWidth) / 2;
    }

    private int getBoardStartY() {
        int pixelSize = getDynamicPixelSize();
        int boardRenderHeight = this.totalHeight * pixelSize;
        return Math.max(25, (this.height - boardRenderHeight) / 2);
    }

    private int[] screenToLocalPixels(double mouseX, double mouseY) {
        int startX = getBoardStartX();
        int startY = getBoardStartY();
        int pixelSize = getDynamicPixelSize();

        if (mouseX < startX || mouseX >= startX + (this.totalWidth * pixelSize) ||
             mouseY < startY || mouseY >= startY + (this.totalHeight * pixelSize)) {
            return null;
        }

        int localPixelX = (int) ((mouseX - startX) / pixelSize);
        int localPixelY = (int) ((mouseY - startY) / pixelSize);
        return new int[] { localPixelX, localPixelY };
    }

    private boolean paintBrushAt(int localPixelX, int localPixelY) {
        int paintColor = this.ERASER ? 0xFF3B4B2E : 0xFFFFFFFF; // Eraser chalkboard green / white
        int halfBrush = this.currentBrushSize / 2;
        boolean modified = false;

        for (int dy = -halfBrush; dy <= halfBrush; dy++) {
            for (int dx = -halfBrush; dx <= halfBrush; dx++) {
                int px = localPixelX + dx;
                int py = localPixelY + dy;

                if (px < 0 || px >= this.totalWidth || py < 0 || py >= this.totalHeight) {
                    continue;
                }

                int masterIdx = py * this.totalWidth + px;
                if (masterIdx >= 0 && masterIdx < this.pixels.length) {
                    if (this.pixels[masterIdx] != paintColor) {
                        this.pixels[masterIdx] = paintColor;
                        modified = true;
                    }
                }
            }
        }
        return modified;
    }

    private void paintLine(int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            paintBrushAt(x0, y0);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    @Override
    protected void init() {
        int sidebarX = 15;
        int yOffset = 40;

       
        // 2. Eraser Toggle Button
        Button eraserButton = Button.builder(Component.literal("Eraser: Off"), (btn) -> {
            this.ERASER = !this.ERASER;
            btn.setMessage(Component.literal("Eraser: " + (this.ERASER ? "ON" : "Off")));
        }).bounds(sidebarX, yOffset, 135, 20).build();
        this.addRenderableWidget(eraserButton);

        yOffset += 35;

        // 3. Brush Size Buttons (1, 3, 5, 10)
        int[] brushSizes = {1, 3, 5, 10};
        for (int size : brushSizes) {
            Button sizeButton = Button.builder(Component.literal("Brush: " + size + "px"), (btn) -> {
                this.currentBrushSize = size;
            }).bounds(sidebarX, yOffset, 135, 20).build();
            this.addRenderableWidget(sizeButton);
            yOffset += 24;
        }
    }

    @Override
    public void onClose() {
        if (this.board != null && this.board.length > 0 && this.pixels != null) {
            ChalkBoardEntity controller = this.board[0];
            BlockPos controllerPos = controller.getControllerPos();
            int bWidth = controller.getBoardWidth();
            int bHeight = controller.getBoardHeight();

            // Send packet to server for persistent world saves
            ClientPlayNetworking.send(new BoardUpdatePayload(controllerPos, bWidth, bHeight, this.pixels));

            int pWidth = controller.getTextureWidth();
            int pHeight = controller.getTextureHeight();

            for (int row = 0; row < bHeight; row++) {
                for (int col = 0; col < bWidth; col++) {
                    int entityIndex = row * bWidth + col;
                    if (entityIndex >= board.length || board[entityIndex] == null) continue;

                    ChalkBoardEntity targetEntity = board[entityIndex];
                    int[] partPixels = new int[pWidth * pHeight];

                    for (int py = 0; py < pHeight; py++) {
                        for (int px = 0; px < pWidth; px++) {
                            int targetX = col * pWidth + px;
                            int targetY = (bHeight - 1 - row) * pHeight + py;
                            int targetIdx = targetY * this.totalWidth + targetX;
                            int srcIdx = py * pWidth + px;

                            if (targetIdx < this.pixels.length && srcIdx < partPixels.length) {
                                partPixels[srcIdx] = this.pixels[targetIdx];
                            }
                        }
                    }

                    targetEntity.setTexturePixels(pWidth, pHeight, partPixels);
                }
            }
        }

        super.onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() == 0 || event.button() == 1) {
            int[] coords = screenToLocalPixels(event.x(), event.y());
            if (coords != null) {
                this.lastPixelX = coords[0];
                this.lastPixelY = coords[1];
                paintBrushAt(this.lastPixelX, this.lastPixelY);
                return true;
            } else {
                this.lastPixelX = -1;
                this.lastPixelY = -1;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        if (super.mouseDragged(event, offsetX, offsetY)) {
            return true;
        }
        if (event.button() == 0 || event.button() == 1) {
            int[] coords = screenToLocalPixels(event.x(), event.y());
            if (coords != null) {
                int currX = coords[0];
                int currY = coords[1];

                if (this.lastPixelX != -1 && this.lastPixelY != -1) {
                    paintLine(this.lastPixelX, this.lastPixelY, currX, currY);
                } else {
                    paintBrushAt(currX, currY);
                }

                this.lastPixelX = currX;
                this.lastPixelY = currY;
                return true;
            }
        }
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Sidebar title label
        graphics.text(this.font, "Chalkboard Controls", 15, 20, 0xFFFFFFFF, true);

        // Optimized rendering: Batch contiguous pixels in each row into single draw calls based on dynamic scale
        if (this.pixels != null && this.pixels.length > 0) {
            int startX = getBoardStartX();
            int startY = getBoardStartY();
            int pixelSize = getDynamicPixelSize();

            for (int y = 0; y < this.totalHeight; y++) {
                int rowStart = y * this.totalWidth;
                int runStart = 0;
                int runColor = this.pixels[rowStart];

                for (int x = 0; x < this.totalWidth; x++) {
                    int color = this.pixels[rowStart + x];
                    if (color != runColor) {
                        if (runColor != 0) {
                            int drawX = startX + runStart * pixelSize;
                            int drawY = startY + y * pixelSize;
                            int drawW = (x - runStart) * pixelSize;
                            graphics.fill(drawX, drawY, drawX + drawW, drawY + pixelSize, runColor);
                        }
                        runStart = x;
                        runColor = color;
                    }
                }
                if (runColor != 0) {
                    int drawX = startX + runStart * pixelSize;
                    int drawY = startY + y * pixelSize;
                    int drawW = (this.totalWidth - runStart) * pixelSize;
                    graphics.fill(drawX, drawY, drawX + drawW, drawY + pixelSize, runColor);
                }
            }
        }
    }
}