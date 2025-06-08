// RUTA: src/main/java/com/TNTStudios/vicefinal/entity/SrTiempoController.java
package com.TNTStudios.vicefinal.entity;

import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.RawAnimation;

/**
 * Mi nuevo controlador lógico para el Sr. Tiempo.
 * Esta clase ya no se encarga de decidir qué animación reproducir en cada tick,
 * sino que mantiene el estado actual (ej: "está caminando", "está agresivo") y
 * gestiona las solicitudes de animaciones de un solo uso, como los ataques.
 */
public class SrTiempoController {

    // --- DEFINICIONES DE ANIMACIÓN (CONSTANTES) ---
    // Defino todas mis animaciones como constantes estáticas para evitar errores
    // de escritura y para tener un único lugar donde gestionarlas.

    // Animaciones en Bucle
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.srtiempo.idle");
    public static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.srtiempo.walk");
    public static final RawAnimation CHANNEL = RawAnimation.begin().thenLoop("animation.srtiempo.channel");
    public static final RawAnimation STATUE = RawAnimation.begin().thenLoop("animation.srtiempo.statue");

    // Animaciones de un Solo Uso (One-Shot)
    public static final RawAnimation SLAP = RawAnimation.begin().thenPlay("animation.srtiempo.slap");
    public static final RawAnimation CHARGE_UP = RawAnimation.begin().thenPlay("animation.srtiempo.charge_up");
    public static final RawAnimation TURN_TIME = RawAnimation.begin().thenPlay("animation.srtiempo.turn_time");
    public static final RawAnimation DEATH = RawAnimation.begin().thenPlay("animation.srtiempo.death");

    // --- ESTADO INTERNO ---
    private boolean isWalking = false;
    private boolean isAggressive = false;

    // La animación base es la que se reproduce en bucle por defecto.
    private RawAnimation baseAnimation = IDLE;

    // Aquí guardo la solicitud para una animación de un solo uso.
    @Nullable
    private RawAnimation requestedOneShot = null;

    // --- GETTERS Y SETTERS DE ESTADO ---
    public boolean isWalking() { return this.isWalking; }
    public boolean isAggressive() { return this.isAggressive; }
    public boolean canMove() { return this.isWalking && baseAnimation != STATUE; }
    public RawAnimation getBaseAnimation() { return this.baseAnimation; }

    /**
     * Devuelve la animación de un solo uso que se ha solicitado y la "consume" (pone a null)
     * para que no se vuelva a disparar en el siguiente tick.
     */
    @Nullable
    public RawAnimation consumeRequestedOneShot() {
        RawAnimation anim = this.requestedOneShot;
        this.requestedOneShot = null;
        return anim;
    }

    // --- MÉTODOS DE CONTROL (LLAMADOS DESDE COMANDOS O IA) ---

    // Comportamientos base
    public void setBehaviorIdle() {
        this.isWalking = false;
        this.isAggressive = false;
        this.baseAnimation = IDLE;
    }

    public void setBehaviorWalk() {
        this.isWalking = true;
        this.isAggressive = false;
        this.baseAnimation = WALK;
    }

    public void setBehaviorAttack() {
        this.isWalking = false;
        this.isAggressive = true;
        this.baseAnimation = CHANNEL;
    }

    public void setBehaviorStatue() {
        this.isWalking = false;
        this.isAggressive = false;
        this.baseAnimation = STATUE;
    }

    public void triggerChannel() {
        this.setBehaviorAttack();
    }


    // Disparadores de animaciones de ataque (one-shot)
    public void triggerSlap() { this.requestedOneShot = SLAP; }
    public void triggerChargeUp() { this.requestedOneShot = CHARGE_UP; }
    public void triggerTurnTime() { this.requestedOneShot = TURN_TIME; }

    // No necesito un trigger para la muerte, ya que la entidad la gestiona con isDead().
}