package com.TNTStudios.vicefinal.client.blocks;

import com.TNTStudios.vicefinal.blocks.NucleoBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class NucleoBlockEntityRenderer implements BlockEntityRenderer<NucleoBlockEntity> {

    // Defino aquí los efectos de partículas para no crearlos en cada renderizado.
    private static final DustParticleEffect PARTICULA_AZUL = new DustParticleEffect(new Vector3f(0.2f, 0.5f, 1.0f), 1.0f);
    private static final DustParticleEffect PARTICULA_VERDE = new DustParticleEffect(new Vector3f(0.2f, 1.0f, 0.5f), 1.0f);

    public NucleoBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        // No hace falta inicialización adicional.
    }

    @Override
    public void render(NucleoBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.gameRenderer == null) {
            return; // Me aseguro de que el mundo y el renderer no sean nulos.
        }

        BlockPos pos = entity.getPos();

        // --- Render texto flotante ---
        matrices.push();

        // Calculo la oscilación vertical del texto.
        double time = (System.currentTimeMillis() % 2000L) / 2000.0 * Math.PI * 2;
        float yOffset = (float)(Math.sin(time) * 0.1);

        // Muevo el texto a su posición inicial sobre el bloque.
        matrices.translate(0.5, 1.5 + yOffset, 0.5);

        // Hago que el texto rote para encarar siempre al jugador.
        matrices.multiply(client.gameRenderer.getCamera().getRotation());

        // Escalo el texto y lo invierto en el eje Y para que se vea correctamente.
        matrices.scale(0.02f, -0.02f, 0.02f);

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        // Dibujo el texto centrado.
        client.textRenderer.draw(
                Text.literal("Núcleo del Tiempo").formatted(Formatting.AQUA),
                -client.textRenderer.getWidth("Núcleo del Tiempo") / 2f,
                0f,
                -1, // -1 usa el color del Text, si no lo tuviera sería blanco.
                true, // Sombra.
                matrix4f,
                vertexConsumers,
                TextRenderer.TextLayerType.SEE_THROUGH, // Lo cambio a SEE_THROUGH para asegurar que se renderice siempre.
                // El tipo NORMAL puede hacer que el texto sea ocluido (no dibujado) si
                // el motor piensa que está detrás de otro objeto. SEE_THROUGH evita esto.
                0, // Color de fondo.
                light
        );

        matrices.pop();

        // --- Render partículas ---
        // Controlo la frecuencia con la que aparecen las partículas para no sobrecargar el cliente.
        if (client.world.getTime() % 4 == 0) {
            for (int i = 0; i < 2; i++) { // Reduzco un poco el número de partículas por tick.
                // Aumento la dispersión de las partículas para que no estén tan juntas.
                double offsetX = (client.world.random.nextDouble() - 0.5) * 1.8;
                double offsetY = client.world.random.nextDouble() * 1.2;
                double offsetZ = (client.world.random.nextDouble() - 0.5) * 1.8;

                // Elijo aleatoriamente entre la partícula azul y la verde.
                DustParticleEffect particleEffect = client.world.random.nextBoolean() ? PARTICULA_AZUL : PARTICULA_VERDE;

                client.world.addParticle(
                        particleEffect,
                        pos.getX() + 0.5 + offsetX,
                        pos.getY() + 0.5 + offsetY,
                        pos.getZ() + 0.5 + offsetZ,
                        0, 0.02, 0
                );
            }
        }
    }
}