/**
 * @file FallbackConfigScreen.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Basic fallback configuration screen for Aetas Ferrea.
 *
 * @description
 * A basic fallback GUI screen that notifies players how to edit Aetas Ferrea configurations
 * when advanced config library mods (like Configured or YACL) are not installed.
 *
 * @since 20/05/2026
 * @updated 08/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.client;

// ---------- IMPORTS
import javax.annotation.Nonnull;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// ---------- CLASS: FALLBACK CONFIG SCREEN
public class FallbackConfigScreen extends Screen {

    // ---------- FIELDS & VARIABLES
    private final Screen parent;

    // Cache components to prevent continuous memory allocation during the render loop
    private static final Component TITLE_TEXT = Component.translatable("gui.aetasferreamod.config.title");
    private static final Component LINE_1 = Component.translatable("gui.aetasferreamod.config.line1");
    private static final Component LINE_2 = Component.translatable("gui.aetasferreamod.config.line2");
    private static final Component LINE_3 = Component.translatable("gui.aetasferreamod.config.line3");
    private static final Component BTN_BACK = Component.translatable("gui.aetasferreamod.config.back");

    // ---------- CONSTRUCTOR
    public FallbackConfigScreen(Screen parent) {
        super(TITLE_TEXT);
        this.parent = parent;
    }

    // ---------- INITIALIZATION (GUI SETUP)
    @Override
    @SuppressWarnings("null")
    protected void init() {
        super.init();
        
        this.addRenderableWidget(Button.builder(BTN_BACK, (btn) -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }).bounds(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    // ---------- RENDERING
    @Override
    @SuppressWarnings("null")
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render background and cached text components
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // Draw cached strings
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Draw informational lines
        graphics.drawCenteredString(this.font, LINE_1, this.width / 2, this.height / 2 - 20, 0xAAAAAA);
        graphics.drawCenteredString(this.font, LINE_2, this.width / 2, this.height / 2, 0xFFFF00);
        graphics.drawCenteredString(this.font, LINE_3, this.width / 2, this.height / 2 + 20, 0xAAAAAA);
    }
}
