package com.TNTStudios.vicefinal.commands;

import com.TNTStudios.vicefinal.entity.SrTiempoEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TiempoCommand {

    // Mantengo la misma excepción, es clara y reutilizable.
    private static final SimpleCommandExceptionType NO_BOSS_FOUND_EXCEPTION = new SimpleCommandExceptionType(Text.literal("No se encontró a Sr. Tiempo en ningún mundo del servidor."));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("tiempo")
                .requires(source -> source.hasPermissionLevel(4))
                .then(literal("caminar")
                        .executes(ctx -> executeBehavior(ctx, Behavior.CAMINAR))
                )
                .then(literal("atacar")
                        .executes(ctx -> executeBehavior(ctx, Behavior.ATACAR))
                )
                .then(literal("detener")
                        .executes(ctx -> executeBehavior(ctx, Behavior.DETENER))
                )
                .then(literal("estatua")
                        .executes(ctx -> executeBehavior(ctx, Behavior.ESTATUA))
                )
                .then(literal("ataque_temporal")
                        .then(argument("rango", IntegerArgumentType.integer(1))
                                .executes(TiempoCommand::executeAtaqueTemporal)
                        )
                )
        );
    }

    private enum Behavior {
        CAMINAR, ATACAR, DETENER, ESTATUA
    }

    private static int executeBehavior(CommandContext<ServerCommandSource> context, Behavior behavior) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        // Ahora busco TODAS las instancias del jefe, no solo la primera.
        List<SrTiempoEntity> bosses = findAllBosses(source.getServer());
        if (bosses.isEmpty()) {
            throw NO_BOSS_FOUND_EXCEPTION.create();
        }

        // Itera sobre cada instancia del jefe encontrada y aplica el comportamiento.
        for (SrTiempoEntity boss : bosses) {
            switch (behavior) {
                case CAMINAR:
                    boss.getController().setWalking(true);
                    boss.getController().setAggressive(false);
                    boss.getController().playWalk();
                    break;
                case ATACAR:
                    boss.getController().setWalking(false);
                    boss.getController().setAggressive(true);
                    boss.getController().playChannel();
                    break;
                case DETENER:
                    boss.getController().setWalking(false);
                    boss.getController().setAggressive(false);
                    boss.getController().playIdle();
                    break;
                case ESTATUA:
                    boss.getController().setWalking(false);
                    boss.getController().setAggressive(false);
                    boss.getController().playStatue();
                    break;
            }
        }

        // Envío una respuesta más informativa, indicando a cuántas entidades se aplicó el cambio.
        final int count = bosses.size();
        source.sendFeedback(() -> Text.literal("Comportamiento '" + behavior.name().toLowerCase() + "' aplicado a " + count + " instancias de Sr. Tiempo."), true);

        return count;
    }

    private static int executeAtaqueTemporal(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int rango = IntegerArgumentType.getInteger(context, "rango");
        // De nuevo, busco todas las instancias.
        List<SrTiempoEntity> bosses = findAllBosses(source.getServer());
        if (bosses.isEmpty()) {
            throw NO_BOSS_FOUND_EXCEPTION.create();
        }

        // Cada jefe ejecutará el ataque en su propia ubicación.
        for (SrTiempoEntity boss : bosses) {
            ServerWorld world = (ServerWorld) boss.getWorld();
            boss.getController().playTurnTime();

            Vec3d bossPos = boss.getPos();
            world.playSound(null, boss.getBlockPos(), SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.HOSTILE, 2.0f, 0.5f);

            Box effectBox = new Box(bossPos, bossPos).expand(rango);
            List<ServerPlayerEntity> playersInRange = world.getEntitiesByClass(ServerPlayerEntity.class, effectBox, player -> player.isAlive());

            for (ServerPlayerEntity player : playersInRange) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 255, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0, false, false, true));
                player.sendMessage(Text.literal("¡El tiempo se ha detenido a tu alrededor!"), true);
            }
        }

        final int count = bosses.size();
        source.sendFeedback(() -> Text.literal("Ataque temporal desatado desde " + count + " instancias de Sr. Tiempo en un rango de " + rango + " bloques."), true);
        return count;
    }

    /**
     * Método de ayuda para encontrar TODAS las instancias del jefe en el servidor.
     * Itera sobre todos los mundos y recopila una lista de todas las entidades SrTiempoEntity.
     */
    private static List<SrTiempoEntity> findAllBosses(MinecraftServer server) {
        List<SrTiempoEntity> allBosses = new ArrayList<>();
        for (ServerWorld world : server.getWorlds()) {
            // getEntitiesByClass es perfecto para esto, ya que nos devuelve todas las instancias de golpe.
            allBosses.addAll(world.getEntitiesByClass(SrTiempoEntity.class, new Box(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), entity -> true));
        }
        return allBosses;
    }
}