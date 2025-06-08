package com.TNTStudios.vicefinal.commands;

import com.TNTStudios.vicefinal.entity.SrTiempoController;
import com.TNTStudios.vicefinal.registry.ModEntities;
import com.TNTStudios.vicefinal.entity.SrTiempoEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
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
import java.util.Collection; // [MI NOTA]: Cambio List por Collection para mayor flexibilidad.
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TiempoCommand {

    // [MI NOTA]: La excepción puede eliminarse si prefiero que el comando simplemente no haga nada si no hay jefes.
    // De momento, la dejo comentada por si la necesitamos en el futuro.
    // private static final SimpleCommandExceptionType NO_BOSS_FOUND_EXCEPTION = new SimpleCommandExceptionType(Text.literal("No se encontró a Sr. Tiempo en ningún mundo del servidor."));

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
        // [MI NOTA]: Buscamos todas las instancias con el nuevo método.
        Collection<SrTiempoEntity> bosses = findAllBosses(source.getServer());

        // [MI NOTA]: Iteramos sobre cada jefe y le aplicamos el comportamiento.
        // Ahora el controller recibirá las órdenes y la entidad reaccionará.
        for (SrTiempoEntity boss : bosses) {
            // [MI NOTA]: El controller es la clave para manejar el estado. Esta parte estaba perfecta.
            SrTiempoController controller = boss.getController();
            switch (behavior) {
                case CAMINAR:
                    controller.setWalking(true);
                    controller.setAggressive(false);
                    controller.playWalk();
                    break;
                case ATACAR:
                    controller.setWalking(false);
                    controller.setAggressive(true);
                    controller.playChannel();
                    break;
                case DETENER:
                    controller.setWalking(false);
                    controller.setAggressive(false);
                    controller.playIdle();
                    break;
                case ESTATUA:
                    controller.setWalking(false);
                    controller.setAggressive(false);
                    controller.playStatue();
                    break;
            }
        }

        int count = bosses.size();
        source.sendFeedback(() -> Text.literal("Comportamiento '" + behavior.name().toLowerCase() + "' aplicado a " + count + " instancias de Sr. Tiempo."), true);
        return count;
    }

    private static int executeAtaqueTemporal(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int rango = IntegerArgumentType.getInteger(context, "rango");
        Collection<SrTiempoEntity> bosses = findAllBosses(source.getServer());

        for (SrTiempoEntity boss : bosses) {
            ServerWorld world = (ServerWorld) boss.getWorld();
            boss.getController().playTurnTime();

            Vec3d bossPos = boss.getPos();
            world.playSound(null, boss.getBlockPos(), SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.HOSTILE, 2.0f, 0.5f);

            Box effectBox = new Box(bossPos, bossPos).expand(rango);
            List<ServerPlayerEntity> playersInRange = world.getEntitiesByClass(ServerPlayerEntity.class, effectBox, PlayerEntity::isAlive);

            for (ServerPlayerEntity player : playersInRange) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 255, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0, false, false, true));
                player.sendMessage(Text.literal("¡El tiempo se ha detenido a tu alrededor!"), true);
            }
        }

        int count = bosses.size();
        source.sendFeedback(() -> Text.literal("Ataque temporal desatado desde " + count + " instancias de Sr. Tiempo en un rango de " + rango + " bloques."), true);
        return count;
    }

    /**
     * [MI NOTA]: MÉTODO CORREGIDO.
     * Ahora busco TODAS las instancias del jefe usando su EntityType registrado.
     * Esta es la forma más segura y optimizada de encontrar entidades específicas.
     */
    private static Collection<SrTiempoEntity> findAllBosses(MinecraftServer server) {
        List<SrTiempoEntity> allBosses = new ArrayList<>();
        for (ServerWorld world : server.getWorlds()) {
            // [MI NOTA]: El cambio clave está aquí. Uso getEntitiesByType con mi EntityType.
            // Esto es mucho más fiable que buscar por la clase de Java.
            // Debo reemplazar ModEntities.SR_TIEMPO por el nombre real de mi campo de EntityType.
            allBosses.addAll(world.getEntitiesByType(ModEntities.SR_TIEMPO, entity -> true));
        }
        return allBosses;
    }
}