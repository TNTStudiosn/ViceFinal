// RUTA: src/main/java/com/TNTStudios/vicefinal/commands/BossChallengeCommand.java
package com.TNTStudios.vicefinal.commands;

import com.TNTStudios.vicefinal.minigame.BossMinigameManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class BossChallengeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("bosschallenge")
                .requires(source -> source.hasPermissionLevel(4))
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    source.sendFeedback(() -> Text.literal("Iniciando el desafío del jefe para todos los jugadores."), true);
                    BossMinigameManager.start(source.getServer());
                    return 1;
                })
        );
    }
}