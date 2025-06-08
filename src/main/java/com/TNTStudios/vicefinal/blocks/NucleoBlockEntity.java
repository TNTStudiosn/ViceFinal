package com.TNTStudios.vicefinal.blocks;

import com.TNTStudios.vicefinal.mixin.MobEntityAccessor;
import com.TNTStudios.vicefinal.registry.ModBlockEntities;
import com.TNTStudios.vicefinal.screen.NucleoScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import toni.immersivemessages.api.ImmersiveMessage;
import toni.immersivemessages.api.SoundEffect;
import toni.immersivemessages.api.TextAnchor;
import toni.immersivemessages.util.ImmersiveColor;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class NucleoBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {

    // --- [NUEVO] Máquina de estados para el minijuego ---
    public enum GameState {
        IDLE,       // Esperando interacción
        COUNTDOWN,  // Cuenta regresiva para empezar
        ACTIVE,     // Minijuego en curso
        FAILED,     // El jugador ha fallado
        COOLDOWN    // Enfriamiento después de fallar
    }

    private GameState gameState = GameState.IDLE;
    private int gameTimer = 0; // Temporizador para la cuenta regresiva y otros delays.

    // --- Variables de la Mecánica ---
    private final Random random = new Random();

    // --- Estado de Oleadas ---
    private int waveTimer = 20;
    private int mobsLeftToSpawn = 0;
    private int spawnTickDelay = 0;
    private double currentHealthMultiplier = 1.0;
    private double currentDamageMultiplier = 1.0;

    // --- Estado del Minijuego ---
    @Nullable
    private UUID interactingPlayer = null;
    private long cooldownEndTime = 0;
    public int lives = 2;
    public int currentNumber = -1;
    public int progress = 0;
    public int numberTimer = 0; // Timer para el número actual, en ticks (20 ticks = 1 segundo)

    // --- Parámetros de la mecánica ---
    private static final int REQUIRED_PROGRESS = 15;
    private static final int INITIAL_LIVES = 3;
    private static final int COUNTDOWN_SECONDS = 3;
    private static final int SPAWN_RADIUS = 20;
    private static final int MIN_SPAWN_RADIUS = 8;
    private static final int CORE_DEFENSE_RADIUS = 30;
    private static final int BASE_MOBS_PER_WAVE = 8;
    private static final int MOBS_PER_ADDITIONAL_PLAYER = 3;
    private static final double BASE_HEALTH_MULTIPLIER = 1.5;
    private static final double HEALTH_INCREASE_PER_PLAYER = 0.1;
    private static final double BASE_DAMAGE_MULTIPLIER = 1.8;
    private static final double DAMAGE_INCREASE_PER_PLAYER = 0.05;
    private static final int MOBS_PER_BATCH = 5;
    private static final int SPAWN_DELAY_TICKS = 4;
    private static final List<EntityType<? extends HostileEntity>> MOB_TYPES = List.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER,
            EntityType.HUSK, EntityType.STRAY, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER
    );

    public NucleoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUCLEO_BLOCK_ENTITY, pos, state);
    }

    // --- Lógica de Ticks (Servidor) ---
    public static void tick(World world, BlockPos pos, BlockState state, NucleoBlockEntity be) {
        if (world.isClient) {
            return;
        }

        // Delegamos el tick a un método de la instancia para tener un código más limpio.
        be.serverTick(world, pos, state);
    }

    private void serverTick(World world, BlockPos pos, BlockState state) {
        // ... (El switch principal ahora necesita manejar el estado FAILED)
        switch (gameState) {
            case COUNTDOWN:
                handleCountdownState(world);
                break;
            case ACTIVE:
                handleActiveState(world, pos);
                break;
            case FAILED:
                // --- [AÑADIR] Lógica para el estado FAILED ---
                handleFailedState(world);
                break;
            case COOLDOWN:
                // El cooldown ahora se maneja aquí.
                if (world.getTime() >= this.cooldownEndTime) {
                    this.gameState = GameState.IDLE;
                    markDirtyAndSync();
                }
                // Las oleadas solo se activan si no hay un minijuego o cooldown activo.
            case IDLE:
                tickWaveSpawner((ServerWorld) world);
                break;
        }
    }

    private void handleCountdownState(World world) {
        // Decremento el timer del countdown (20 ticks por segundo).
        gameTimer--;

        // Siempre sincronizo el estado para que el cliente actualice el número visible.
        markDirtyAndSync();

        // Cada vez que pasamos a un nuevo segundo (múltiplo de 20 ticks), reproduzco un sonido de tick.
        if (gameTimer % 20 == 0 && gameTimer > 0) {
            world.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(),
                    SoundCategory.BLOCKS,
                    0.5f,
                    1.2f
            );
        }

        // Cuando la cuenta regresiva llega a 0, inicio el minijuego.
        if (gameTimer <= 0) {
            this.gameState = GameState.ACTIVE;
            world.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    SoundEvents.ENTITY_PLAYER_LEVELUP,
                    SoundCategory.BLOCKS,
                    0.8f,
                    1.5f
            );
            generateNextNumber(); // Genero el primer número del minijuego.
            markDirtyAndSync();   // Sincronizo de nuevo para que el cliente vea el cambio de estado y el número.
        }
    }



    private void handleActiveState(World world, BlockPos pos) {
        PlayerEntity player = world.getPlayerByUuid(this.interactingPlayer);
        if (player == null || player.isDead() || player.getPos().distanceTo(pos.toCenterPos()) > 8.0) {
            failMinigame();
            return;
        }

        // Decremento el timer del número actual
        numberTimer--;
        if (numberTimer <= 0) {
            // El jugador no respondió a tiempo → pierde una vida
            this.lives--;
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 1.0f, 0.8f);

            if (this.lives <= 0) {
                failMinigame();
            } else {
                generateNextNumber();
            }
            markDirtyAndSync();
        }
    }


    // --- Lógica del Minijuego ---

    public void prepareMinigame(PlayerEntity player) {
        if (world == null || world.isClient || gameState != GameState.IDLE) return;

        this.interactingPlayer = player.getUuid();
        this.lives = INITIAL_LIVES;
        this.progress = 0;
        this.currentNumber = -1;
        this.gameState = GameState.COUNTDOWN;
        this.gameTimer = COUNTDOWN_SECONDS * 20; // 3 segundos de cuenta regresiva.

        markDirtyAndSync();
    }

    // --- [AÑADIR] Nuevo método para manejar el estado FAILED ---
    private void handleFailedState(World world) {
        // Este estado sirve como un pequeño delay para que el jugador vea el mensaje de fallo.
        gameTimer--;
        if (gameTimer <= 0) {
            // Cuando el temporizador termina, cierro la pantalla del jugador.
            // Al cerrarse, se llamará a 'onClosed' en el ScreenHandler, que activará el cooldown.
            PlayerEntity player = world.getPlayerByUuid(this.interactingPlayer);
            if (player instanceof ServerPlayerEntity serverPlayer) {
                // La pantalla solo se cierra si el jugador todavía la tiene abierta.
                if (serverPlayer.currentScreenHandler instanceof NucleoScreenHandler) {
                    serverPlayer.closeHandledScreen();
                }
            }

            // Si el jugador ya no está (se desconectó, etc.), simplemente paso al cooldown.
            // La llamada a `stopMinigame` desde `onClosed` se encargará de esto.
            // Si el jugador no existe, lo fuerzo para evitar quedarme atascado.
            if (player == null) {
                stopMinigame();
            }
        }
    }

    // --- [MODIFICAR] El método stopMinigame ---
    public void stopMinigame() {
        // Ahora este método es más inteligente. Decide si pasar a IDLE o a COOLDOWN.
        if (this.gameState == GameState.IDLE || this.gameState == GameState.COOLDOWN) return;

        // Si el juego se detuvo porque el jugador falló, activamos el enfriamiento.
        if (this.gameState == GameState.FAILED) {
            this.gameState = GameState.COOLDOWN;
            this.cooldownEndTime = world.getTime() + (30 * 20); // 30 segundos de cooldown.
        } else {
            // Si se detuvo por cualquier otra razón (ej. el jugador cerró la GUI manualmente),
            // simplemente volvemos al estado de espera.
            this.gameState = GameState.IDLE;
        }

        this.interactingPlayer = null;
        this.currentNumber = -1;
        markDirtyAndSync();
    }


    // --- [MODIFICAR] El método failMinigame ---
    public void failMinigame() {
        if (world == null || world.isClient() || gameState != GameState.ACTIVE) return;

        // En lugar de cerrar la pantalla aquí, ahora pongo el estado en FAILED
        // y configuro un pequeño temporizador para mostrar el mensaje de fallo.
        this.gameState = GameState.FAILED;
        this.gameTimer = 40; // 2 segundos para que el jugador lea el mensaje en la GUI.

        PlayerEntity player = world.getPlayerByUuid(this.interactingPlayer);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.literal("Has fallado. El núcleo entra en enfriamiento.").formatted(Formatting.RED), false);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.5f, 1.0f);
        }

        // El resto (interactingPlayer = null, etc.) se gestionará en stopMinigame
        // cuando la pantalla finalmente se cierre.
        markDirtyAndSync();
    }


    public void winMinigame() {
        if (world == null || world.isClient() || world.getServer() == null || gameState != GameState.ACTIVE) return;

        PlayerEntity player = world.getPlayerByUuid(this.interactingPlayer);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.closeHandledScreen();
        }

        world.breakBlock(getPos(), false);
        world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.0f, World.ExplosionSourceType.BLOCK);
        world.getServer().getPlayerManager().broadcast(Text.literal("¡El núcleo ha sido destruido!").formatted(Formatting.GREEN, Formatting.BOLD), false);

        // No necesito llamar a stopMinigame() porque el bloque ya no existirá.
    }

    public void handlePlayerInput(int number) {
        if (gameState != GameState.ACTIVE || world == null) return;

        if (number != this.currentNumber) {
            this.lives--;
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 1.0f, 1.0f);
            if (this.lives <= 0) {
                failMinigame();
            } else {
                generateNextNumber();
            }
        } else {
            this.progress++;
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 0.5f, 1.5f);
            if (this.progress >= REQUIRED_PROGRESS) {
                winMinigame();
            } else {
                generateNextNumber();
            }
        }
        markDirtyAndSync();
    }

    private void generateNextNumber() {
        this.currentNumber = random.nextInt(10); // Número aleatorio del 0 al 9.
        this.numberTimer = 20; // tiempo para poner tecla
    }


    // --- Getters para la GUI y el Bloque ---
    public GameState getGameState() {
        return gameState;
    }

    public int getGameTimer() {
        return gameTimer;
    }

    public boolean isPlayerInteracting(PlayerEntity player) {
        return player.getUuid().equals(this.interactingPlayer);
    }

    // Método que faltaba para comprobar si el juego está en marcha.
    public boolean isGameActive() {
        // Un juego se considera activo si está en la cuenta regresiva o en la fase principal.
        return this.gameState == GameState.COUNTDOWN || this.gameState == GameState.ACTIVE;
    }

    public boolean isOnCooldown() {
        return world != null && world.getTime() < this.cooldownEndTime;
    }

    public long getCooldownSeconds() {
        if (!isOnCooldown() || world == null) return 0;
        return (cooldownEndTime - world.getTime()) / 20;
    }


    // --- Lógica de Oleadas (Sin cambios, pero la muevo a su propio método de tick) ---
    private void tickWaveSpawner(ServerWorld world) {
        if (mobsLeftToSpawn > 0) {
            spawnTickDelay--;
            if (spawnTickDelay <= 0) {
                spawnMobBatch(world);
                spawnTickDelay = SPAWN_DELAY_TICKS;
            }
            return;
        }

        if (waveTimer > 0) {
            waveTimer--;
            return;
        }

        waveTimer = 500 + random.nextInt(400);
        startWave(world);
    }

    private void startWave(ServerWorld world) {
        sendWaveNotification(world.getServer(), pos);
        int playerCount = world.getServer().getPlayerManager().getPlayerList().size();
        if (playerCount == 0) return;

        this.mobsLeftToSpawn = BASE_MOBS_PER_WAVE + (playerCount > 1 ? (playerCount - 1) * MOBS_PER_ADDITIONAL_PLAYER : 0);
        this.currentHealthMultiplier = BASE_HEALTH_MULTIPLIER + (playerCount > 1 ? (playerCount - 1) * HEALTH_INCREASE_PER_PLAYER : 0);
        this.currentDamageMultiplier = BASE_DAMAGE_MULTIPLIER + (playerCount > 1 ? (playerCount - 1) * DAMAGE_INCREASE_PER_PLAYER : 0);
        this.spawnTickDelay = 0;
    }

    // --- El resto de métodos de oleadas, NBT y sincronización permanecen casi iguales ---

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("CooldownEndTime", this.cooldownEndTime);
        if (this.interactingPlayer != null) {
            nbt.putUuid("InteractingPlayer", this.interactingPlayer);
        }
        // Guardo el estado del juego
        nbt.putInt("GameState", this.gameState.ordinal());
        nbt.putInt("GameTimer", this.gameTimer);
        nbt.putInt("GameLives", this.lives);
        nbt.putInt("GameProgress", this.progress);
        nbt.putInt("CurrentNumber", this.currentNumber);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.cooldownEndTime = nbt.getLong("CooldownEndTime");
        if (nbt.containsUuid("InteractingPlayer")) {
            this.interactingPlayer = nbt.getUuid("InteractingPlayer");
        } else {
            this.interactingPlayer = null;
        }
        // Leo el estado del juego
        this.gameState = GameState.values()[nbt.getInt("GameState")];
        this.gameTimer = nbt.getInt("GameTimer");
        this.lives = nbt.getInt("GameLives");
        this.progress = nbt.getInt("GameProgress");
        this.currentNumber = nbt.getInt("CurrentNumber");
    }

    // El ScreenHandler ahora preparará el minijuego en lugar de iniciarlo
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        prepareMinigame(player);
        return new NucleoScreenHandler(syncId, playerInventory, this);
    }

    // --- [MÉTODOS SIN CAMBIOS IMPORTANTES] ---
    // (spawnMobBatch, findSpawnPosition, etc., siguen aquí)
    private void spawnMobBatch(ServerWorld world) {
        int count = Math.min(this.mobsLeftToSpawn, MOBS_PER_BATCH);
        for (int i = 0; i < count; i++) {
            EntityType<? extends HostileEntity> mobType = MOB_TYPES.get(random.nextInt(MOB_TYPES.size()));
            BlockPos spawnPos = findSpawnPosition(world);
            if (spawnPos != null) {
                HostileEntity entity = mobType.create(world);
                if (entity != null) {
                    entity.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                    scaleAttribute(entity, EntityAttributes.GENERIC_MAX_HEALTH, this.currentHealthMultiplier);
                    scaleAttribute(entity, EntityAttributes.GENERIC_ATTACK_DAMAGE, this.currentDamageMultiplier);
                    entity.heal(entity.getMaxHealth());
                    setCoreDefenseAI(entity, pos);
                    world.spawnEntityAndPassengers(entity);
                }
            }
        }
        this.mobsLeftToSpawn -= count;
    }
    private static void sendWaveNotification(MinecraftServer server, BlockPos pos) {
        if (server == null) return;
        String coords = String.format("§e[X: %d, Y: %d, Z: %d]", pos.getX(), pos.getY(), pos.getZ());
        String title = "§c§l¡Oleada en camino!";
        String subtitle = "El núcleo en " + coords + " está siendo protegido.";
        server.getPlayerManager().broadcast(Text.literal(title + " " + subtitle), false);
        String fullMessage = title + "\n§r" + subtitle;
        ImmersiveMessage notification = ImmersiveMessage.popup(10.0f, fullMessage, "")
                .sound(SoundEffect.LOW).color(0xFF4444).style(style -> style.withBold(true))
                .background().backgroundColor(new ImmersiveColor(0, 0, 0, 180))
                .borderTopColor(new ImmersiveColor(0xFF4444).mixWith(ImmersiveColor.WHITE, 0.3f))
                .borderBottomColor(new ImmersiveColor(0xFF4444).mixWith(ImmersiveColor.BLACK, 0.5f))
                .anchor(TextAnchor.TOP_CENTER).y(40f).wrap(300).slideUp(0.5f).slideOutDown(0.5f)
                .fadeIn(0.5f).fadeOut(0.5f).typewriter(1.0f, true);
        notification.sendServerToAll(server);
    }
    private void setCoreDefenseAI(HostileEntity entity, BlockPos corePos) {
        GoalSelector targetSelector = ((MobEntityAccessor) entity).getTargetSelector();
        targetSelector.clear(goal -> true);
        targetSelector.add(1, new ActiveTargetGoal<>(entity, PlayerEntity.class, true,
                (target) -> target.getBlockPos().isWithinDistance(corePos, CORE_DEFENSE_RADIUS)
        ));
        targetSelector.add(2, new RevengeGoal(entity));
    }
    private void scaleAttribute(HostileEntity entity, net.minecraft.entity.attribute.EntityAttribute attribute, double multiplier) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance != null) {
            instance.setBaseValue(instance.getBaseValue() * multiplier);
        }
    }
    private BlockPos findSpawnPosition(ServerWorld world) {
        for (int i = 0; i < 20; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = MIN_SPAWN_RADIUS + random.nextDouble() * (SPAWN_RADIUS - MIN_SPAWN_RADIUS);
            int x = pos.getX() + (int)(radius * Math.cos(angle));
            int z = pos.getZ() + (int)(radius * Math.sin(angle));
            BlockPos groundPos = world.getTopPosition(Heightmap.Type.WORLD_SURFACE, new BlockPos(x, 0, z));
            if (isSafeSpawnPosition(world, groundPos)) {
                return groundPos;
            }
        }
        return null;
    }
    private boolean isSafeSpawnPosition(ServerWorld world, BlockPos pos) {
        if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
            return false;
        }
        return world.isAir(pos) && world.isAir(pos.up());
    }
    private void markDirtyAndSync() {
        if (world != null && !world.isClient) {
            markDirty();
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }
    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
    @Nullable
    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
    @Override
    public Text getDisplayName() {
        return Text.literal("Desactivación del Núcleo");
    }
    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }
}