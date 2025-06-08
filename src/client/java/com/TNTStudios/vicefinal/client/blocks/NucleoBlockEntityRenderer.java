package com.TNTStudios.vicefinal.client.blocks;

import com.TNTStudios.vicefinal.blocks.NucleoBlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import org.joml.Matrix4f;


public class NucleoBlockEntityRenderer implements BlockEntityRenderer<NucleoBlockEntity> {

    public NucleoBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        // No hace falta inicialización adicional
    }

    @Override
    public void render(NucleoBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        MinecraftClient client = MinecraftClient.getInstance();
        BlockPos pos = entity.getPos();

        // --- Render texto flotante ---
        matrices.push();
        double time = (System.currentTimeMillis() % 2000L) / 2000.0 * Math.PI * 2;
        float yOffset = (float)(Math.sin(time) * 0.1);

        matrices.translate(0.5, 1.5 + yOffset, 0.5);
        matrices.scale(0.02f, -0.02f, 0.02f);

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        client.textRenderer.draw(
                "Núcleo del Tiempo",
                -client.textRenderer.getWidth("Núcleo del Tiempo") / 2f,
                0f,
                Formatting.AQUA.getColorValue(),
                true, // shadow
                matrix4f,
                vertexConsumers,
                TextRenderer.TextLayerType.NORMAL,
                0, // background color
                light
        );




        matrices.pop();

        // --- Render partículas ---
        if (client.world.getTime() % 5 == 0) { // No spamear partículas cada frame
            for (int i = 0; i < 3; i++) {
                double offsetX = (client.world.random.nextDouble() - 0.5) * 0.5;
                double offsetY = client.world.random.nextDouble() * 0.5;
                double offsetZ = (client.world.random.nextDouble() - 0.5) * 0.5;

                client.world.addParticle(
                        client.world.random.nextBoolean() ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.END_ROD,
                        pos.getX() + 0.5 + offsetX,
                        pos.getY() + 0.5 + offsetY,
                        pos.getZ() + 0.5 + offsetZ,
                        0, 0.02, 0
                );
            }
        }

        // --- TODO opcional: Render beam (requiere un poco más de código con RenderSystem) ---
    }
}
