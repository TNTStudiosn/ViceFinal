package com.TNTStudios.vicefinal;

import com.TNTStudios.vicefinal.blocks.VicefinalBlocks;
import com.TNTStudios.vicefinal.commands.TNTAlertCommand;
import com.TNTStudios.vicefinal.registry.ModBlockEntities;
import com.TNTStudios.vicefinal.registry.ModEntities;
import com.TNTStudios.vicefinal.registry.ModScreenHandlers; // Importo la nueva clase
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class Vicefinal implements ModInitializer {

    @Override
    public void onInitialize() {
        ModEntities.register();
        VicefinalBlocks.register();
        ModBlockEntities.register();
        ModScreenHandlers.register(); // Añado el registro de los ScreenHandlers

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TNTAlertCommand.register(dispatcher, registryAccess);
        });
    }
}