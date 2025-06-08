package com.TNTStudios.vicefinal.blocks;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import java.util.stream.Stream;

public class Nucleo extends Block {

    public Nucleo(AbstractBlock.Settings settings) {
        super(settings);
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
    ).reduce((v1, v2) -> VoxelShapes.union(v1, v2)).get();
}
