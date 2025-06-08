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

    // --- Variables para manejar el estado y la animación en el cliente ---
    private NucleoBlockEntity.GameState lastKnownState;
    private int clientTicks; // Un temporizador local para la animación.

    public NucleoScreen(NucleoScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, Text.literal(""));
        this.backgroundWidth = 176;
        this.backgroundHeight = 180;
    }

    @Override
    protected void init() {
        super.init();
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
        // Al iniciar la pantalla, sincronizo el estado y reseteo el timer del cliente.
        this.lastKnownState = this.handler.getBlockEntity().getGameState();
        this.clientTicks = 0;
    }

    // --- [ELIMINADO] El método tick() ya no se puede sobreescribir. ---
    // La lógica se ha movido al método render().

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // --- [LÓGICA DE TICK MOVIDA AQUÍ] ---
        // Como el método tick() es 'final' en HandledScreen, muevo la lógica aquí.
        // El método render() se ejecuta en cada frame, siendo el lugar ideal para esto.
        NucleoBlockEntity.GameState currentState = this.handler.getBlockEntity().getGameState();
        if (this.lastKnownState != currentState) {
            this.lastKnownState = currentState;
            this.clientTicks = 0; // Reseteo para la nueva fase (ej. la cuenta atrás).
        }
        // Incremento mi temporizador local en cada frame.
        this.clientTicks++;
        // --- Fin de la lógica movida ---

        // El fondo semitransparente del juego.
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        // Renderizo la lógica de la GUI basada en el estado del juego.
        drawUI(context);

        // Renderizo los tooltips al final para que se dibujen por encima de todo.
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Aquí iría el dibujo de la textura de fondo si tuvieras una. Ejemplo:
        // context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    // El método drawUI ahora usará las variables locales para la animación.
    private void drawUI(DrawContext context) {
        NucleoBlockEntity be = handler.getBlockEntity();
        if (be == null) return;

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Desactivación del Núcleo").formatted(Formatting.AQUA, Formatting.BOLD), this.width / 2, y + 8, 0xFFFFFF);

        // Usamos el estado local 'lastKnownState' para decidir qué dibujar.
        switch(this.lastKnownState) {
            case COUNTDOWN:
                // Paso el temporizador del BlockEntity como el tiempo inicial de la cuenta atrás.
                drawCountdown(context, be.getGameTimer(), this.width / 2, y + 60);
                break;
            case ACTIVE:
                drawActiveGame(context, be, x, y);
                break;
            case FAILED:
                drawFailureScreen(context, this.width / 2, y + 60);
                break;
            default:
                // No dibujo nada si no hay un estado de juego activo.
                break;
        }
    }

    // --- El método drawCountdown usa el timer del cliente para una animación fluida ---
    private void drawCountdown(DrawContext context, int initialTicks, int centerX, int centerY) {
        // Calculo los segundos restantes basándome en el tiempo inicial y los ticks del cliente.
        // Esto crea una animación fluida que no depende de la sincronización del servidor.
        int secondsLeft = (int) Math.ceil((initialTicks - this.clientTicks) / 20.0);
        if (secondsLeft < 0) secondsLeft = 0; // Me aseguro de que no sea negativo.

        String countdownText = String.valueOf(secondsLeft);

        MutableText infoText = Text.literal("¡Prepárate!").formatted(Formatting.YELLOW);
        context.drawCenteredTextWithShadow(textRenderer, infoText, centerX, centerY - 20, 0xFFFFFF);

        context.getMatrices().push();
        context.getMatrices().scale(5.0f, 5.0f, 5.0f);
        // Dibujo el número que calculé localmente.
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(countdownText).formatted(Formatting.RED, Formatting.BOLD), (int)(centerX / 5f), (int)(centerY / 5f), 0xFFFFFF);
        context.getMatrices().pop();
    }

    private void drawActiveGame(DrawContext context, NucleoBlockEntity be, int x, int y) {
        // El número a presionar, grande y en el centro.
        if (be.currentNumber != -1) {
            String numberStr = String.valueOf(be.currentNumber);
            context.getMatrices().push();
            context.getMatrices().scale(6.0f, 6.0f, 6.0f);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(numberStr).formatted(Formatting.YELLOW, Formatting.BOLD), (int)((this.width / 2) / 6f), (int)((y + 60) / 6f), 0xFFFFFF);
            context.getMatrices().pop();
        }

        // --- Información inferior (Vidas y Progreso) ---
        int bottomY = y + this.backgroundHeight - 30;

        // Vidas restantes
        MutableText livesText = Text.literal("Vidas: ").formatted(Formatting.WHITE);
        livesText.append(Text.literal(String.valueOf(be.lives)).formatted(be.lives > 1 ? Formatting.GREEN : Formatting.RED, Formatting.BOLD));
        context.drawTextWithShadow(textRenderer, livesText, x + 15, bottomY, 0xFFFFFF);

        // Progreso
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Llamo primero al padre para manejar el cierre con la tecla de inventario, etc.
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        // Solo acepto input si el juego está activo.
        if (handler.getBlockEntity().getGameState() != NucleoBlockEntity.GameState.ACTIVE) {
            return false;
        }

        int number = -1;
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            number = keyCode - GLFW.GLFW_KEY_0;
        } else if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            number = keyCode - GLFW.GLFW_KEY_KP_9;
        }

        if (number != -1) {
            // Envío el evento al servidor para que procese el número presionado.
            this.client.interactionManager.clickButton(this.handler.syncId, number);
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}