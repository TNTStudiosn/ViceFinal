package com.TNTStudios.vicefinal.mixin;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Este mixin me permite acceder a campos y métodos privados o protegidos
 * de la clase MobEntity de una manera segura y compatible.
 * En este caso, lo necesito para obtener el selector de objetivos (targetSelector).
 */
@Mixin(MobEntity.class)
public interface MobEntityAccessor {

    /**
     * Con esta anotación, Fabric genera automáticamente el código necesario
     * para devolver el valor del campo 'targetSelector'.
     * @return El GoalSelector de los objetivos de la entidad.
     */
    @Accessor("targetSelector")
    GoalSelector getTargetSelector();
}