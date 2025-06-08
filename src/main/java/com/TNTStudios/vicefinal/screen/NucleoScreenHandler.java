package com.TNTStudios.vicefinal.screen;

import com.TNTStudios.vicefinal.blocks.NucleoBlockEntity;
import com.TNTStudios.vicefinal.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;

public class NucleoScreenHandler extends ScreenHandler {
    private final NucleoBlockEntity blockEntity;
    private final ScreenHandlerContext context;

    // Constructor para el cliente, que obtiene el BlockEntity del mundo a través del buffer.
    public NucleoScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, (NucleoBlockEntity) playerInventory.player.getWorld().getBlockEntity(buf.readBlockPos()));
    }

    // Constructor principal para el servidor.
    public NucleoScreenHandler(int syncId, PlayerInventory playerInventory, NucleoBlockEntity blockEntity) {
        super(ModScreenHandlers.NUCLEO_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;
        this.context = ScreenHandlerContext.create(blockEntity.getWorld(), blockEntity.getPos());
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= 0 && id <= 9) {
            // Solo procesamos el input si el juego está activo.
            if(this.blockEntity.getGameState() == NucleoBlockEntity.GameState.ACTIVE) {
                blockEntity.handlePlayerInput(id);
            }
            return true;
        }
        return super.onButtonClick(player, id);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        // El jugador puede usarlo mientras sea el que está interactuando.
        // La comprobación de distancia se hace en el tick del BlockEntity.
        return blockEntity.isPlayerInteracting(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // Cuando el jugador cierra la GUI (o es forzado a cerrarla),
        // le comunicamos al BlockEntity que detenga el minijuego.
        // El BlockEntity se encargará de poner el estado en IDLE o COOLDOWN.
        // La forma correcta de comprobar si estamos en el servidor es `!world.isClient`.
        if (!player.getWorld().isClient) {
            blockEntity.stopMinigame();
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    public NucleoBlockEntity getBlockEntity() {
        return blockEntity;
    }
}