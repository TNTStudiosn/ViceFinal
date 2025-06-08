package com.TNTStudios.vicefinal.client;

import com.TNTStudios.vicefinal.blocks.VicefinalBlocks;
import com.TNTStudios.vicefinal.registry.ModBlockEntities;
import com.TNTStudios.vicefinal.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.TNTStudios.vicefinal.client.entity.SrTiempoRenderer;
import com.TNTStudios.vicefinal.client.blocks.NucleoBlockEntityRenderer;
import net.minecraft.client.render.RenderLayer;

public class VicefinalClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.SR_TIEMPO, SrTiempoRenderer::new);

        BlockEntityRendererRegistry.register(ModBlockEntities.NUCLEO_BLOCK_ENTITY, NucleoBlockEntityRenderer::new);

        // Esto hace que el bloque Nucleo se vea sólido
        BlockRenderLayerMap.INSTANCE.putBlock(VicefinalBlocks.NUCLEO_BLOCK, RenderLayer.getSolid());
    }
}
