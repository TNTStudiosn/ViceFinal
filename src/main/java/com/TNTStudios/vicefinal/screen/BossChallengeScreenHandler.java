// E:\TNTStudiosn\TNTStudiosn\TNTMods Fabric\ViceFinal\src\main\java\com\TNTStudios\vicefinal\screen\BossChallengeScreenHandler.java
package com.TNTStudios.vicefinal.screen;

import com.TNTStudios.vicefinal.minigame.BossMinigameManager;
import com.TNTStudios.vicefinal.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.Random;

public class BossChallengeScreenHandler extends ScreenHandler {
    private final PlayerEntity player;
    private final PropertyDelegate propertyDelegate;
    private final Random random = new Random();

    // Estado del minijuego por jugador
    private int lives = 2;
    private int progress = 0;
    private int currentNumber = -1;
    private int numberTimer = 0; // en ticks

    private static final int REQUIRED_PROGRESS = 10;
    private static final int NUMBER_TIMEOUT_TICKS = 20;

    public BossChallengeScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new ArrayPropertyDelegate(4));
    }

    public BossChallengeScreenHandler(int syncId, PlayerInventory playerInventory, PropertyDelegate delegate) {
        super(ModScreenHandlers.BOSS_CHALLENGE_SCREEN_HANDLER, syncId);
        checkDataCount(delegate, 4);
        this.player = playerInventory.player;
        this.propertyDelegate = delegate;
        this.addProperties(delegate);

        BossMinigameManager.registerHandler(this);
        generateNextNumber();
    }

    // --- MÉTODOS GETTER PARA LA PANTALLA ---
    // Añado estos métodos para que la pantalla pueda leer los datos de forma segura.

    public int getLives() {
        return this.propertyDelegate.get(0);
    }

    public int getProgress() {
        return this.propertyDelegate.get(1);
    }

    public int getCurrentNumber() {
        return this.propertyDelegate.get(2);
    }

    public int getRequiredProgress() {
        return this.propertyDelegate.get(3);
    }

    // --- LÓGICA DEL HANDLER (sin cambios) ---

    public PlayerEntity getPlayer() {
        return this.player;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= 0 && id <= 9) {
            if (id == this.currentNumber) {
                handleSuccess();
            } else {
                handleFailure();
            }
            return true;
        }
        return super.onButtonClick(player, id);
    }

    public void serverTick() {
        if (this.numberTimer > 0) {
            this.numberTimer--;
            if (this.numberTimer == 0) {
                handleFailure();
            }
        }
        updateProperties();
    }

    private void handleSuccess() {
        this.progress++;
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.5f);

        if (this.progress >= REQUIRED_PROGRESS) {
            endMinigame(true);
        } else {
            generateNextNumber();
        }
    }

    private void handleFailure() {
        this.lives--;
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 1.0f);

        if (this.lives <= 0) {
            endMinigame(false);
        } else {
            generateNextNumber();
        }
    }

    private void generateNextNumber() {
        this.currentNumber = random.nextInt(10);
        this.numberTimer = NUMBER_TIMEOUT_TICKS;
        updateProperties();
    }

    private void endMinigame(boolean success) {
        BossMinigameManager.reportResult(this.player.getUuid(), success);
        if (this.player instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) this.player).closeHandledScreen();
        }
    }

    private void updateProperties() {
        this.propertyDelegate.set(0, this.lives);
        this.propertyDelegate.set(1, this.progress);
        this.propertyDelegate.set(2, this.currentNumber);
        this.propertyDelegate.set(3, REQUIRED_PROGRESS);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        BossMinigameManager.unregisterHandler(player.getUuid());
        BossMinigameManager.reportResult(player.getUuid(), false);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}