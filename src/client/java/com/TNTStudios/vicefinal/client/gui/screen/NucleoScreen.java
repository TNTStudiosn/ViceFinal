package com.TNTStudios.vicefinal.client.gui.screen;

import com.TNTStudios.vicefinal.blocks.NucleoBlockEntity;
import com.TNTStudios.vicefinal.screen.NucleoScreenHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class NucleoScreen extends HandledScreen<NucleoScreenHandler> {

    // Una textura de fondo sutil para la GUI. Puedes crear una si quieres.

    public NucleoScreen(NucleoScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 200;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // El fondo semitransparente.
        this.renderBackground(context); // Corrected line
        super.render(context, mouseX, mouseY, delta);
        drawTexts(context);
    }

    private void drawTexts(DrawContext context) {
        int centerX = this.width / 2;
        int y = (this.height - this.backgroundHeight) / 2 + 20;

        // Título e instrucciones
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Desactivación del Núcleo").formatted(Formatting.BOLD, Formatting.AQUA), centerX, y, 0xFFFFFF);
        y += 15;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Presiona rápidamente los números que aparezcan.").formatted(Formatting.WHITE), centerX, y, 0xFFFFFF);
        y += 10;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Puedes usar el teclado numérico o los números superiores.").formatted(Formatting.WHITE), centerX, y, 0xFFFFFF);

        y += 25;

        // El número a presionar
        NucleoBlockEntity be = handler.getBlockEntity();
        if (be.currentNumber != -1) {
            String numberStr = String.valueOf(be.currentNumber);
            // Dibujo el número bien grande en el centro.
            context.getMatrices().push();
            context.getMatrices().scale(4.0f, 4.0f, 4.0f);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(numberStr).formatted(Formatting.YELLOW, Formatting.BOLD), centerX / 4, y / 4, 0xFFFFFF);
            context.getMatrices().pop();
        }

        // Vidas y progreso
        y += 50;
        Text livesText = Text.literal("Vidas restantes: " + be.lives).formatted(be.lives > 1 ? Formatting.GREEN : Formatting.RED);
        context.drawCenteredTextWithShadow(textRenderer, livesText, centerX, y, 0xFFFFFF);

        y += 15;
        Text progressText = Text.literal("Progreso: " + be.progress + " / 15").formatted(Formatting.GOLD);
        context.drawCenteredTextWithShadow(textRenderer, progressText, centerX, y, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Si el jugador presiona ESC, se cierra la pantalla.
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        // Convierto el código de la tecla a un número.
        int number = -1;
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            number = keyCode - GLFW.GLFW_KEY_0;
        } else if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            number = keyCode - GLFW.GLFW_KEY_KP_0;
        }

        // Si es un número válido, se lo envío al servidor.
        if (number != -1) {
            this.client.interactionManager.clickButton(this.handler.syncId, number);
            return true;
        }

        return false;
    }

    // Para evitar que el juego se pause en singleplayer.
    @Override
    public boolean shouldPause() {
        return false;
    }
}