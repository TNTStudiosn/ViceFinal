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
import net.minecraft.entity.attribute.EntityAttribute;
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

// Implemento ExtendedScreenHandlerFactory para poder pasar datos extra (la posición del bloque) al cliente.
public class NucleoBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {

    // --- Variables de la Mecánica ---
    private final Random random = new Random();

    // --- Estado de Oleadas ---
    private int waveTimer = 20; // Ticks para la primera oleada.
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

    // --- Parámetros de la mecánica ---
    private static final int REQUIRED_PROGRESS = 15;
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

    // --- Constructor ---
    public NucleoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUCLEO_BLOCK_ENTITY, pos, state);
    }

    // --- Lógica de Ticks (Servidor) ---
    public static void tick(World world, BlockPos pos, BlockState state, NucleoBlockEntity be) {
        if (world.isClient) {
            return;
        }

        // Si el jugador se aleja, se muere o se desconecta, detengo el minijuego.
        if (be.isGameActive()) {
            PlayerEntity player = world.getPlayerByUuid(be.interactingPlayer);
            if (player == null || player.isDead() || player.getPos().distanceTo(pos.toCenterPos()) > 8.0) {
                be.failMinigame();
            }
        }

        // 1. Proceso de generación escalonada.
        if (be.mobsLeftToSpawn > 0) {
            be.spawnTickDelay--;
            if (be.spawnTickDelay <= 0) {
                be.spawnMobBatch((ServerWorld) world);
                be.spawnTickDelay = SPAWN_DELAY_TICKS;
            }
            return;
        }

        // 2. Contador para la siguiente oleada.
        if (be.waveTimer > 0) {
            be.waveTimer--;
            return;
        }

        // 3. Iniciar una nueva oleada.
        be.waveTimer = 500 + be.random.nextInt(400); // Reseteo para la próxima.
        be.startWave((ServerWorld) world);
    }

    // --- Lógica del Minijuego ---
    public boolean isGameActive() {
        return this.interactingPlayer != null;
    }

    public boolean isPlayerInteracting(PlayerEntity player) {
        return player.getUuid().equals(this.interactingPlayer);
    }

    public boolean isOnCooldown() {
        return world != null && world.getTime() < this.cooldownEndTime;
    }

    public long getCooldownSeconds() {
        if (!isOnCooldown() || world == null) return 0;
        return (cooldownEndTime - world.getTime()) / 20;
    }

    public void startMinigame(PlayerEntity player) {
        if (world == null || world.isClient) return;

        this.interactingPlayer = player.getUuid();
        this.lives = 2;
        this.progress = 0;
        generateNextNumber();
        markDirtyAndSync();
    }

    public void stopMinigame() {
        this.interactingPlayer = null;
        this.currentNumber = -1;
        markDirtyAndSync();
    }

    public void failMinigame() {
        if (world == null || world.isClient()) return;

        this.cooldownEndTime = world.getTime() + (30 * 20); // 30 segundos de cooldown.

        // Busco al jugador para notificarle y cerrar su pantalla.
        PlayerEntity player = world.getPlayerByUuid(this.interactingPlayer);
        if (player instanceof ServerPlayerEntity serverPlayer) { // CORRECCIÓN: Usar ServerPlayerEntity.
            serverPlayer.sendMessage(Text.literal("Has fallado. El núcleo entra en enfriamiento.").formatted(Formatting.RED), false);
            serverPlayer.closeHandledScreen(); // CORRECCIÓN: Llamar al método desde la instancia correcta.
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.5f, 1.0f);
        }

        stopMinigame();
    }

    public void winMinigame() {
        if (world == null || world.isClient() || world.getServer() == null) return;

        // Busco al jugador para cerrar su pantalla antes de destruir todo.
        PlayerEntity player = world.getPlayerByUuid(this.interactingPlayer);
        if (player instanceof ServerPlayerEntity serverPlayer) { // CORRECCIÓN: Usar ServerPlayerEntity.
            serverPlayer.closeHandledScreen(); // CORRECCIÓN: Llamar al método desde la instancia correcta.
        }

        // Destruyo el bloque y creo una explosión.
        world.breakBlock(getPos(), false);
        world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.0f, World.ExplosionSourceType.BLOCK);

        // Envío un mensaje global.
        world.getServer().getPlayerManager().broadcast(Text.literal("¡El núcleo ha sido destruido!").formatted(Formatting.GREEN, Formatting.BOLD), false);

        stopMinigame();
    }

    public void handlePlayerInput(int number) {
        if (!isGameActive() || world == null) return;

        if (number != this.currentNumber) {
            this.lives--;
            if (this.lives <= 0) {
                failMinigame();
            } else {
                generateNextNumber();
                world.playSound(null, getPos(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 1.0f, 1.0f);
            }
        } else {
            this.progress++;
            world.playSound(null, getPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 0.5f, 1.5f);
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
    }

    // --- Lógica de Oleadas ---
    private void startWave(ServerWorld world) {
        sendWaveNotification(world.getServer(), pos);
        int playerCount = world.getServer().getPlayerManager().getPlayerList().size();
        if (playerCount == 0) return;

        this.mobsLeftToSpawn = BASE_MOBS_PER_WAVE + (playerCount > 1 ? (playerCount - 1) * MOBS_PER_ADDITIONAL_PLAYER : 0);
        this.currentHealthMultiplier = BASE_HEALTH_MULTIPLIER + (playerCount > 1 ? (playerCount - 1) * HEALTH_INCREASE_PER_PLAYER : 0);
        this.currentDamageMultiplier = BASE_DAMAGE_MULTIPLIER + (playerCount > 1 ? (playerCount - 1) * DAMAGE_INCREASE_PER_PLAYER : 0);
        this.spawnTickDelay = 0; // Inicia la generación en el siguiente tick.
    }

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

    // --- Métodos de Utilidad ---
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

    // --- Sincronización y NBT ---
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
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("CooldownEndTime", this.cooldownEndTime);
        if (this.interactingPlayer != null) {
            nbt.putUuid("InteractingPlayer", this.interactingPlayer);
        }
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
        this.lives = nbt.getInt("GameLives");
        this.progress = nbt.getInt("GameProgress");
        this.currentNumber = nbt.getInt("CurrentNumber");
    }

    // --- Implementación de ExtendedScreenHandlerFactory ---
    @Override
    public Text getDisplayName() {
        return Text.literal("Desactivación del Núcleo");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        // Al abrir la GUI, inicio el minijuego con el jugador que la abrió.
        startMinigame(player);
        // Le paso la referencia de este BlockEntity al ScreenHandler.
        return new NucleoScreenHandler(syncId, playerInventory, this);
    }

    // Este método es la clave: escribe la posición del bloque en el paquete que se envía al cliente.
    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }
}