package com.TNTStudios.vicefinal;

import com.TNTStudios.vicefinal.blocks.VicefinalBlocks;
import com.TNTStudios.vicefinal.commands.BossChallengeCommand;
import com.TNTStudios.vicefinal.commands.TNTAlertCommand;
import com.TNTStudios.vicefinal.commands.TiempoCommand; // Importo mi nuevo comando
import com.TNTStudios.vicefinal.minigame.BossMinigameManager;
import com.TNTStudios.vicefinal.registry.ModBlockEntities;
import com.TNTStudios.vicefinal.registry.ModEntities;
import com.TNTStudios.vicefinal.registry.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;


public class Vicefinal implements ModInitializer {

    @Override
    public void onInitialize() {
        ModEntities.register();
        VicefinalBlocks.register();
        ModBlockEntities.register();
        ModScreenHandlers.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TNTAlertCommand.register(dispatcher, registryAccess);
            TiempoCommand.register(dispatcher, registryAccess);
            BossChallengeCommand.register(dispatcher, registryAccess); // <- REGISTRO MI NUEVO COMANDO
        });

        // REGISTRO EL TICK DEL LADO SERVIDOR PARA EL MANAGER DEL MINIJUEGO
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            BossMinigameManager.tick();
        });
    }
}