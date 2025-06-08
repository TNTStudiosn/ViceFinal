package com.TNTStudios.vicefinal.blocks;

import com.TNTStudios.vicefinal.registry.ModBlockEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
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
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS; // Del lado cliente, siempre indico éxito para que se muestre la animación de la mano.
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof NucleoBlockEntity nucleoEntity) {
            // Compruebo si el núcleo está en cooldown.
            if (nucleoEntity.isOnCooldown()) {
                player.sendMessage(Text.literal("El núcleo está en enfriamiento. Espera " + nucleoEntity.getCooldownSeconds() + " segundos.").formatted(Formatting.RED), true);
                return ActionResult.CONSUME;
            }
            // Compruebo si otro jugador ya está interactuando.
            if (nucleoEntity.isGameActive() && !nucleoEntity.isPlayerInteracting(player)) {
                player.sendMessage(Text.literal("Alguien más ya está desactivando el núcleo.").formatted(Formatting.YELLOW), true);
                return ActionResult.CONSUME;
            }

            // Si todo está en orden, abro la pantalla.
            player.openHandledScreen((NamedScreenHandlerFactory) be);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }


    // El resto de la clase permanece igual...
    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new NucleoBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient()) {
            return null;
        }
        return checkType(type, ModBlockEntities.NUCLEO_BLOCK_ENTITY, (world1, pos, state1, be) -> NucleoBlockEntity.tick(world1, pos, state1, (NucleoBlockEntity) be));
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