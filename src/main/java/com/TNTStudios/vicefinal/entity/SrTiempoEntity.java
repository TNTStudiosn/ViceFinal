package com.TNTStudios.vicefinal.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.controller.AnimationControllerState;

public class SrTiempoEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public SrTiempoEntity(EntityType<? extends SrTiempoEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0) // Vida de boss
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 15.0) // Daño
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0) // Que no lo muevan fácil
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void initGoals() {
        // Aquí puedes agregar inteligencia con SmartBrain o Goals básicos
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected void updateLimbs(float animationProgress) {
        super.updateLimbs(animationProgress);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T> AnimationControllerState predicate(AnimationController<T> controller) {
        controller.setAnimation(RawAnimation.begin().thenLoop("animation.srtiempo.idle"));
        return AnimationControllerState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
