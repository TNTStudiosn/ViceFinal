package com.TNTStudios.vicefinal.client.entity;

import com.TNTStudios.vicefinal.entity.SrTiempoEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SrTiempoRenderer extends GeoEntityRenderer<SrTiempoEntity> {

    public SrTiempoRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new SrTiempoModel());
        this.shadowRadius = 1.5f; // Tamaño de sombra
    }
}
