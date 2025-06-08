package com.TNTStudios.vicefinal;

import com.TNTStudios.vicefinal.blocks.VicefinalBlocks;
import com.TNTStudios.vicefinal.registry.ModEntities;
import net.fabricmc.api.ModInitializer;

public class Vicefinal implements ModInitializer {

    @Override
    public void onInitialize() {
        ModEntities.register();
        VicefinalBlocks.register();
    }
}
