package ru.mipt.npm.muon.sim;

import org.apache.commons.math3.optim.PointValuePair;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Created by darksnake on 16-Oct-16.
 */
public class SphericalHarmonicsTest {
    /**
     * Test if old and new formula give the same result;
     *
     * @throws Exception
     */
    @Test
    public void plgndr() throws Exception {
        for (int l = 0; l < 10; l++) {
            for (int m = 0; m < l; m++) {
                assertEquals(SphericalHarmonics.plgndrTest(l, m, 0.5), SphericalHarmonics.plgndr(l, m, 0.5), 0.001);
            }
        }
    }

    @Test
    public void fit() {
        SphericalHarmonics.Point[] points = new SphericalHarmonics.Point[3];
        points[0] = new SphericalHarmonics.Point(0, 0, 100, 10);
        points[1] = new SphericalHarmonics.Point(Math.PI / 2, Math.PI / 2, 100, 10);
        points[2] = new SphericalHarmonics.Point(-Math.PI / 2, -Math.PI / 2, 100, 10);
        PointValuePair pair = SphericalHarmonics.fit(points, 2);
        System.out.printf("The point is %s, the value is %f", Arrays.toString(pair.getPoint()), pair.getValue());
    }


}