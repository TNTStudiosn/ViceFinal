package com.TNTStudios.vicefinal.client;

import com.TNTStudios.vicefinal.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.TNTStudios.vicefinal.client.entity.SrTiempoRenderer;

public class VicefinalClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.SR_TIEMPO, SrTiempoRenderer::new);
    }
}
