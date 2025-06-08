// RUTA: src/client/java/com/TNTStudios/vicefinal/client/gui/screen/NucleoScreen.java

package com.TNTStudios.vicefinal.client.gui.screen;

import com.TNTStudios.vicefinal.blocks.NucleoBlockEntity;
import com.TNTStudios.vicefinal.screen.NucleoScreenHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class NucleoScreen extends HandledScreen<NucleoScreenHandler> {

    private NucleoBlockEntity.GameState cachedGameState = NucleoBlockEntity.GameState.IDLE;

    public NucleoScreen(NucleoScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, Text.literal(""));
        this.backgroundWidth = 176;
        this.backgroundHeight = 180;
    }

    @Override
    protected void init() {
        super.init();
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // No dibujamos nada, evitamos que aparezca "Inventario"
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        // Actualizo el estado local que usaré en keyPressed()
        this.cachedGameState = this.handler.getBlockEntity().getGameState();

        drawUI(context);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        // Textura de fondo (si la hubiera)
    }

    private void drawUI(DrawContext context) {
        NucleoBlockEntity be = handler.getBlockEntity();
        if (be == null) return;

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Desactivación del Núcleo").formatted(Formatting.AQUA, Formatting.BOLD), this.width / 2, y + 8, 0xFFFFFF);

        // Ahora el switch usa directamente el estado actual del BlockEntity.
        switch(be.getGameState()) {
            case COUNTDOWN:
                // Pasamos directamente el temporizador restante del servidor.
                drawCountdown(context, be.getGameTimer(), this.width / 2, y + 60);
                break;
            case ACTIVE:
                drawActiveGame(context, be, x, y);
                break;
            case FAILED:
                drawFailureScreen(context, this.width / 2, y + 60);
                break;
            default:
                break;
        }
    }

    // --- [CORREGIDO] drawCountdown ahora es server-authoritative ---
    private void drawCountdown(DrawContext context, int remainingServerTicks, int centerX, int centerY) {
        // Calculamos los segundos restantes basándonos en los ticks que nos da el servidor.
        int secondsLeft = (int) Math.ceil(remainingServerTicks / 20.0);
        if (secondsLeft < 0) secondsLeft = 0; // Nos aseguramos de que no sea negativo.

        String countdownText = String.valueOf(secondsLeft);
        MutableText infoText = Text.literal("¡Prepárate!").formatted(Formatting.YELLOW);
        MutableText instructionText = Text.literal("Pulsa el número que aparezca en menos de 1s").formatted(Formatting.WHITE);

        context.drawCenteredTextWithShadow(textRenderer, infoText, centerX, centerY - 35, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, instructionText, centerX, centerY - 20, 0xFFFFFF);

        context.getMatrices().push();
        context.getMatrices().scale(5.0f, 5.0f, 5.0f);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(countdownText).formatted(Formatting.RED, Formatting.BOLD), (int)(centerX / 5f), (int)(centerY / 5f), 0xFFFFFF);
        context.getMatrices().pop();
    }


    private void drawActiveGame(DrawContext context, NucleoBlockEntity be, int x, int y) {
        if (be.currentNumber != -1) {
            String numberStr = String.valueOf(be.currentNumber);
            context.getMatrices().push();
            context.getMatrices().scale(6.0f, 6.0f, 6.0f);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(numberStr).formatted(Formatting.YELLOW, Formatting.BOLD), (int)((this.width / 2) / 6f), (int)((y + 60) / 6f), 0xFFFFFF);
            context.getMatrices().pop();
        }

        int bottomY = y + this.backgroundHeight - 30;
        MutableText livesText = Text.literal("Vidas: ").formatted(Formatting.WHITE);
        livesText.append(Text.literal(String.valueOf(be.lives)).formatted(be.lives > 1 ? Formatting.GREEN : Formatting.RED, Formatting.BOLD));
        context.drawTextWithShadow(textRenderer, livesText, x + 15, bottomY, 0xFFFFFF);

        MutableText progressText = Text.literal("Progreso: ").formatted(Formatting.WHITE);
        progressText.append(Text.literal(be.progress + " / 15").formatted(Formatting.GOLD, Formatting.BOLD));
        context.drawTextWithShadow(textRenderer, progressText, x + backgroundWidth - textRenderer.getWidth(progressText) - 15, bottomY, 0xFFFFFF);
    }

    private void drawFailureScreen(DrawContext context, int centerX, int centerY) {
        MutableText failText = Text.literal("¡FALLASTE!").formatted(Formatting.RED, Formatting.BOLD);
        context.getMatrices().push();
        context.getMatrices().scale(2.0f, 2.0f, 2.0f);
        context.drawCenteredTextWithShadow(textRenderer, failText, (int)(centerX / 2f), (int)(centerY / 2f), 0xFFFFFF);
        context.getMatrices().pop();

        MutableText cooldownText = Text.literal("El núcleo entra en enfriamiento.").formatted(Formatting.GRAY);
        context.drawCenteredTextWithShadow(textRenderer, cooldownText, centerX, centerY + 30, 0xFFFFFF);
    }


    /**
     * [CORREGIDO] Se reestructura el método para priorizar la lógica del minijuego.
     * Esto asegura que las teclas numéricas sean capturadas por nuestra GUI
     * antes de que cualquier otra parte del juego pueda interceptarlas.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Mi lógica primero. Si la tecla es un número y el juego está activo,
        // la proceso y detengo la propagación del evento devolviendo 'true'.
        if (this.cachedGameState == NucleoBlockEntity.GameState.ACTIVE) {
            final int number = this.getNumberFromKeyCode(keyCode);
            if (number != -1) {
                // Envío el evento al servidor a través del ScreenHandler.
                this.client.interactionManager.clickButton(this.handler.syncId, number);
                return true; // ¡Importante! Indico que ya manejé la tecla.
            }
        }

        // Si mi lógica no manejó la tecla (porque no era un número o el juego no estaba activo),
        // dejo que la clase padre maneje el evento. Esto es crucial para que la tecla de
        // inventario (por defecto 'E') siga cerrando la GUI.
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Convierte un código de tecla GLFW a su valor numérico correspondiente (0-9).
     * Este método maneja tanto las teclas numéricas de la fila superior del teclado
     * como las del teclado numérico (numpad).
     *
     * @param keyCode El código de tecla GLFW a convertir.
     * @return El valor entero (0-9) si la tecla es numérica; de lo contrario, -1.
     */
    private int getNumberFromKeyCode(int keyCode) {
        // Reviso si la tecla está en el rango de los números de la fila superior (ej: '1', '2', ...).
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return keyCode - GLFW.GLFW_KEY_0;
        }

        // Si no, reviso si está en el rango del teclado numérico (ej: 'KP_1', 'KP_2', ...).
        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            return keyCode - GLFW.GLFW_KEY_KP_0;
        }

        // Si no corresponde a ninguna tecla numérica que me interese, devuelvo -1.
        return -1;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}