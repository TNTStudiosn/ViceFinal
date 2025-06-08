package com.TNTStudios.vicefinal.client.entity;

import com.TNTStudios.vicefinal.entity.SrTiempoEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SrTiempoModel extends DefaultedEntityGeoModel<SrTiempoEntity> {

    public SrTiempoModel() {
        super(new Identifier("vicefinal", "srtiempo"));
    }
}
