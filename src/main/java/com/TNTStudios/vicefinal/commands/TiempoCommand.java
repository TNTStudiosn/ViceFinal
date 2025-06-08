package com.TNTStudios.vicefinal.commands;

import com.TNTStudios.vicefinal.entity.SrTiempoEntity;
import com.TNTStudios.vicefinal.registry.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TiempoCommand {

    private static final SimpleCommandExceptionType NO_BOSS_FOUND_EXCEPTION = new SimpleCommandExceptionType(Text.literal("No se encontró a Sr. Tiempo en ningún mundo del servidor."));
    // [NUEVO] Añado una excepción para cuando el jefe ya existe y se intenta invocar de nuevo.
    private static final SimpleCommandExceptionType BOSS_ALREADY_EXISTS_EXCEPTION = new SimpleCommandExceptionType(Text.literal("Ya existe una instancia de Sr. Tiempo. Usa '/tiempo eliminar' primero."));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("tiempo")
                .requires(source -> source.hasPermissionLevel(4))
                // --- Comandos de gestión ---
                .then(literal("spawn")
                        .executes(TiempoCommand::executeSpawn))
                .then(literal("eliminar")
                        .executes(TiempoCommand::executeEliminar))

                // --- Comandos de comportamiento ---
                .then(literal("caminar")
                        .executes(ctx -> executeBehavior(ctx, Behavior.CAMINAR)))
                .then(literal("atacar")
                        .executes(ctx -> executeBehavior(ctx, Behavior.ATACAR)))
                .then(literal("detener")
                        .executes(ctx -> executeBehavior(ctx, Behavior.DETENER)))
                .then(literal("estatua")
                        .executes(ctx -> executeBehavior(ctx, Behavior.ESTATUA)))

                // --- Comandos de ataque ---
                .then(literal("ataque_temporal")
                        .then(argument("rango", IntegerArgumentType.integer(1))
                                .executes(TiempoCommand::executeAtaqueTemporal)))

                // --- Comandos de animación directa ---
                .then(literal("anim")
                        .then(literal("slap").executes(ctx -> executeAnimation(ctx, Animation.SLAP)))
                        .then(literal("charge_up").executes(ctx -> executeAnimation(ctx, Animation.CHARGE_UP)))
                        .then(literal("death").executes(ctx -> executeAnimation(ctx, Animation.DEATH)))
                )
        );
    }

    private enum Behavior {
        CAMINAR, ATACAR, DETENER, ESTATUA
    }

    private enum Animation {
        SLAP, CHARGE_UP, DEATH
    }

    /**
     * [NUEVO] Invoca una nueva instancia de Sr. Tiempo en la ubicación del jugador.
     */
    private static int executeSpawn(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        // Primero, me aseguro de que no exista ya un jefe para evitar duplicados.
        if (!findAllBosses(source.getServer()).isEmpty()) {
            throw BOSS_ALREADY_EXISTS_EXCEPTION.create();
        }

        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = player.getServerWorld();

        // Creo la nueva instancia de la entidad.
        // Utilizo mi registro de entidades para obtener el EntityType correcto.
        SrTiempoEntity boss = new SrTiempoEntity(ModEntities.SR_TIEMPO, world);

        // Coloco al jefe en la posición del jugador que ejecutó el comando.
        boss.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0F);

        // Finalmente, lo añado al mundo para que aparezca.
        world.spawnEntity(boss);

        source.sendFeedback(() -> Text.literal("Sr. Tiempo ha sido invocado.").formatted(Formatting.GOLD), true);
        return 1;
    }

    private static int executeBehavior(CommandContext<ServerCommandSource> context, Behavior behavior) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<SrTiempoEntity> bosses = findAllBosses(source.getServer());
        if (bosses.isEmpty()) {
            throw NO_BOSS_FOUND_EXCEPTION.create();
        }

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

        final int count = bosses.size();
        source.sendFeedback(() -> Text.literal("Comportamiento '" + behavior.name().toLowerCase() + "' aplicado a " + count + " instancias de Sr. Tiempo."), true);

        return count;
    }

    private static int executeAtaqueTemporal(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int rango = IntegerArgumentType.getInteger(context, "rango");
        List<SrTiempoEntity> bosses = findAllBosses(source.getServer());
        if (bosses.isEmpty()) {
            throw NO_BOSS_FOUND_EXCEPTION.create();
        }

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

    private static int executeAnimation(CommandContext<ServerCommandSource> context, Animation animation) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<SrTiempoEntity> bosses = findAllBosses(source.getServer());
        if (bosses.isEmpty()) {
            throw NO_BOSS_FOUND_EXCEPTION.create();
        }

        for (SrTiempoEntity boss : bosses) {
            switch (animation) {
                case SLAP:
                    boss.getController().playSlap();
                    break;
                case CHARGE_UP:
                    boss.getController().playChargeUp();
                    break;
                case DEATH:
                    boss.getController().playDeath();
                    break;
            }
        }

        final int count = bosses.size();
        source.sendFeedback(() -> Text.literal("Animación '" + animation.name().toLowerCase() + "' ejecutada en " + count + " instancias."), true);
        return count;
    }

    private static int executeEliminar(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<SrTiempoEntity> bosses = findAllBosses(source.getServer());
        if (bosses.isEmpty()) {
            throw NO_BOSS_FOUND_EXCEPTION.create();
        }

        int count = bosses.size();
        for (SrTiempoEntity boss : bosses) {
            boss.discard();
        }

        source.sendFeedback(() -> Text.literal("Se eliminaron " + count + " instancias de Sr. Tiempo del servidor.").formatted(Formatting.GREEN), true);
        return count;
    }

    private static List<SrTiempoEntity> findAllBosses(MinecraftServer server) {
        List<SrTiempoEntity> allBosses = new ArrayList<>();
        for (ServerWorld world : server.getWorlds()) {
            allBosses.addAll(world.getEntitiesByClass(SrTiempoEntity.class, new Box(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), Entity::isAlive));
        }
        return allBosses;
    }
}