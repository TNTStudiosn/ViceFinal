package com.TNTStudios.vicefinal.blocks;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.TNTStudios.vicefinal.blocks.Nucleo;

// Clase de registro de bloques, por ejemplo: VicefinalBlocks.java
public class VicefinalBlocks {

    public static final Block NUCLEO_BLOCK = Registry.register(Registries.BLOCK,
            new Identifier("vicefinal", "nucleo"),
            new Nucleo(FabricBlockSettings
                    .copyOf(Blocks.BEDROCK) // Copiamos las propiedades de la Bedrock
                    .strength(-1.0F, 3600000.0F) // Firmeza y resistencia para no ser destruible
                    .dropsNothing() // No dropea nada
            )
    );

    public static final Item NUCLEO_ITEM = Registry.register(Registries.ITEM,
            new Identifier("vicefinal", "nucleo"),
            new BlockItem(NUCLEO_BLOCK, new Item.Settings()) // Aquí estaba el problema
    );


    public static void register() {
        // Este método se llama en el ModInit para registrar
    }
}
