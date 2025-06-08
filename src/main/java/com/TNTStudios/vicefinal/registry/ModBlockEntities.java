package com.TNTStudios.vicefinal.registry;

import com.TNTStudios.vicefinal.blocks.NucleoBlockEntity;
import com.TNTStudios.vicefinal.blocks.VicefinalBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static BlockEntityType<NucleoBlockEntity> NUCLEO_BLOCK_ENTITY;

    public static void register() {
        NUCLEO_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                new Identifier("vicefinal", "nucleo_block_entity"),
                FabricBlockEntityTypeBuilder.create(NucleoBlockEntity::new, VicefinalBlocks.NUCLEO_BLOCK).build());
    }
}
