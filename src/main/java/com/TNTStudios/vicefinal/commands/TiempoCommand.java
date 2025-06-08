package com.TNTStudios.vicefinal.commands;

import com.TNTStudios.vicefinal.entity.SrTiempoEntity;
import com.TNTStudios.vicefinal.registry.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TiempoCommand {

    private static final SimpleCommandExceptionType NO_BOSS_FOUND_EXCEPTION = new SimpleCommandExceptionType(Text.literal("No se encontró a Sr. Tiempo en ningún mundo del servidor."));
    private static final SimpleCommandExceptionType BOSS_ALREADY_EXISTS_EXCEPTION = new SimpleCommandExceptionType(Text.literal("Ya existe una instancia de Sr. Tiempo. Usa '/tiempo eliminar' primero."));

    // [CORREGIDO] Añado la nueva animación al enum para que el comando la reconozca.
    private enum Animation { SLAP, CHARGE_UP, TURN_TIME, DEATH, CHANNEL }
    private enum Behavior { CAMINAR, ATACAR, DETENER, ESTATUA }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("tiempo")
                .requires(source -> source.hasPermissionLevel(4))
                // --- Comandos de gestión ---
                .then(literal("spawn")
                        .executes(TiempoCommand::executeSpawn))
                .then(literal("eliminar")
                        .executes(TiempoCommand::executeEliminar))
                .then(literal("vulnerable")
                        .then(argument("estado", BoolArgumentType.bool())
                                .executes(TiempoCommand::executeVulnerable)))

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
                                .then(argument("daño", FloatArgumentType.floatArg(0))
                                        .executes(TiempoCommand::executeAtaqueTemporal))))
                .then(literal("fisura_temporal")
                        .then(argument("rango", IntegerArgumentType.integer(1))
                                .then(argument("fuerza", FloatArgumentType.floatArg(0.1f))
                                        .then(argument("daño", FloatArgumentType.floatArg(0))
                                                .executes(TiempoCommand::executeFisuraTemporal)))))

                // --- Comandos de animación directa (CORREGIDOS Y AMPLIADOS) ---
                .then(literal("anim")
                        .then(literal("slap").executes(ctx -> executeAnimation(ctx, Animation.SLAP)))
                        .then(literal("charge_up").executes(ctx -> executeAnimation(ctx, Animation.CHARGE_UP)))
                        .then(literal("death").executes(ctx -> executeAnimation(ctx, Animation.DEATH)))
                        .then(literal("turn_time").executes(ctx -> executeAnimation(ctx, Animation.TURN_TIME)))
                        // [CORREGIDO] Ahora el comando "channel" es válido.
                        .then(literal("channel").executes(ctx -> executeAnimation(ctx, Animation.CHANNEL)))
                )
        );
    }

    // --- Lógica de Comandos ---

    private static int executeAnimation(CommandContext<ServerCommandSource> context, Animation animation) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<SrTiempoEntity> bosses = findAllBosses(source.getServer());
        if (bosses.isEmpty()) throw NO_BOSS_FOUND_EXCEPTION.create();

        for (SrTiempoEntity boss : bosses) {
            // Ahora llamo a los métodos "trigger" del controlador.
            switch (animation) {
                case SLAP:
                    boss.getController().triggerSlap();
                    break;
                case CHARGE_UP:
                    boss.getController().triggerChargeUp();
                    break;
                case TURN_TIME:
                    boss.getController().triggerTurnTime();
                    break;
                case DEATH:
                    boss.kill(); // El método kill() ya gestiona la animación de muerte.
                    break;
                // [NUEVO] Añado la lógica para la animación 'channel'.
                // Recuerda crear el método triggerChannel() en SrTiempoController.
                case CHANNEL:
                    boss.getController().triggerChannel();
                    break;
            }
        }
        source.sendFeedback(() -> Text.literal("Animación '" + animation.name().toLowerCase() + "' ejecutada."), true);
        return bosses.size();
    }

    private static int executeVulnerable(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean estado = BoolArgumentType.getBool(context, "estado");
        List<SrTiempoEntity> bosses = findAllBosses(context.getSource().getServer());
        if (bosses.isEmpty()) throw NO_BOSS_FOUND_EXCEPTION.create();

        for (SrTiempoEntity boss : bosses) {
            boss.setVulnerable(estado);
        }

        String feedback = estado ? "Sr. Tiempo ahora es vulnerable." : "Sr. Tiempo ahora es invulnerable.";
        context.getSource().sendFeedback(() -> Text.literal(feedback).formatted(Formatting.YELLOW), true);
        return bosses.size();
    }

    private static int executeAtaqueTemporal(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int rango = IntegerArgumentType.getInteger(context, "rango");
        float daño = FloatArgumentType.getFloat(context, "daño");
        List<SrTiempoEntity> bosses = findAllBosses(source.getServer());
        if (bosses.isEmpty()) throw NO_BOSS_FOUND_EXCEPTION.create();

        int affectedPlayers = 0;
        for (SrTiempoEntity boss : bosses) {
            ServerWorld world = (ServerWorld) boss.getWorld();
            boss.getController().triggerTurnTime(); // Uso la animación correcta para este ataque.

            world.playSound(null, boss.getBlockPos(), SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.HOSTILE, 2.0f, 0.5f);

            Box effectBox = new Box(boss.getPos(), boss.getPos()).expand(rango);
            List<ServerPlayerEntity> playersInRange = world.getEntitiesByClass(ServerPlayerEntity.class, effectBox, player -> player.isAlive());

            affectedPlayers += playersInRange.size();
            for (ServerPlayerEntity player : playersInRange) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 255, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0, false, false, true));
                player.damage(world.getDamageSources().magic(), daño);
                player.sendMessage(Text.literal("¡El tiempo se ha detenido a tu alrededor!"), true);
            }
        }

        final int finalAffectedPlayers = affectedPlayers;
        source.sendFeedback(() -> Text.literal("Ataque temporal desatado. " + finalAffectedPlayers + " jugadores afectados con " + daño + " de daño."), true);
        return finalAffectedPlayers;
    }

    private static int executeFisuraTemporal(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int rango = IntegerArgumentType.getInteger(context, "rango");
        float fuerza = FloatArgumentType.getFloat(context, "fuerza");
        float daño = FloatArgumentType.getFloat(context, "daño");
        List<SrTiempoEntity> bosses = findAllBosses(source.getServer());
        if (bosses.isEmpty()) throw NO_BOSS_FOUND_EXCEPTION.create();

        int affectedPlayers = 0;
        for (SrTiempoEntity boss : bosses) {
            ServerWorld world = (ServerWorld) boss.getWorld();
            boss.getController().triggerChargeUp();

            world.playSound(null, boss.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 2.0f, 0.2f);

            Box effectBox = new Box(boss.getPos(), boss.getPos()).expand(rango);
            List<ServerPlayerEntity> playersInRange = world.getEntitiesByClass(ServerPlayerEntity.class, effectBox, player -> player.isAlive() && !player.isCreative());

            affectedPlayers += playersInRange.size();
            for (ServerPlayerEntity player : playersInRange) {
                Vec3d playerPos = player.getPos();
                Vec3d bossPos = boss.getPos();
                Vec3d pullVector = bossPos.subtract(playerPos).normalize().multiply(fuerza);
                player.addVelocity(pullVector.x, pullVector.y + 0.1, pullVector.z);
                world.spawnParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
            }

            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                source.getServer().execute(() -> {
                    boss.getController().triggerSlap();
                    world.playSound(null, boss.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 1.0f);
                    for(ServerPlayerEntity player : playersInRange) {
                        if (player.isAlive() && player.getPos().isInRange(boss.getPos(), rango + 2)) {
                            player.damage(world.getDamageSources().magic(), daño);
                        }
                    }
                });
            }, 1, TimeUnit.SECONDS);
        }

        final int finalAffectedPlayers = affectedPlayers;
        source.sendFeedback(() -> Text.literal("Fisura temporal iniciada. Atrayendo a " + finalAffectedPlayers + " jugadores."), true);
        return finalAffectedPlayers;
    }

    // --- El resto de métodos de ayuda permanecen sin cambios ---
    private static int executeSpawn(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        if (!findAllBosses(source.getServer()).isEmpty()) {
            throw BOSS_ALREADY_EXISTS_EXCEPTION.create();
        }
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = player.getServerWorld();
        SrTiempoEntity boss = new SrTiempoEntity(ModEntities.SR_TIEMPO, world);
        boss.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0F);
        world.spawnEntity(boss);
        source.sendFeedback(() -> Text.literal("Sr. Tiempo ha sido invocado.").formatted(Formatting.GOLD), true);
        return 1;
    }

    private static int executeBehavior(CommandContext<ServerCommandSource> context, Behavior behavior) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<SrTiempoEntity> bosses = findAllBosses(source.getServer());
        if (bosses.isEmpty()) throw NO_BOSS_FOUND_EXCEPTION.create();

        for (SrTiempoEntity boss : bosses) {
            if (boss.isDead()) continue;
            // Ahora uso los nuevos métodos para establecer el comportamiento base.
            switch (behavior) {
                case CAMINAR:
                    boss.getController().setBehaviorWalk();
                    break;
                case ATACAR:
                    boss.getController().setBehaviorAttack();
                    break;
                case DETENER:
                    boss.getController().setBehaviorIdle();
                    break;
                case ESTATUA:
                    boss.getController().setBehaviorStatue();
                    break;
            }
        }
        source.sendFeedback(() -> Text.literal("Comportamiento '" + behavior.name().toLowerCase() + "' aplicado."), true);
        return bosses.size();
    }

    private static int executeEliminar(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<SrTiempoEntity> bosses = findAllBosses(source.getServer());
        if (bosses.isEmpty()) throw NO_BOSS_FOUND_EXCEPTION.create();

        int count = bosses.size();
        for (SrTiempoEntity boss : bosses) {
            boss.discard();
        }
        source.sendFeedback(() -> Text.literal("Se eliminaron " + count + " instancias de Sr. Tiempo.").formatted(Formatting.GREEN), true);
        return count;
    }

    private static List<SrTiempoEntity> findAllBosses(MinecraftServer server) {
        List<SrTiempoEntity> allBosses = new ArrayList<>();
        for (ServerWorld world : server.getWorlds()) {
            allBosses.addAll(world.getEntitiesByType(ModEntities.SR_TIEMPO, entity -> true));
        }
        return allBosses;
    }
}