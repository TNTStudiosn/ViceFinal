package com.TNTStudios.vicefinal.client;

import com.TNTStudios.vicefinal.blocks.VicefinalBlocks;
import com.TNTStudios.vicefinal.client.gui.screen.NucleoScreen; // Importo la nueva pantalla
import com.TNTStudios.vicefinal.registry.ModBlockEntities;
import com.TNTStudios.vicefinal.registry.ModEntities;
import com.TNTStudios.vicefinal.registry.ModScreenHandlers; // Importo el registro
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.TNTStudios.vicefinal.client.entity.SrTiempoRenderer;
import com.TNTStudios.vicefinal.client.blocks.NucleoBlockEntityRenderer;
import net.minecraft.client.gui.screen.ingame.HandledScreens; // Importo HandledScreens
import net.minecraft.client.render.RenderLayer;
import com.TNTStudios.vicefinal.client.gui.screen.BossChallengeScreen;

public class VicefinalClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.SR_TIEMPO, SrTiempoRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.NUCLEO_BLOCK_ENTITY, NucleoBlockEntityRenderer::new);

        HandledScreens.register(ModScreenHandlers.NUCLEO_SCREEN_HANDLER, NucleoScreen::new);
        // REGISTRO MI NUEVA PANTALLA
        HandledScreens.register(ModScreenHandlers.BOSS_CHALLENGE_SCREEN_HANDLER, BossChallengeScreen::new);
    }
}