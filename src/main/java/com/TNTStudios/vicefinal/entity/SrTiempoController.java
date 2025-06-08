// RUTA: src/main/java/com/TNTStudios/vicefinal/entity/SrTiempoController.java
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

    /**
     * [CORREGIDO] La animación de 'slap' ahora vuelve a un estado de loop de forma correcta.
     */
    public void playSlap() {
        // Determino el nombre de la siguiente animación en bucle.
        String nextAnimationName = this.isAggressive
                ? "animation.srtiempo.channel"
                : "animation.srtiempo.idle";

        // Primero reproduzco 'slap' una vez, y luego entro en bucle con la siguiente animación.
        this.currentAnimation = RawAnimation.begin().thenPlay("animation.srtiempo.slap").thenLoop(nextAnimationName);
    }

    /**
     * [CORREGIDO] La animación 'turn_time' ahora vuelve a un estado de loop de forma correcta.
     */
    public void playTurnTime() {
        // Determino el nombre de la siguiente animación en bucle.
        String nextAnimationName = this.isAggressive
                ? "animation.srtiempo.channel"
                : "animation.srtiempo.idle";

        // Primero reproduzco 'turn_time' una vez, y luego entro en bucle con la siguiente animación.
        this.currentAnimation = RawAnimation.begin().thenPlay("animation.srtiempo.turn_time").thenLoop(nextAnimationName);
    }

    public void playChannel() {
        this.currentAnimation = RawAnimation.begin().thenLoop("animation.srtiempo.channel");
    }

    /**
     * [CORREGIDO] La animación 'charge_up' ahora vuelve a un estado de loop de forma correcta.
     */
    public void playChargeUp() {
        // Determino el nombre de la siguiente animación en bucle.
        String nextAnimationName = this.isAggressive
                ? "animation.srtiempo.channel"
                : "animation.srtiempo.idle";

        // Primero reproduzco 'charge_up' una vez, y luego entro en bucle con la siguiente animación.
        this.currentAnimation = RawAnimation.begin().thenPlay("animation.srtiempo.charge_up").thenLoop(nextAnimationName);
    }

    /**
     * [MEJORADO] La animación de muerte no loopea, se queda en el último frame.
     */
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