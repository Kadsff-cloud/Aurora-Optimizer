package dev.aurora.optimizer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.aurora.optimizer.Aurora;
import dev.aurora.optimizer.AuroraConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

import java.util.List;

/**
 * Dibuja el LOD lejano como quads de color. Es intencionadamente barato:
 * una submission por frame, sin texturas ni entidades, y con presupuesto de
 * quads para que un radio grande no destruya los FPS.
 */
public final class LodRenderer {
    private static boolean loggedFirstFrame;
    private static boolean loggedDiagnostic;
    private static long lastDebugLogMillis;

    private LodRenderer() {
    }

    public static void onCollectSubmits(LevelRenderContext context) {
        HookProbe.mark("render");
        AuroraConfig config = Aurora.config();
        if (!loggedDiagnostic) {
            loggedDiagnostic = true;
            boolean hasState = context.levelState() != null;
            Aurora.LOGGER.info("LOD diag: active={} pose={} state={} camera={} chunks={}",
                    config.lodActive(),
                    context.poseStack() != null,
                    hasState,
                    hasState && context.levelState().cameraRenderState != null,
                    LodManager.size());
        }
        if (!config.lodActive() || context.poseStack() == null || context.levelState() == null) {
            return;
        }

        List<LodChunk> chunks = LodManager.snapshot();
        if (chunks.isEmpty()) {
            return;
        }

        CameraRenderState camera = context.levelState().cameraRenderState;
        if (camera == null || camera.pos == null) {
            return;
        }

        Vec3 cam = camera.pos;
        Frustum frustum = camera.cullFrustum;
        Minecraft minecraft = Minecraft.getInstance();
        int effective = minecraft.options.getEffectiveRenderDistance();
        // En modo debug se usa el inicio configurado y se omite el frustum para
        // poder verificar que la geometria se dibuja aunque la camara no apunte.
        int startChunks = config.lodDebug
                ? Math.max(1, config.lodStartDistanceChunks)
                : Math.max(config.lodStartDistanceChunks, effective + 1);
        int maxChunks = Math.max(startChunks + 1, config.lodMaxDistanceChunks);
        double startBlocks = startChunks * 16.0;
        double maxBlocks = maxChunks * 16.0;
        double maxBlocksSq = maxBlocks * maxBlocks;
        double startBlocksSq = startBlocks * startBlocks;
        int maxQuads = config.lodMaxQuads;

        final int fogColor;
        if (camera.fogData != null && camera.fogData.color != null) {
            Vector4f color = camera.fogData.color;
            fogColor = ((int) (color.x * 255) << 16) | ((int) (color.y * 255) << 8) | (int) (color.z * 255);
        } else {
            fogColor = 0x9AB8D8;
        }

        AuroraStats.lodQuads = 0;
        context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.debugQuads(),
                (pose, consumer) -> {
                    // submitCustomGeometry puede diferir la ejecucion: el conteo
                    // real se hace aqui, no al encolar la submission.
                    int drawn = 0;
                    outer:
                    for (LodChunk chunk : chunks) {
                        if (drawn >= maxQuads) {
                            break;
                        }

                        double centerX = chunk.chunkX * 16.0 + 8.0;
                        double centerZ = chunk.chunkZ * 16.0 + 8.0;
                        double dx = centerX - cam.x;
                        double dz = centerZ - cam.z;
                        double distSq = dx * dx + dz * dz;
                        if (distSq < startBlocksSq || distSq > maxBlocksSq) {
                            continue;
                        }

                        double minX = chunk.chunkX * 16.0;
                        double minZ = chunk.chunkZ * 16.0;
                        if (!config.lodDebug && frustum != null
                                && !frustum.isVisible(new AABB(minX, -64.0, minZ, minX + 16.0, 320.0, minZ + 16.0))) {
                            continue;
                        }

                        float fog = (float) Math.max(0.0, Math.min(1.0,
                                (Math.sqrt(distSq) - startBlocks) / Math.max(1.0, maxBlocks - startBlocks)));
                        fog *= 0.85f;

                        for (int cz = 0; cz < chunk.cells; cz++) {
                            for (int cx = 0; cx < chunk.cells; cx++) {
                                if (drawn >= maxQuads) {
                                    break outer;
                                }
                                int index = cz * chunk.cells + cx;
                                short height = chunk.heights[index];
                                if (height == LodChunk.EMPTY) {
                                    continue;
                                }

                                int color = chunk.colors[index];
                                int r = blend((color >> 16) & 0xFF, (fogColor >> 16) & 0xFF, fog);
                                int g = blend((color >> 8) & 0xFF, (fogColor >> 8) & 0xFF, fog);
                                int b = blend(color & 0xFF, fogColor & 0xFF, fog);
                                int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
                                int sideArgb = shade(argb, 0.68f);

                                float x0 = (float) (minX + cx * chunk.cellSize - cam.x);
                                float x1 = (float) (x0 + chunk.cellSize);
                                float z0 = (float) (minZ + cz * chunk.cellSize - cam.z);
                                float z1 = (float) (z0 + chunk.cellSize);
                                // La cara superior del bloque esta en height + 1, no en height + 0.06.
                                float yTop = (float) (height + 1.0 - cam.y);

                                addQuad(pose, consumer, x0, yTop, z0, x1, z1, argb);
                                drawn++;

                                // Sin caras laterales el LOD lejano se ve como puntos flotantes.
                                if (cz > 0) {
                                    short n = chunk.heights[index - chunk.cells];
                                    if (n != LodChunk.EMPTY && n < height) {
                                        addSideNorth(pose, consumer, x0, x1, z0,
                                                yTop, (float) (n + 1.0 - cam.y), sideArgb);
                                        drawn++;
                                    }
                                }
                                if (cz + 1 < chunk.cells) {
                                    short s = chunk.heights[index + chunk.cells];
                                    if (s != LodChunk.EMPTY && s < height) {
                                        addSideSouth(pose, consumer, x0, x1, z1,
                                                yTop, (float) (s + 1.0 - cam.y), sideArgb);
                                        drawn++;
                                    }
                                }
                                if (cx > 0) {
                                    short w = chunk.heights[index - 1];
                                    if (w != LodChunk.EMPTY && w < height) {
                                        addSideWest(pose, consumer, z0, z1, x0,
                                                yTop, (float) (w + 1.0 - cam.y), sideArgb);
                                        drawn++;
                                    }
                                }
                                if (cx + 1 < chunk.cells) {
                                    short e = chunk.heights[index + 1];
                                    if (e != LodChunk.EMPTY && e < height) {
                                        addSideEast(pose, consumer, z0, z1, x1,
                                                yTop, (float) (e + 1.0 - cam.y), sideArgb);
                                        drawn++;
                                    }
                                }
                            }
                        }
                    }

                    AuroraStats.lodQuads = drawn;
                    if (!loggedFirstFrame && drawn > 0) {
                        loggedFirstFrame = true;
                        Aurora.LOGGER.info("LOD: primer frame dibujado con {} quads desde {} chunks", drawn, chunks.size());
                    }
                    if (config.lodDebug && drawn > 0) {
                        long now = System.currentTimeMillis();
                        if (now - lastDebugLogMillis >= 1000L) {
                            lastDebugLogMillis = now;
                            Aurora.LOGGER.info("LOD: {} chunks en cache, {} quads", chunks.size(), drawn);
                        }
                    }
                });
    }

    private static int blend(int from, int to, float amount) {
        return (int) (from + (to - from) * amount);
    }

    private static void addQuad(PoseStack.Pose pose, VertexConsumer consumer,
                                float x0, float y, float z0, float x1, float z1, int color) {
        consumer.addVertex(pose, x0, y, z0).setColor(color);
        consumer.addVertex(pose, x0, y, z1).setColor(color);
        consumer.addVertex(pose, x1, y, z1).setColor(color);
        consumer.addVertex(pose, x1, y, z0).setColor(color);
    }

    private static void addSideNorth(PoseStack.Pose pose, VertexConsumer consumer,
                                     float x0, float x1, float z, float yTop, float yBottom, int color) {
        consumer.addVertex(pose, x0, yTop, z).setColor(color);
        consumer.addVertex(pose, x1, yTop, z).setColor(color);
        consumer.addVertex(pose, x1, yBottom, z).setColor(color);
        consumer.addVertex(pose, x0, yBottom, z).setColor(color);
    }

    private static void addSideSouth(PoseStack.Pose pose, VertexConsumer consumer,
                                     float x0, float x1, float z, float yTop, float yBottom, int color) {
        consumer.addVertex(pose, x1, yTop, z).setColor(color);
        consumer.addVertex(pose, x0, yTop, z).setColor(color);
        consumer.addVertex(pose, x0, yBottom, z).setColor(color);
        consumer.addVertex(pose, x1, yBottom, z).setColor(color);
    }

    private static void addSideWest(PoseStack.Pose pose, VertexConsumer consumer,
                                    float z0, float z1, float x, float yTop, float yBottom, int color) {
        consumer.addVertex(pose, x, yTop, z1).setColor(color);
        consumer.addVertex(pose, x, yTop, z0).setColor(color);
        consumer.addVertex(pose, x, yBottom, z0).setColor(color);
        consumer.addVertex(pose, x, yBottom, z1).setColor(color);
    }

    private static void addSideEast(PoseStack.Pose pose, VertexConsumer consumer,
                                    float z0, float z1, float x, float yTop, float yBottom, int color) {
        consumer.addVertex(pose, x, yTop, z0).setColor(color);
        consumer.addVertex(pose, x, yTop, z1).setColor(color);
        consumer.addVertex(pose, x, yBottom, z1).setColor(color);
        consumer.addVertex(pose, x, yBottom, z0).setColor(color);
    }

    private static int shade(int argb, float factor) {
        int r = (int) (((argb >> 16) & 0xFF) * factor);
        int g = (int) (((argb >> 8) & 0xFF) * factor);
        int b = (int) ((argb & 0xFF) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
