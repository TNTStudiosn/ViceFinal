package com.TNTStudios.vicefinal.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class SrTiempoEntity extends HostileEntity implements GeoEntity {

    // El caché para las animaciones de GeckoLib, todo en orden.
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    // El constructor ahora usa el tipo correcto, HostileEntity.
    public SrTiempoEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    // Para los atributos, ahora parto de la base de un mob hostil.
    // Así me aseguro de tener todos los atributos necesarios desde el principio.
    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0) // Vida de boss
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 15.0) // Daño
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0) // Que no lo muevan fácil
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0); // Rango de seguimiento
    }

    // Ahora sí puedo sobreescribir initGoals porque HostileEntity lo hereda de MobEntity.
    // Aquí registraré la IA básica de mi entidad.
    @Override
    protected void initGoals() {
        // Le doy un comportamiento de ataque cuerpo a cuerpo.
        this.goalSelector.add(0, new MeleeAttackGoal(this, 1.0D, false));
        // Haré que deambule por el mundo cuando esté inactivo.
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0D));
        // Y que me mire si estoy cerca.
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
    }

    // --- Lógica de Animación de GeckoLib ---

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // Aquí registro los controladores de animación.
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    // Este predicado controla qué animación se reproduce.
    // Por ahora, solo la de idle.
    private <E extends GeoEntity> PlayState predicate(AnimationState<E> state) {
        // Si la entidad se está moviendo, podría reproducir una animación de caminar.
        if (state.isMoving()) {
            // state.setAnimation(RawAnimation.begin().thenLoop("animation.srtiempo.walk"));
            // return PlayState.CONTINUE;
        }

        // Si no, reproduzco la animación de estar quieto (idle).
        state.setAnimation(RawAnimation.begin().thenLoop("animation.srtiempo.idle"));
        return PlayState.CONTINUE;
    }
}