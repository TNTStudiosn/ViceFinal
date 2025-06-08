// RUTA: src/main/java/com/TNTStudios/vicefinal/entity/SrTiempoEntity.java
package com.TNTStudios.vicefinal.entity;

import net.minecraft.entity.Entity;
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
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.ArrayList;
import java.util.List;

public class SrTiempoEntity extends HostileEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final SrTiempoController controller = new SrTiempoController();

    // [CORREGIDO] La BossBar solo debe existir en el servidor.
    // La declaro como @Nullable para indicar que puede ser nula (en el lado del cliente).
    @Nullable
    private final ServerBossBar bossBar;

    public SrTiempoEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);

        // [CORREGIDO] Solo instancio la ServerBossBar en el lado del servidor.
        if (!world.isClient()) {
            this.bossBar = new ServerBossBar(this.getDisplayName(), BossBar.Color.PURPLE, BossBar.Style.PROGRESS);
        } else {
            this.bossBar = null;
        }

        this.setHealth(this.getMaxHealth());
        this.setInvulnerable(true);
        this.setNoGravity(false);
    }

    // El resto de la clase permanece mayormente igual, pero con comprobaciones de nulidad.

    public SrTiempoController getController() {
        return controller;
    }

    // --- GESTIÓN DE PERSISTENCIA Y DATOS ---

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("isWalking", controller.isWalking());
        nbt.putBoolean("isAggressive", controller.isAggressive());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        controller.setWalking(nbt.getBoolean("isWalking"));
        controller.setAggressive(nbt.getBoolean("isAggressive"));

        if (controller.isWalking()) {
            controller.playWalk();
        } else if (controller.isAggressive()) {
            controller.playChannel();
        } else {
            controller.playIdle();
        }
    }

    // --- COMPORTAMIENTO Y FÍSICA ---

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        // No hago nada, soy inamovible.
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean success = super.tryAttack(target);
        if (success) {
            this.controller.playSlap();
        }
        return success;
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
                return controller.isAggressive() && super.canStart();
            }
        });
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0D) {
            @Override
            public boolean canStart() {
                return controller.isWalking() && super.canStart();
            }
        });
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F) {
            @Override
            public boolean canStart() {
                return controller.canMove() && super.canStart();
            }
        });
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true) {
            @Override
            public boolean canStart() {
                return controller.isAggressive() && super.canStart();
            }
        });
    }

    // --- INMORTALIDAD Y BOSS BAR ---

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void setCustomName(Text name) {
        super.setCustomName(name);
        // [CORREGIDO] Añado la comprobación para evitar una NullPointerException en el cliente.
        if (this.bossBar != null) {
            this.bossBar.setName(this.getDisplayName());
        }
    }

    @Override
    public void tick() {
        super.tick();
        // [CORREGIDO] El bloque entero ya estaba protegido, pero ahora la existencia del bossBar también lo está.
        if (!getWorld().isClient() && this.bossBar != null) {
            this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());

            List<ServerPlayerEntity> playersInRange = ((ServerWorld)this.getWorld()).getPlayers(p -> p.squaredDistanceTo(this) < 4096);
            List<ServerPlayerEntity> currentBossBarPlayers = new ArrayList<>(this.bossBar.getPlayers());

            currentBossBarPlayers.stream()
                    .filter(player -> !playersInRange.contains(player))
                    .forEach(this.bossBar::removePlayer);

            playersInRange.stream()
                    .filter(player -> !this.bossBar.getPlayers().contains(player))
                    .forEach(this.bossBar::addPlayer);
        }
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        // [CORREGIDO] Me aseguro de limpiar la BossBar solo si existe (lado servidor).
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
        if (state.isMoving() && controller.isWalking()) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.srtiempo.walk"));
        }
        return state.setAndContinue(controller.getCurrentAnimation());
    }
}