package com.journeyassistant.location;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HaversineDistanceTest {

    @Test
    void returnsZeroForIdenticalPoints() {
        assertEquals(0.0, HaversineDistance.betweenMeters(12.9716, 77.5946, 12.9716, 77.5946), 0.001);
    }

    @Test
    void calculatesOneDegreeAlongTheEquator() {
        assertEquals(111_194.9, HaversineDistance.betweenMeters(0, 0, 0, 1), 1.0);
    }

    @Test
    void usesTheShortArcAcrossTheInternationalDateLine() {
        assertEquals(222_389.9, HaversineDistance.betweenMeters(0, 179, 0, -179), 2.0);
    }

    @Test
    void rejectsOutOfRangeCoordinates() {
        assertThrows(IllegalArgumentException.class,
                () -> HaversineDistance.betweenMeters(91, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> HaversineDistance.betweenMeters(0, Double.NaN, 0, 0));
    }
}
