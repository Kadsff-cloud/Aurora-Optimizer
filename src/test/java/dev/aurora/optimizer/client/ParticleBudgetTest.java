package dev.aurora.optimizer.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleBudgetTest {
    @Test
    void countsGroupedPendingAndEmitters() {
        assertEquals(30, ParticleBudget.used(10, 15, 5));
        assertEquals(0, ParticleBudget.used(-4, -1, -9), "valores negativos no deben restar presupuesto");
    }

    @Test
    void zeroOrNegativeLimitMeansUnlimited() {
        assertFalse(ParticleBudget.isFull(1_000_000, 0));
        assertFalse(ParticleBudget.isFull(1_000_000, -5));
        assertTrue(ParticleBudget.isFull(4000, 4000));
        assertFalse(ParticleBudget.isFull(3999, 4000));
    }

    @Test
    void emitterLimitIsIndependent() {
        assertTrue(ParticleBudget.isEmitterFull(256, 256));
        assertFalse(ParticleBudget.isEmitterFull(255, 256));
        assertFalse(ParticleBudget.isEmitterFull(10_000, 0));
    }
}
