package com.TNTStudios.vicefinal.blocks;

import com.TNTStudios.vicefinal.registry.ModBlockEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
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
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    // Le indico a Minecraft que este BlockEntity tiene una lógica que debe ejecutarse.
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        // La lógica de spawn solo debe correr en el servidor para evitar problemas.
        if (world.isClient()) {
            return null;
        }

        // Devuelvo una referencia a mi método tick, asegurándome de que los tipos coincidan.
        // El '::' es una referencia a un método, una forma moderna y eficiente en Java
        // de apuntar a una función sin tener que crear una clase anónima.
        return checkType(type, ModBlockEntities.NUCLEO_BLOCK_ENTITY, NucleoBlockEntity::tick);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    private static final VoxelShape SHAPE = Stream.of(
            Block.createCuboidShape(5, 5, 5, 11, 11, 11),
            Block.createCuboidShape(11, 6, 6, 11.5, 10, 10),
            Block.createCuboidShape(6, 6, 11, 10, 10, 11.5),
            Block.createCuboidShape(6, 6, 4.5, 10, 10, 5),
            Block.createCuboidShape(4.5, 6, 6, 5, 10, 10),
            Block.createCuboidShape(6, 4.5, 6, 10, 5, 10),
            Block.createCuboidShape(6, 11, 6, 10, 11.5, 10)
    ).reduce(VoxelShapes::union).get();
}