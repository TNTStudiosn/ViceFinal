package com.TNTStudios.vicefinal.entity;

import software.bernie.geckolib.core.animation.RawAnimation;

/**
 * Controlador para SrTiempoEntity.
 * Desde aquí el mod puede controlar la lógica del boss.
 */
public class SrTiempoController {

    private RawAnimation currentAnimation;
    private boolean isWalking = false;
    private boolean isAggressive = false;

    public SrTiempoController() {
        // Por defecto, comienza en modo 'idle' (quieto), no 'statue'.
        // Así, 'statue' se convierte en un estado que activamos a propósito.
        this.playIdle();
    }

    // API pública:

    public void playIdle() {
        this.currentAnimation = RawAnimation.begin().thenLoop("animation.srtiempo.idle");
    }

    public void playWalk() {
        this.currentAnimation = RawAnimation.begin().thenLoop("animation.srtiempo.walk");
    }

    public void playSlap() {
        this.currentAnimation = RawAnimation.begin().thenPlay("animation.srtiempo.slap");
    }

    public void playTurnTime() {
        this.currentAnimation = RawAnimation.begin().thenPlay("animation.srtiempo.turn_time");
    }

    public void playChannel() {
        this.currentAnimation = RawAnimation.begin().thenLoop("animation.srtiempo.channel");
    }

    public void playChargeUp() {
        this.currentAnimation = RawAnimation.begin().thenPlay("animation.srtiempo.charge_up");
    }

    public void playDeath() {
        this.currentAnimation = RawAnimation.begin().thenPlay("animation.srtiempo.death");
    }

    public void playStatue() {
        this.currentAnimation = RawAnimation.begin().thenLoop("animation.srtiempo.statue");
    }

    public RawAnimation getCurrentAnimation() {
        return currentAnimation;
    }

    // Este método ahora determina si la IA de movimiento (caminar o atacar) debe activarse.
    public boolean canMove() {
        return this.isWalking || this.isAggressive;
    }

    // Este método determina si la IA de ataque específico (MeleeAttackGoal) debe activarse.
    public boolean isAggressive() {
        return this.isAggressive;
    }

    // Este método determina si la IA de paseo (WanderAroundFarGoal) debe activarse.
    public boolean isWalking() {
        return this.isWalking;
    }

    // Setters para controlar el estado desde los comandos.
    public void setWalking(boolean walking) {
        this.isWalking = walking;
    }

    public void setAggressive(boolean aggressive) {
        this.isAggressive = aggressive;
    }
}