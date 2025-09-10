package com.thealgorithms.audiofilters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IIRFilterTest {

    @Test
    void testConstructorValidOrder() {
        // Test a valid filter creation
        IIRFilter filter = new IIRFilter(2);
        assertNotNull(filter, "Filter should be instantiated correctly");
    }

    @Test
    void testConstructorInvalidOrder() {
        // Test an invalid filter creation (order <= 0)
        assertThrows(IllegalArgumentException.class, () -> { new IIRFilter(0); }, "Order must be greater than zero");
    }

    @Test
    void testSetCoeffsInvalidLengthA() {
        IIRFilter filter = new IIRFilter(2);

        // Invalid 'aCoeffs' length
        double[] aCoeffs = {1.0}; // too short
        double[] bCoeffs = {1.0, 0.5};
        assertThrows(IllegalArgumentException.class, () -> { filter.setCoeffs(aCoeffs, bCoeffs); }, "aCoeffs must be of size 2");
    }

    @Test
    void testSetCoeffsInvalidLengthB() {
        IIRFilter filter = new IIRFilter(2);

        // Invalid 'bCoeffs' length
        double[] aCoeffs = {1.0, 0.5};
        double[] bCoeffs = {1.0}; // too short
        assertThrows(IllegalArgumentException.class, () -> { filter.setCoeffs(aCoeffs, bCoeffs); }, "bCoeffs must be of size 2");
    }

    @Test
    void testSetCoeffsInvalidACoeffZero() {
        IIRFilter filter = new IIRFilter(2);

        // Invalid 'aCoeffs' where aCoeffs[0] == 0.0
        double[] aCoeffs = {0.0, 0.5}; // aCoeffs[0] must not be zero
        double[] bCoeffs = {1.0, 0.5};
        assertThrows(IllegalArgumentException.class, () -> { filter.setCoeffs(aCoeffs, bCoeffs); }, "aCoeffs[0] must not be zero");
    }

    @Test
    void testProcessWithNoCoeffsSet() {
        // Test process method with default coefficients (sane defaults)
        IIRFilter filter = new IIRFilter(2);
        double inputSample = 0.5;
        double result = filter.process(inputSample);

        // Since default coeffsA[0] and coeffsB[0] are 1.0, expect output = input
        assertEquals(inputSample, result, 1e-6, "Process should return the same value as input with default coefficients");
    }

    @Test
    void testProcessWithCoeffsSet() {
        // Test process method with set coefficients
        IIRFilter filter = new IIRFilter(2);

        double[] aCoeffs = {1.0, 0.5};
        double[] bCoeffs = {1.0, 0.5};
        filter.setCoeffs(aCoeffs, bCoeffs);

        // Process a sample
        double inputSample = 0.5;
        double result = filter.process(inputSample);

        // Expected output can be complex to calculate in advance;
        // check if the method runs and returns a result within reasonable bounds
        assertTrue(result >= -1.0 && result <= 1.0, "Processed result should be in the range [-1, 1]");
    }

    @Test
    void testImpulseResponseFirstOrderBehavior() {
        // Using order=2 but only first-order terms are non-zero due to current setCoeffs behavior
        // Difference equation realized: y[n] = b0*x[n] - a1*y[n-1] with a0=1
        IIRFilter filter = new IIRFilter(2);
        double[] aCoeffs = {1.0, 0.5}; // a0=1.0, a1=0.5 → y[n] = x[n] - 0.5*y[n-1]
        double[] bCoeffs = {1.0, 0.0}; // b0=1.0, b1=0.0
        filter.setCoeffs(aCoeffs, bCoeffs);

        double[] impulse = {1.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        double[] expected = {1.0, -0.5, 0.25, -0.125, 0.0625, -0.03125};

        for (int i = 0; i < impulse.length; i++) {
            double y = filter.process(impulse[i]);
            assertEquals(expected[i], y, 1e-9, "Impulse response mismatch at index " + i);
        }
    }

    @Test
    void testEMAEquivalentBehaviorWithZeroInitialCondition() {
        // Target EMA: y[n] = alpha*x[n] + (1-alpha)*y[n-1]
        // Map to filter: a0=1, a1=-(1-alpha); b0=alpha, b1=0
        double alpha = 0.2;
        IIRFilter filter = new IIRFilter(2);
        double[] aCoeffs = {1.0, -(1.0 - alpha)}; // {1.0, -0.8}
        double[] bCoeffs = {alpha, 0.0};          // {0.2, 0.0}
        filter.setCoeffs(aCoeffs, bCoeffs);

        double[] x = {0.1, 0.5, 0.8, 0.6, 0.3, 0.9, 0.4};
        double[] expected = new double[x.length];
        double yPrev = 0.0; // zero initial condition due to filter's history default
        for (int i = 0; i < x.length; i++) {
            expected[i] = alpha * x[i] + (1.0 - alpha) * yPrev;
            yPrev = expected[i];
        }

        for (int i = 0; i < x.length; i++) {
            double y = filter.process(x[i]);
            assertEquals(expected[i], y, 1e-9, "EMA-equivalent output mismatch at index " + i);
        }
    }
}
