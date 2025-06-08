package com.TNTStudios.vicefinal.minigame;

import com.TNTStudios.vicefinal.entity.SrTiempoEntity;
import com.TNTStudios.vicefinal.screen.BossChallengeScreenHandler;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BossMinigameManager {

    private static boolean gameActive = false;
    private static final Map<UUID, Boolean> playerResults = new ConcurrentHashMap<>();
    private static final Map<UUID, BossChallengeScreenHandler> activeHandlers = new ConcurrentHashMap<>();
    private static int totalParticipants = 0;

    // Almaceno la instancia del servidor para no depender de obtenerla a través de un jugador.
    private static MinecraftServer activeServer = null;

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> timeoutTask;

    /**
     * Inicia el minijuego para todos los jugadores que no son operadores, comenzando con una cuenta regresiva.
     * @param server La instancia del servidor.
     */
    public static void start(MinecraftServer server) {
        if (gameActive) {
            return; // Ya hay un juego en curso.
        }

        List<ServerPlayerEntity> participants = server.getPlayerManager().getPlayerList().stream()
                .filter(p -> !p.hasPermissionLevel(4)).toList();

        if (participants.isEmpty()) {
            server.getPlayerManager().broadcast(Text.literal("No hay jugadores elegibles para el desafío.").formatted(Formatting.YELLOW), false);
            return;
        }

        gameActive = true;
        activeServer = server; // Guardo la instancia del servidor actual.
        playerResults.clear();
        activeHandlers.clear();
        totalParticipants = participants.size();

        server.getPlayerManager().broadcast(Text.literal("¡Un desafío ha comenzado! ¡Prepárense!").formatted(Formatting.GOLD, Formatting.BOLD), false);

        // Inicio la secuencia de cuenta regresiva para los participantes.
        startCountdown(participants);
    }

    /**
     * Gestiona la cuenta regresiva y muestra las instrucciones en pantalla.
     * @param participants La lista de jugadores que participan.
     */
    private static void startCountdown(List<ServerPlayerEntity> participants) {
        // Defino los textos de las instrucciones.
        Text instructions = Text.literal("Pulsa los números que aparezcan en pantalla.").formatted(Formatting.YELLOW);

        // Muestro las instrucciones a todos los participantes.
        for (ServerPlayerEntity player : participants) {
            // Configuro los tiempos de fade-in, stay y fade-out para los títulos.
            player.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 40, 5)); // 0.25s fade-in, 2s stay, 0.25s fade-out
            player.networkHandler.sendPacket(new SubtitleS2CPacket(instructions));
        }

        // Programo la cuenta regresiva usando el scheduler.
        AtomicInteger countdown = new AtomicInteger(5);
        for (int i = 0; i <= 5; i++) {
            int currentSecond = 5 - i;
            scheduler.schedule(() -> activeServer.execute(() -> {
                if (!gameActive) return; // Si el juego se canceló, no continúo.

                Text titleText;
                float soundPitch;

                if (currentSecond > 0) {
                    titleText = Text.literal(String.valueOf(currentSecond)).formatted(Formatting.RED, Formatting.BOLD);
                    soundPitch = 0.8f;
                } else {
                    titleText = Text.literal("¡YA!").formatted(Formatting.GREEN, Formatting.BOLD);
                    soundPitch = 1.2f;
                }

                // Envío el título a cada jugador y reproduzco un sonido.
                for (ServerPlayerEntity player : participants) {
                    player.networkHandler.sendPacket(new TitleS2CPacket(titleText));
                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, 1.0f, soundPitch);
                }

                // Al finalizar la cuenta regresiva (currentSecond == 0), lanzo el minijuego.
                if (currentSecond == 0) {
                    launchMinigame(participants);
                }

            }), i, TimeUnit.SECONDS);
        }
    }

    /**
     * Abre la pantalla del minijuego para los jugadores e inicia el temporizador global.
     * Este método se llama después de la cuenta regresiva.
     * @param participants La lista de jugadores.
     */
    private static void launchMinigame(List<ServerPlayerEntity> participants) {
        for (ServerPlayerEntity player : participants) {
            // Verifico que la entidad del jugador no haya sido eliminada (por ejemplo, por desconexión) antes de interactuar con él.
            if (!player.isRemoved()) {
                NamedScreenHandlerFactory factory = new NamedScreenHandlerFactory() {
                    @Override
                    public Text getDisplayName() {
                        return Text.literal("Desafío del Jefe");
                    }

                    @Nullable
                    @Override
                    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
                        return new BossChallengeScreenHandler(syncId, inv);
                    }
                };
                player.openHandledScreen(factory);
            }
        }

        // Ahora que el minijuego ha comenzado de verdad, establezco el tiempo límite global.
        timeoutTask = scheduler.schedule(() -> activeServer.execute(BossMinigameManager::endGame), 30, TimeUnit.SECONDS);
    }

    /**
     * Registra el resultado de un jugador que ha terminado su minijuego.
     * @param uuid El UUID del jugador.
     * @param success Si el jugador tuvo éxito.
     */
    public static void reportResult(UUID uuid, boolean success) {
        if (!gameActive) {
            return;
        }
        // Uso putIfAbsent para asegurar que el primer resultado reportado sea el que cuente.
        playerResults.putIfAbsent(uuid, success);

        // Si todos han reportado su resultado, terminamos el juego inmediatamente.
        if (playerResults.size() >= totalParticipants) {
            // Aseguro que la finalización se ejecute en el hilo principal del servidor.
            if (activeServer != null) {
                activeServer.execute(BossMinigameManager::endGame);
            }
        }
    }

    /**
     * Termina el evento, calcula los resultados y aplica las consecuencias.
     */
    private static void endGame() {
        if (!gameActive) {
            return;
        }
        gameActive = false;
        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(false);
        }

        // Ahora uso la instancia del servidor que guardé, es más seguro.
        if (activeServer == null) return;

        long successes = playerResults.values().stream().filter(b -> b).count();
        double successRate = totalParticipants > 0 ? (double) successes / totalParticipants : 0.0;

        if (successRate > 0.70) {
            activeServer.getPlayerManager().broadcast(Text.literal("¡Desafío superado! El jefe ha sido debilitado.").formatted(Formatting.GREEN, Formatting.BOLD), false);
            applyDamageToBoss(activeServer, 250.0f); // Le bajamos 250 de vida.
        } else {
            activeServer.getPlayerManager().broadcast(Text.literal("¡El desafío ha fracasado! No se coordinaron a tiempo. se necesitan almenos el 70% de ViceCraft apruebe").formatted(Formatting.RED, Formatting.BOLD), false);
        }

        // Cierro las pantallas de los jugadores que no terminaron a tiempo.
        activeServer.execute(() -> activeHandlers.keySet().forEach(uuid -> {
            ServerPlayerEntity p = activeServer.getPlayerManager().getPlayer(uuid);
            // Compruebo que el jugador sigue conectado y tiene la pantalla del desafío abierta.
            if (p != null && p.currentScreenHandler instanceof BossChallengeScreenHandler) {
                p.closeHandledScreen();
            }
        }));

        activeHandlers.clear();
        activeServer = null; // Limpio la referencia al servidor para evitar memory leaks.
    }

    private static void applyDamageToBoss(MinecraftServer server, float damage) {
        for (ServerWorld world : server.getWorlds()) {
            // SOLUCIÓN: Uso getEntitiesByClass para evitar problemas con los genéricos de getEntitiesByType. Es más robusto.
            List<SrTiempoEntity> bosses = world.getEntitiesByClass(SrTiempoEntity.class, new Box(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), e -> true)
                    .stream().filter(e -> e.isAlive() && e.isVulnerable()).toList();
            for (SrTiempoEntity boss : bosses) {
                DamageSource damageSource = world.getDamageSources().magic();
                boss.damage(damageSource, damage);
                boss.getController().triggerTurnTime(); // Hago que reaccione al daño.
            }
        }
    }

    // Métodos para gestionar los ScreenHandlers activos y poder 'tickearlos'.
    public static void registerHandler(BossChallengeScreenHandler handler) {
        if (!gameActive) return;
        activeHandlers.put(handler.getPlayer().getUuid(), handler);
    }

    public static void unregisterHandler(UUID playerUuid) {
        activeHandlers.remove(playerUuid);
    }

    public static void tick() {
        if (!activeHandlers.isEmpty()) {
            // Hago una copia de la colección para evitar ConcurrentModificationException si se desregistra un handler durante el tick.
            new ConcurrentHashMap<>(activeHandlers).values().forEach(BossChallengeScreenHandler::serverTick);
        }
    }
}