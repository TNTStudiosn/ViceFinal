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
import net.minecraft.entity.damage.DamageTypes;
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

    // Añado un estado para controlar si el jefe puede recibir daño.
    private boolean isVulnerable = false;

    @Nullable
    private final ServerBossBar bossBar;

    public SrTiempoEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);

        if (!world.isClient()) {
            this.bossBar = new ServerBossBar(this.getDisplayName(), BossBar.Color.PURPLE, BossBar.Style.PROGRESS);
        } else {
            this.bossBar = null;
        }

        this.setHealth(this.getMaxHealth());
        // Por defecto, el jefe es invulnerable hasta que un comando diga lo contrario.
        this.setInvulnerable(true);
        this.setNoGravity(false);
    }

    public SrTiempoController getController() {
        return controller;
    }

    // --- GESTIÓN DE PERSISTENCIA Y DATOS ---

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("isWalking", controller.isWalking());
        nbt.putBoolean("isAggressive", controller.isAggressive());
        // Guardo el estado de vulnerabilidad para que persista.
        nbt.putBoolean("isVulnerable", this.isVulnerable);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        controller.setWalking(nbt.getBoolean("isWalking"));
        controller.setAggressive(nbt.getBoolean("isAggressive"));
        this.setVulnerable(nbt.getBoolean("isVulnerable")); // Restauro el estado.

        // Restauro la animación correcta al cargar el mundo.
        if (this.isDead()) {
            controller.playDeath();
        } else if (controller.isWalking()) {
            controller.playWalk();
        } else if (controller.isAggressive()) {
            controller.playChannel();
        } else {
            controller.playIdle();
        }
    }

    // --- LÓGICA DE DAÑO Y VULNERABILIDAD ---

    public void setVulnerable(boolean vulnerable) {
        this.isVulnerable = vulnerable;
        // La invulnerabilidad de Minecraft es un extra, mi lógica principal se basa en isVulnerable.
        this.setInvulnerable(!vulnerable);
    }

    public boolean isVulnerable() {
        return this.isVulnerable;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Si mi lógica dice que soy invulnerable o ya estoy muerto, no recibo daño.
        if (!this.isVulnerable || this.isDead()) {
            return false;
        }
        // Si el daño es de tipo /kill o similar, muero sin más.
        if (source.isOf(DamageTypes.OUT_OF_WORLD)) {
            this.kill();
            return true;
        }

        // Delego el daño a la clase padre y actualizo la barra de vida.
        boolean damaged = super.damage(source, amount);
        if (damaged && this.bossBar != null) {
            this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        }

        // Si mi vida llega a 0, inicio mi secuencia de muerte.
        if (this.getHealth() <= 0 && !getWorld().isClient) {
            this.onDeath(source);
        }

        return damaged;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);

        // Inhabilito el movimiento y la percepción para que la entidad se quede quieta.
        this.getNavigation().stop();
        this.setHealth(0.0F); // Aseguro que la vida sea 0.
        this.setInvulnerable(true); // Me vuelvo invulnerable para no recibir más daño durante la animación.

        if (this.bossBar != null) {
            this.bossBar.setPercent(0);
        }

    }

    @Override
    public void kill() {
        this.onDeath(getDamageSources().genericKill());
    }

    @Override
    public boolean tryAttack(Entity target) {
        // El ataque melee ahora solo funciona si estoy en modo agresivo.
        if (!controller.isAggressive()) {
            return false;
        }
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

    // --- LÓGICA DE ANIMACIÓN GECKOLIB (CORREGIDA) ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <E extends GeoEntity> PlayState predicate(AnimationState<E> state) {
        // Si estoy muerto, la animación de muerte tiene prioridad absoluta.
        if (this.isDead()) {
            return state.setAndContinue(RawAnimation.begin().thenPlay("animation.srtiempo.death"));
        }

        // Prioridad 1: Si la entidad se está moviendo (según el motor del juego) Y mi lógica permite caminar,
        // fuerzo la animación de caminar. Esto resuelve el "caminar en idle".
        if (state.isMoving() && this.controller.isWalking()) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.srtiempo.walk"));
        }

        // Prioridad 2: Si no camino, simplemente reproduzco la animación que mi controlador indica.
        // Esto mantiene la coherencia entre los comandos y lo que se ve en el juego.
        return state.setAndContinue(this.controller.getCurrentAnimation());
    }

    // El resto de la clase (tick, onRemoved, etc.) permanece igual.
    // ... (resto del código sin cambios)
    @Override
    public boolean isPersistent() { return true; }
    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) { return false; }
    @Override
    public boolean isPushable() { return false; }
    @Override
    public boolean isPushedByFluids() { return false; }
    @Override
    public void onPlayerCollision(PlayerEntity player) { }
    @Override
    protected void initGoals() {
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0D, false) {
            @Override
            public boolean canStart() {
                return !isDead() && controller.isAggressive() && super.canStart();
            }
        });
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0D) {
            @Override
            public boolean canStart() {
                return !isDead() && controller.isWalking() && super.canStart();
            }
        });
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F) {
            @Override
            public boolean canStart() {
                return !isDead() && controller.canMove() && super.canStart();
            }
        });
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true) {
            @Override
            public boolean canStart() {
                return !isDead() && controller.isAggressive() && super.canStart();
            }
        });
    }
    @Override
    public void setCustomName(Text name) {
        super.setCustomName(name);
        if (this.bossBar != null) {
            this.bossBar.setName(this.getDisplayName());
        }
    }
    @Override
    public void tick() {
        super.tick();
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
        if (this.bossBar != null) {
            this.bossBar.setVisible(false);
            this.bossBar.clearPlayers();
        }
    }
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}