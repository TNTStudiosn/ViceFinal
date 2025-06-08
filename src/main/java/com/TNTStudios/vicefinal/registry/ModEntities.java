package com.TNTStudios.vicefinal.registry;

import com.TNTStudios.vicefinal.entity.SrTiempoEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<SrTiempoEntity> SR_TIEMPO = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier("vicefinal", "sr_tiempo"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, SrTiempoEntity::new)
                    .dimensions(EntityDimensions.fixed(1.0f, 3.0f)) // tamaño del boss
                    .trackRangeBlocks(80)
                    .build()
    );

    public static void register() {
        FabricDefaultAttributeRegistry.register(SR_TIEMPO, SrTiempoEntity.createAttributes());
    }
}
