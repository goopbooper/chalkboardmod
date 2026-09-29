/*package com.example;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox; // or TextFieldWidget depending on mappings
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public class BoardScreen extends Screen {
	private EditBox inputBox;

	public BoardScreen(Component title) {
		super(title);
	}

	@Override
	protected void init() {
		// 1. Setup the text input box widget (Font, x, y, width, height, narration/message)
		this.inputBox = new EditBox(this.font, 40, 40, 200, 20, Component.literal("Input Field"));
		this.inputBox.setMaxLength(256);
		
		// Register the input box to automate both rendering and focus mapping
		this.addRenderableWidget(this.inputBox);
		
		// Set focus to the input box automatically when the GUI opens
		this.setFocused(this.inputBox);

		// 2. Setup the existing Button (shifted down slightly to Y=80 to prevent overlap)
		Button buttonWidget = Button.builder(Component.literal("Hello World"), (btn) -> {
			this.minecraft.gui.toastManager().addToast(
					new SystemToast(SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.nullToEmpty("Hello World!"), Component.nullToEmpty("This is a toast."))
			);
		}).bounds(40, 80, 120, 20).build();

		this.addRenderableWidget(buttonWidget);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// Allow input box to handle typing actions/enter/backspace hotkeys first
		if (this.inputBox.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		// Mandatory for rendering text keys (letters, symbols, and numbers)
		if (this.inputBox.charTyped(chr, modifiers)) {
			return true;
		}
		return super.charTyped(chr, modifiers);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		// Draws custom text cleanly using the GuiGraphicsExtractor instance
		graphics.text(this.font, "Enter text here:", 40, 40 - this.font.lineHeight - 4, 0xFFFFFFFF, true);
		graphics.text(this.font, "Special Button", 40, 80 - this.font.lineHeight - 4, 0xFFFFFFFF, true);
	}
}
*/