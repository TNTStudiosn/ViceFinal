// RUTA: src/client/java/com/TNTStudios/vicefinal/client/gui/screen/BossChallengeScreen.java
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
import org.lwjgl.glfw.GLFW; // Me aseguro de importar GLFW para los códigos de las teclas.

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
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // Renderizo el fondo de la GUI
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        // Dibujo la textura base de la GUI si la tuviera.
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

        // Uso los getters que definí en el handler.
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

    /**
     * [NUEVO] Sobrescribo el método para capturar las pulsaciones de teclas.
     * Esta es la corrección clave: mi lógica se ejecuta primero. Si proceso
     * una tecla numérica, devuelvo 'true' para indicar que el evento ya
     * fue manejado y no debe propagarse más (evitando, por ejemplo, que
     * la tecla de inventario cierre la GUI).
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // La lógica del minijuego es la prioridad. Solo proceso números si hay uno en pantalla.
        if (this.handler.getCurrentNumber() != -1) {
            final int number = this.getNumberFromKeyCode(keyCode);
            if (number != -1) {
                // Envío la pulsación al servidor a través del ScreenHandler.
                this.client.interactionManager.clickButton(this.handler.syncId, number);
                return true; // ¡Clave! He manejado la tecla, así que detengo su procesamiento.
            }
        }

        // Si no era una tecla numérica o el minijuego no estaba en un estado para recibirla,
        // dejo que el comportamiento por defecto se ejecute (como cerrar con la tecla 'E').
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * [NUEVO] Este método de utilidad convierte un código de tecla GLFW a su
     * número correspondiente (0-9), funcionando tanto para las teclas
     * numéricas superiores como para las del numpad.
     *
     * @param keyCode El código de tecla de GLFW.
     * @return El número del 0 al 9, o -1 si no es una tecla numérica.
     */
    private int getNumberFromKeyCode(int keyCode) {
        // Reviso las teclas numéricas de la fila superior.
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return keyCode - GLFW.GLFW_KEY_0;
        }

        // Reviso las teclas del teclado numérico (numpad).
        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            return keyCode - GLFW.GLFW_KEY_KP_0;
        }

        // Si no es una tecla numérica, devuelvo -1.
        return -1;
    }

    /**
     * [NUEVO] Evito que el juego se pause en el fondo cuando esta GUI está abierta.
     * Esencial para minijuegos o interfaces que no deben detener el mundo.
     */
    @Override
    public boolean shouldPause() {
        return false;
    }
}