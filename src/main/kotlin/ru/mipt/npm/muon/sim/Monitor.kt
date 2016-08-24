package ru.mipt.npm.muon.sim

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import java.util.*

/**
 * Created by darksnake on 09-May-16.
 */

val GEOMETRY_TOLERANCE = 0.01;
val PIXEL_XY_SIZE = 125.0;
val PIXEL_Z_SIZE = 30.0;
val CENTRAL_LAYER_Z = 0.0;
val UPPER_LAYER_Z = 166.0;
val LOWER_LAYER_Z =  -180.0;

var rnd: RandomGenerator = JDKRandomGenerator();

val layers = arrayOf(Layer("center", CENTRAL_LAYER_Z), Layer("up", UPPER_LAYER_Z), Layer("bottom", LOWER_LAYER_Z));
val pixels = buildPixels()

fun findLayer(z: Double): Layer {
    for (l in layers) {
        if (l.z == z) {
            return l;
        }
    }
    throw RuntimeException("Could not find layer with z = ${z}");
}

/**
 * Build map for the whole monitor
 */
fun buildPixels(): Map<String, Pixel> {
    var map = HashMap<String, Pixel>();
    ClassLoader.getSystemClassLoader().getResourceAsStream("map-RMM110.sc16").bufferedReader().forEachLine { line ->
        if (line.startsWith(" ")) {
            var split = line.trim().split("\\s+".toPattern());
            var detectorName = split[1];
            var x = split[4].toDouble() - 500;
            var y = split[5].toDouble() - 500;
            var z = split[6].toDouble() - 180;
            map.putAll(buildDetector(detectorName, Vector3D(x, y, z)))
        }
    }
    return map
}

/**
 * Build map for single detector
 */
fun buildDetector(detectorName: String, detectorPos: Vector3D): Map<String, Pixel> {
    var map = HashMap<String, Pixel>();
    for (index in 0..15) {
        var x: Double;
        var y: Double;
        when (index) {
            7 -> {
                x = 1.5 * PIXEL_XY_SIZE;
                y = 1.5 * PIXEL_XY_SIZE;
            }
            4 -> {
                x = 0.5 * PIXEL_XY_SIZE;
                y = 1.5 * PIXEL_XY_SIZE;
            }
            6 -> {
                x = 1.5 * PIXEL_XY_SIZE;
                y = 0.5 * PIXEL_XY_SIZE;
            }
            5 -> {
                x = 0.5 * PIXEL_XY_SIZE;
                y = 0.5 * PIXEL_XY_SIZE;
            }

            3 -> {
                x = -1.5 * PIXEL_XY_SIZE;
                y = 1.5 * PIXEL_XY_SIZE;
            }
            0 -> {
                x = -0.5 * PIXEL_XY_SIZE;
                y = 1.5 * PIXEL_XY_SIZE;
            }
            2 -> {
                x = -1.5 * PIXEL_XY_SIZE;
                y = 0.5 * PIXEL_XY_SIZE;
            }
            1 -> {
                x = -0.5 * PIXEL_XY_SIZE;
                y = 0.5 * PIXEL_XY_SIZE;
            }

            11 -> {
                x = -1.5 * PIXEL_XY_SIZE;
                y = -1.5 * PIXEL_XY_SIZE;
            }
            8 -> {
                x = -0.5 * PIXEL_XY_SIZE;
                y = -1.5 * PIXEL_XY_SIZE;
            }
            10 -> {
                x = -1.5 * PIXEL_XY_SIZE;
                y = -0.5 * PIXEL_XY_SIZE;
            }
            9 -> {
                x = -0.5 * PIXEL_XY_SIZE;
                y = -0.5 * PIXEL_XY_SIZE;
            }

            15 -> {
                x = 1.5 * PIXEL_XY_SIZE;
                y = -1.5 * PIXEL_XY_SIZE;
            }
            12 -> {
                x = 0.5 * PIXEL_XY_SIZE;
                y = -1.5 * PIXEL_XY_SIZE;
            }
            14 -> {
                x = 1.5 * PIXEL_XY_SIZE;
                y = -0.5 * PIXEL_XY_SIZE;
            }
            13 -> {
                x = 0.5 * PIXEL_XY_SIZE;
                y = -0.5 * PIXEL_XY_SIZE;
            }
            else -> throw Error();
        }
        val offset = rotateDetector(Vector3D(x, y, 0.0));
        val pixelName = "${detectorName}_${index}"
        val pixel = Pixel(pixelName, detectorPos.add(offset))
        map.put(pixelName, pixel)
    }
    return map
}

/**
 * Apply current detector rotation
 */
fun rotateDetector(vector: Vector3D): Vector3D {
    return Vector3D(-vector.y, vector.x, vector.z);
}

private fun coordsToString(vector: Vector3D): String {
    return "(${vector.x},${vector.y},${vector.z})"
}