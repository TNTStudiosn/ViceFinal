package com.TNTStudios.vicefinal.blocks;

import com.TNTStudios.vicefinal.registry.ModBlockEntities;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class NucleoBlockEntity extends BlockEntity {

    public NucleoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUCLEO_BLOCK_ENTITY, pos, state);
    }
}
