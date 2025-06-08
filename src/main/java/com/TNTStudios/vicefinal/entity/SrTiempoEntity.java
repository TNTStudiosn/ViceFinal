package com.TNTStudios.vicefinal.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;

public class SrTiempoEntity extends HostileEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    // Hago el controlador final, ya que siempre estará ahí.
    private final SrTiempoController controller = new SrTiempoController();
    // La BossBar también es final.
    private final ServerBossBar bossBar;

    public SrTiempoEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.bossBar = new ServerBossBar(this.getDisplayName(), BossBar.Color.PURPLE, BossBar.Style.PROGRESS);
        this.setHealth(this.getMaxHealth());

        // Lo hago invulnerable desde el principio. Es la forma más robusta.
        this.setInvulnerable(true);
        // Me aseguro de que la entidad no sea afectada por la gravedad si está en modo estatua, por ejemplo.
        this.setNoGravity(false); // Inicialmente tiene gravedad.
    }

    public SrTiempoController getController() {
        return controller;
    }

    // --- GESTIÓN DE PERSISTENCIA Y DATOS ---

    @Override
    public boolean isPersistent() {
        // Soy una entidad de jefe, no debo desaparecer bajo ninguna circunstancia.
        // Esto previene el despawn por mob-cap y asegura que se guarde con el chunk.
        return true;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        // Doble confirmación de que no debo desaparecer. isPersistent() es más fuerte.
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        // Guardo el estado del controlador para que sobreviva a reinicios.
        nbt.putBoolean("isWalking", controller.isWalking());
        nbt.putBoolean("isAggressive", controller.isAggressive());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        // Leo el estado del controlador al cargar la entidad.
        controller.setWalking(nbt.getBoolean("isWalking"));
        controller.setAggressive(nbt.getBoolean("isAggressive"));

        // Sincronizo la animación correcta después de cargar el estado.
        // Por ejemplo, si estaba caminando, que siga caminando.
        if (controller.isWalking()) {
            controller.playWalk();
        } else if (controller.isAggressive()) {
            controller.playChannel();
        } else {
            // Podrías tener un estado 'estatua' persistente también.
            // Por ahora, si no es ninguno, lo dejo en idle.
            controller.playIdle();
        }
    }

    // --- COMPORTAMIENTO Y FÍSICA ---

    @Override
    public boolean isPushable() {
        // Soy una fuerza inamovible.
        return false;
    }

    @Override
    public boolean isPushedByFluids() {
        // Los fluidos no me afectan.
        return false;
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        // No me muevo si un jugador choca conmigo. Soy imponente.
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 1000.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0D, false) {
            @Override
            public boolean canStart() {
                // Solo puedo empezar a atacar si el controller me da permiso.
                return controller.isAggressive() && super.canStart();
            }
        });

        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0D) {
            @Override
            public boolean canStart() {
                // Solo puedo empezar a caminar si el controller me da permiso.
                return controller.isWalking() && super.canStart();
            }
        });

        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F) {
            @Override
            public boolean canStart() {
                // Miro a los jugadores si estoy en modo caminar o atacar.
                return controller.canMove() && super.canStart();
            }
        });

        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true) {
            @Override
            public boolean canStart() {
                // Solo busco objetivos si mi controller me dice que sea agresivo.
                return controller.isAggressive() && super.canStart();
            }
        });
    }

    // --- INMORTALIDAD Y BOSS BAR ---

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Mi poder está más allá de tu comprensión. No puedes herirme.
        return false;
    }

    @Override
    public void setCustomName(Text name) {
        super.setCustomName(name);
        this.bossBar.setName(this.getDisplayName());
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient()) {
            this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());

            // La gestión de la BossBar estaba bien, la mantengo.
            // Uso getPlayers en lugar de getEntitiesByClass para ser más directo.
            List<ServerPlayerEntity> playersInRange = ((ServerWorld)this.getWorld()).getPlayers(p -> p.squaredDistanceTo(this) < 4096); // 64*64 bloques

            // Jugadores que se alejan.
            this.bossBar.getPlayers().stream()
                    .filter(player -> !playersInRange.contains(player))
                    .forEach(this.bossBar::removePlayer);

            // Jugadores que se acercan.
            playersInRange.stream()
                    .filter(player -> !this.bossBar.getPlayers().contains(player))
                    .forEach(this.bossBar::addPlayer);
        }
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        // Me aseguro de que la barra se limpie correctamente.
        if (this.bossBar != null) {
            this.bossBar.setVisible(false);
            this.bossBar.clearPlayers();
        }
    }

    // --- LÓGICA DE ANIMACIÓN GECKOLIB ---

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <E extends GeoEntity> PlayState predicate(AnimationState<E> state) {
        // La lógica de animación depende del estado del controlador.
        // Si el controlador tiene la animación correcta, esto funcionará a la perfección.
        state.setAnimation(controller.getCurrentAnimation());
        return PlayState.CONTINUE;
    }
}