package com.TNTStudios.vicefinal.blocks;

import com.TNTStudios.vicefinal.mixin.MobEntityAccessor;
import com.TNTStudios.vicefinal.registry.ModBlockEntities;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import toni.immersivemessages.api.ImmersiveMessage;
import toni.immersivemessages.api.SoundEffect;
import toni.immersivemessages.api.TextAnchor;
import toni.immersivemessages.util.ImmersiveColor;

import java.util.List;
import java.util.Random;

public class NucleoBlockEntity extends BlockEntity {

    // --- Contadores y Estado ---
    private int waveTimer = 20; // Timer para la primera oleada (1 segundo).
    private int mobsLeftToSpawn = 0; // Mobs que faltan por generar en la oleada actual.
    private int spawnTickDelay = 0; // Temporizador para espaciar la generación de mobs.

    // --- Parámetros de la mecánica ---
    private static final int SPAWN_RADIUS = 20;
    private static final int MIN_SPAWN_RADIUS = 8;
    private static final int CORE_DEFENSE_RADIUS = 30;

    // --- Parámetros de escalado de dificultad ---
    private static final int BASE_MOBS_PER_WAVE = 8;
    private static final int MOBS_PER_ADDITIONAL_PLAYER = 3;
    private static final double BASE_HEALTH_MULTIPLIER = 1.5;
    private static final double HEALTH_INCREASE_PER_PLAYER = 0.1;
    private static final double BASE_DAMAGE_MULTIPLIER = 1.8;
    private static final double DAMAGE_INCREASE_PER_PLAYER = 0.05;

    // --- NUEVOS Parámetros de Optimización ---
    private static final int MOBS_PER_BATCH = 5; // Genero mobs en lotes de 5 para no sobrecargar el tick.
    private static final int SPAWN_DELAY_TICKS = 4; // Espero 4 ticks (0.2s) entre cada lote.

    // Variables para almacenar los multiplicadores de la oleada actual.
    private double currentHealthMultiplier = 1.0;
    private double currentDamageMultiplier = 1.0;

    private static final List<EntityType<? extends HostileEntity>> MOB_TYPES = List.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER,
            EntityType.HUSK, EntityType.STRAY, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER
    );

    private final Random random = new Random();

    public NucleoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUCLEO_BLOCK_ENTITY, pos, state);
    }

    /**
     * Lógica principal que se ejecuta en cada tick del servidor.
     * Ahora gestiona tanto el inicio de la oleada como la generación escalonada.
     */
    public static void tick(World world, BlockPos pos, BlockState state, NucleoBlockEntity be) {
        if (world.isClient) {
            return;
        }

        // --- 1. Proceso de Generación Escalonada ---
        // Si quedan mobs por generar, me encargo de ellos primero.
        if (be.mobsLeftToSpawn > 0) {
            be.spawnTickDelay--;
            if (be.spawnTickDelay <= 0) {
                be.spawnMobBatch((ServerWorld) world);
                be.spawnTickDelay = SPAWN_DELAY_TICKS; // Reseteo el contador para el siguiente lote.
            }
            return; // No continúo con el timer de la oleada principal.
        }

        // --- 2. Contador para la Siguiente Oleada ---
        // Si no hay una oleada activa, simplemente descuento el tiempo.
        if (be.waveTimer > 0) {
            be.waveTimer--;
            return;
        }

        // --- 3. Iniciar una Nueva Oleada ---
        // El contador llegó a cero, así que inicio una nueva oleada.
        be.waveTimer = 500 + be.random.nextInt(400); // Reseteo para la *próxima* oleada.
        be.startWave((ServerWorld) world);
    }

    /**
     * Prepara una nueva oleada calculando la cantidad de mobs y sus estadísticas,
     * pero NO los genera todavía. Solo inicializa los contadores.
     */
    private void startWave(ServerWorld world) {
        // La notificación se envía al inicio, eso está perfecto.
        sendWaveNotification(world.getServer(), pos);

        int playerCount = world.getServer().getPlayerManager().getPlayerList().size();
        if (playerCount == 0) return;

        // Calculo y almaceno los valores para la oleada que va a comenzar.
        this.mobsLeftToSpawn = BASE_MOBS_PER_WAVE + (playerCount > 1 ? (playerCount - 1) * MOBS_PER_ADDITIONAL_PLAYER : 0);
        this.currentHealthMultiplier = BASE_HEALTH_MULTIPLIER + (playerCount > 1 ? (playerCount - 1) * HEALTH_INCREASE_PER_PLAYER : 0);
        this.currentDamageMultiplier = BASE_DAMAGE_MULTIPLIER + (playerCount > 1 ? (playerCount - 1) * DAMAGE_INCREASE_PER_PLAYER : 0);

        // Inicio el contador de generación inmediatamente para el primer lote.
        this.spawnTickDelay = 0;
    }

    /**
     * Genera un pequeño lote de mobs. Este método se llama repetidamente
     * hasta que todos los mobs de la oleada han sido generados.
     */
    private void spawnMobBatch(ServerWorld world) {
        // Determino cuántos mobs generar en este tick, sin pasarme del total.
        int count = Math.min(this.mobsLeftToSpawn, MOBS_PER_BATCH);

        for (int i = 0; i < count; i++) {
            EntityType<? extends HostileEntity> mobType = MOB_TYPES.get(random.nextInt(MOB_TYPES.size()));
            BlockPos spawnPos = findSpawnPosition(world);

            if (spawnPos != null) {
                HostileEntity entity = mobType.create(world);
                if (entity != null) {
                    entity.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                            random.nextFloat() * 360.0F, 0.0F);

                    // Aplico los multiplicadores que calculé al inicio de la oleada.
                    scaleAttribute(entity, EntityAttributes.GENERIC_MAX_HEALTH, this.currentHealthMultiplier);
                    scaleAttribute(entity, EntityAttributes.GENERIC_ATTACK_DAMAGE, this.currentDamageMultiplier);
                    entity.heal(entity.getMaxHealth());

                    setCoreDefenseAI(entity, pos);
                    world.spawnEntityAndPassengers(entity);
                }
            }
        }

        // Descuento los mobs que acabo de generar del total pendiente.
        this.mobsLeftToSpawn -= count;
    }

    // --- Métodos de Utilidad (Sin Cambios) ---

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
}