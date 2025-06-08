// E:\TNTStudiosn\TNTStudiosn\TNTMods Fabric\ViceFinal\src\client\java\com\TNTStudios\vicefinal\client\gui\screen\BossChallengeScreen.java
package com.TNTStudios.vicefinal.client.gui.screen;

import com.TNTStudios.vicefinal.screen.BossChallengeScreenHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

// Me aseguro de que la clase extienda HandledScreen con el tipo específico de mi handler.
public class BossChallengeScreen extends HandledScreen<BossChallengeScreenHandler> {




    public BossChallengeScreen(BossChallengeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        // Ajusto el tamaño del fondo según mi textura. Si no hay textura, estos valores son para la lógica de posicionamiento.
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // Inicializo cualquier widget o botón aquí si fuera necesario.
        // Por ejemplo, aquí irían los botones numéricos del 0 al 9.
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // Renderizo el fondo de la GUI
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        // Dibujo la textura base de la GUI.
        // context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // El método render se encarga de todo el dibujado.
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawUI(context); // Llamo a mi lógica de dibujado personalizada.
        // El tooltip de los slots se dibuja automáticamente si los tuviera.
        // drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void drawUI(DrawContext context) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Desafío del Jefe").formatted(Formatting.RED, Formatting.BOLD), this.width / 2, y + 8, 0xFFFFFF);

        // SOLUCIÓN: Ahora uso los getters que definí en el handler.
        // El compilador ya sabe que `this.handler` es de tipo `BossChallengeScreenHandler` gracias a la firma de la clase.
        int lives = this.handler.getLives();
        int progress = this.handler.getProgress();
        int currentNumber = this.handler.getCurrentNumber();
        int requiredProgress = this.handler.getRequiredProgress();

        // Dibujo el número a pulsar
        if (currentNumber != -1) {
            String numberStr = String.valueOf(currentNumber);
            context.getMatrices().push();
            context.getMatrices().scale(6.0f, 6.0f, 6.0f);
            // Centro el texto correctamente después de escalar.
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(numberStr).formatted(Formatting.YELLOW, Formatting.BOLD), (int)((this.width / 2) / 6f), (int)((y + 60) / 6f), 0xFFFFFF);
            context.getMatrices().pop();
        }

        // Dibujo Vidas y Progreso
        int bottomY = y + this.backgroundHeight - 30;
        MutableText livesText = Text.literal("Vidas: ").formatted(Formatting.WHITE);
        livesText.append(Text.literal(String.valueOf(lives)).formatted(lives > 1 ? Formatting.GREEN : Formatting.RED, Formatting.BOLD));
        context.drawTextWithShadow(textRenderer, livesText, x + 15, bottomY, 0xFFFFFF);

        MutableText progressText = Text.literal("Progreso: ").formatted(Formatting.WHITE);
        progressText.append(Text.literal(progress + " / " + requiredProgress).formatted(Formatting.GOLD, Formatting.BOLD));
        context.drawTextWithShadow(textRenderer, progressText, x + backgroundWidth - textRenderer.getWidth(progressText) - 15, bottomY, 0xFFFFFF);
    }
}