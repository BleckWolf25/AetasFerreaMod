/*
 * @file FallbackConfigScreen.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Aetas Ferrea - Vanilla Fallback Config Screen
 *
 * @description BEHAVIOR:
 * - Provides a basic GUI for the Forge Mod Menu when Configured or YACL are not installed.
 * - Informs the user to edit the toml file directly or install a config GUI mod.
 *
 * @since 07/06/2026
 * @updated 07/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.client;

// ---------- IMPORTS
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// ---------- CLASS
public class FallbackConfigScreen extends Screen {

    // ---------- VARIABLES
    private final Screen parent;

    // ---------- CONSTRUCTOR
    public FallbackConfigScreen(Screen parent) {
        super(Component.literal("Aetas Ferrea Configuration"));
        this.parent = parent;
    }

    // ---------- INITIALIZATION
    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("Back"), (btn) -> {
            this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    // ---------- RENDERING
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("Vanilla Forge does not provide a native config GUI."), this.width / 2, this.height / 2 - 20, 0xAAAAAA);
        graphics.drawCenteredString(this.font, Component.literal("To edit these values in-game, please install 'Configured' or 'YACL'."), this.width / 2, this.height / 2, 0xFFFF00);
        graphics.drawCenteredString(this.font, Component.literal("Otherwise, edit 'aetasferreamod-common.toml' in your config folder."), this.width / 2, this.height / 2 + 20, 0xAAAAAA);
    }
}
