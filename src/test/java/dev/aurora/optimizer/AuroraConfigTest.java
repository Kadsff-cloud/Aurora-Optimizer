package dev.aurora.optimizer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuroraConfigTest {
    @Test
    void clampsDangerousValues() {
        AuroraConfig config = new AuroraConfig();
        config.particleLimit = -5;
        config.entityCullDistance = 9999;
        config.lodStartDistanceChunks = 10;
        config.lodMaxDistanceChunks = 2;
        config.fogStart = 0.9f;
        config.fogEnd = 0.1f;
        config.lodMode = "banana";

        config.sanitize();

        assertEquals(0, config.particleLimit);
        assertEquals(512, config.entityCullDistance);
        assertTrue(config.lodMaxDistanceChunks > config.lodStartDistanceChunks);
        assertTrue(config.fogEnd > config.fogStart);
        assertEquals("auto", config.lodMode);
    }

    @Test
    void cellSizeIsPowerOfTwoWithinRange() {
        AuroraConfig config = new AuroraConfig();
        config.lodCellSize = 3;
        assertEquals(4, config.cellSize());
        config.lodCellSize = 0;
        assertEquals(1, config.cellSize());
        config.lodCellSize = 99;
        assertEquals(8, config.cellSize());
    }

    @Test
    void voxyModeDisablesBuiltInLod() {
        AuroraConfig config = new AuroraConfig();
        config.lodEnabled = true;
        config.lodMode = "voxy";
        assertFalse(config.lodActive(false, false));
        config.lodMode = "aurora";
        assertTrue(config.lodActive(false, false));
    }

    @Test
    void autoLodCedesToSodiumAndVoxy() {
        AuroraConfig config = new AuroraConfig();
        config.lodEnabled = true;
        config.lodMode = "auto";
        config.sodiumCompat = true;
        config.voxyCompat = true;

        assertTrue(config.lodActive(false, false), "sin Sodium/Voxy, auto usa Aurora LOD");
        assertFalse(config.lodActive(true, false), "auto debe ceder a Sodium");
        assertFalse(config.lodActive(false, true), "auto debe ceder a Voxy");
        assertFalse(config.lodActive(true, true));

        config.lodMode = "aurora";
        assertTrue(config.lodActive(true, true), "aurora fuerza el LOD aunque haya Sodium/Voxy");
    }

    @Test
    void compatFlagsCanKeepAutoLod() {
        AuroraConfig config = new AuroraConfig();
        config.lodEnabled = true;
        config.lodMode = "auto";
        config.sodiumCompat = false;
        config.voxyCompat = false;

        assertTrue(config.lodActive(true, true), "si el usuario desactiva la compatibilidad, auto no cede");
    }

    @Test
    void worldgenDefaultsAreOptInAndConservative() {
        AuroraConfig config = new AuroraConfig();

        assertFalse(config.lodWorldgen, "el worldgen LOD debe ser opt-in");
        assertEquals(4, config.lodWorldgenBatch);
        assertEquals(18, config.lodWorldgenRadiusChunks);
        assertEquals(12000, config.lodMaxQuads);
    }

    @Test
    void clampsWorldgenAndEmitterLimits() {
        AuroraConfig config = new AuroraConfig();
        config.trackingEmitterLimit = -1;
        config.lodWorldgenBatch = 99;
        config.lodWorldgenRadiusChunks = 1;
        config.lodStartDistanceChunks = 8;
        config.lodMaxDistanceChunks = 96;

        config.sanitize();

        assertEquals(0, config.trackingEmitterLimit);
        assertEquals(8, config.lodWorldgenBatch);
        assertEquals(config.lodStartDistanceChunks + 1, config.lodWorldgenRadiusChunks);

        config.lodWorldgenRadiusChunks = 999;
        config.sanitize();
        assertEquals(config.lodMaxDistanceChunks, config.lodWorldgenRadiusChunks);
    }
}
