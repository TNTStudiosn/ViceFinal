package com.TNTStudios.vicefinal.screen;

import com.TNTStudios.vicefinal.blocks.NucleoBlockEntity;
import com.TNTStudios.vicefinal.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;

public class NucleoScreenHandler extends ScreenHandler {
    private final NucleoBlockEntity blockEntity;
    private final ScreenHandlerContext context;

    // Constructor para el servidor, que obtiene el BlockEntity del mundo.
    public NucleoScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, (NucleoBlockEntity) playerInventory.player.getWorld().getBlockEntity(buf.readBlockPos()));
    }

    // Constructor principal que inicializa el ScreenHandler.
    public NucleoScreenHandler(int syncId, PlayerInventory playerInventory, NucleoBlockEntity blockEntity) {
        super(ModScreenHandlers.NUCLEO_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;
        this.context = ScreenHandlerContext.create(blockEntity.getWorld(), blockEntity.getPos());
    }

    // Este método se dispara cuando el cliente envía un "click". Lo usaré para procesar la entrada del minijuego.
    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        // El 'id' será el número que el jugador ha presionado (0-9).
        if (id >= 0 && id <= 9) {
            blockEntity.handlePlayerInput(id);
            return true;
        }
        return super.onButtonClick(player, id);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        // El jugador puede usarlo si está cerca y es el jugador que inició la interacción.
        return blockEntity.isPlayerInteracting(player);
    }

    // Cuando el jugador cierra la GUI, se lo comunico al BlockEntity para que resetee el estado.
    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        blockEntity.stopMinigame();
    }

    // Simplifico el código al no tener que mover items.
    @Override
    public net.minecraft.item.ItemStack quickMove(PlayerEntity player, int slot) {
        return net.minecraft.item.ItemStack.EMPTY;
    }

    public NucleoBlockEntity getBlockEntity() {
        return blockEntity;
    }
}