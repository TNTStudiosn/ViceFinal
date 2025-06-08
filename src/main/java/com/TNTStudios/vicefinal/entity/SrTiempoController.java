package com.TNTStudios.vicefinal.entity;

import software.bernie.geckolib.core.animation.RawAnimation;

/**
 * Controlador para SrTiempoEntity.
 * Desde aquí el mod puede controlar la lógica del boss.
 */
public class SrTiempoController {

    private RawAnimation currentAnimation;
    private boolean canMove;

    public SrTiempoController() {
        this.currentAnimation = RawAnimation.begin().thenLoop("animation.srtiempo.idle");
        this.canMove = false; // Inicialmente no puede moverse
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

    public boolean canMove() {
        return canMove;
    }

    public void setCanMove(boolean canMove) {
        this.canMove = canMove;
    }
}
