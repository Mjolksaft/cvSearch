package com.cvsearch.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

import com.cvsearch.util.DistanceCalculator;

class DistanceCalculatorTest {

    @Test
    void haversine_LundToStockholm() {
        double distance = DistanceCalculator.haversine(55.704, 13.191, 59.329, 18.068);

        assertThat(distance).isCloseTo(497.0, within(1.0));
    }

    @Test
    void haversine_SamePoint_ShouldReturnZero() {
        double distance = DistanceCalculator.haversine(55.704, 13.191, 55.704, 13.191);

        assertThat(distance).isCloseTo(0.0, within(0.001));
    }

    @Test
    void haversine_LundToMalmo() {
        double distance = DistanceCalculator.haversine(55.704, 13.191, 55.605, 13.003);

        assertThat(distance).isCloseTo(16.1, within(0.5));
    }

    @Test
    void haversine_NorthPoleToEquator() {
        double distance = DistanceCalculator.haversine(90, 0, 0, 0);

        assertThat(distance).isCloseTo(10007.0, within(1.0));
    }

    @Test
    void haversine_Symmetric() {
        double aToB = DistanceCalculator.haversine(55.704, 13.191, 59.329, 18.068);
        double bToA = DistanceCalculator.haversine(59.329, 18.068, 55.704, 13.191);

        assertThat(aToB).isCloseTo(bToA, within(0.001));
    }
}
