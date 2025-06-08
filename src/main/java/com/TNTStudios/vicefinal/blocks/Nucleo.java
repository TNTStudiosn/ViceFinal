package com.TNTStudios.vicefinal.blocks;

import com.TNTStudios.vicefinal.registry.ModBlockEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class Nucleo extends BlockWithEntity {

    public Nucleo(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new NucleoBlockEntity(pos, state);
    }


    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    // Definimos la forma compuesta
    private static final VoxelShape SHAPE = Stream.of(
            // Cubo 1
            Block.createCuboidShape(5, 5, 5, 11, 11, 11),
            // Cubo 2
            Block.createCuboidShape(11, 6, 6, 11.5, 10, 10),
            // Cubo 3
            Block.createCuboidShape(6, 6, 11, 10, 10, 11.5),
            // Cubo 4
            Block.createCuboidShape(6, 6, 4.5, 10, 10, 5),
            // Cubo 5
            Block.createCuboidShape(4.5, 6, 6, 5, 10, 10),
            // Cubo 6
            Block.createCuboidShape(6, 4.5, 6, 10, 5, 10),
            // Cubo 7
            Block.createCuboidShape(6, 11, 6, 10, 11.5, 10)
    ).reduce(VoxelShapes::union).get();
}
