package ru.mipt.npm.muon.sim

import org.apache.commons.math3.geometry.euclidean.threed.Plane
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D

/**
 * A single pixel
 * Created by darksnake on 09-May-16.
 */
class Pixel(val name: String, val center: Vector3D, var efficiency: Double = 1.0,
            val xSize: Double = PIXEL_XY_SIZE, val ySize: Double = PIXEL_XY_SIZE, val zSize: Double = PIXEL_Z_SIZE) {
    //    val layer: Layer = findLayer(center.z);
    private val upLayer = findLayer(center.z + zSize / 2.0)//Layer("${name}_up", center.z + zSize / 2.0);
    private val bottomLayer = findLayer(center.z - zSize / 2.0)//Layer("${name}_bottom", center.z - zSize / 2.0);
    private val centralLayer = findLayer(center.z)

    val sideLayers: Array<Plane> = arrayOf(Plane(center.add(Vector3D(PIXEL_XY_SIZE / 2, 0.0, 0.0)), Vector3D(1.0, 0.0, 0.0), GEOMETRY_TOLERANCE),
            Plane(center.add(Vector3D(-PIXEL_XY_SIZE / 2, 0.0, 0.0)), Vector3D(-1.0, 0.0, 0.0), GEOMETRY_TOLERANCE),
            Plane(center.add(Vector3D(0.0, PIXEL_XY_SIZE / 2, 0.0)), Vector3D(0.0, 1.0, 0.0), GEOMETRY_TOLERANCE),
            Plane(center.add(Vector3D(0.0, -PIXEL_XY_SIZE / 2, 0.0)), Vector3D(0.0, -1.0, 0.0), GEOMETRY_TOLERANCE));

    //TODO add efficiency
    fun containsPoint(x: Double, y: Double, z: Double, tolerance: Double = GEOMETRY_TOLERANCE): Boolean {
        return x <= this.center.x + this.xSize / 2.0 + tolerance && x >= this.center.x - this.xSize / 2.0 - tolerance &&
                y <= this.center.y + this.ySize / 2.0 + tolerance && y >= this.center.y - this.ySize / 2.0 - tolerance &&
                z <= this.center.z + this.zSize / 2.0 + tolerance && z >= this.center.z - this.zSize / 2.0 - tolerance;
    }

    /**
     * Check if pixel contains point
     */
    fun containsPoint(point: Vector3D, tolerance: Double = GEOMETRY_TOLERANCE): Boolean {
        return containsPoint(point.x, point.y, point.z, tolerance);
    }

    /**
     * Return number of detector pixel like SCxx-12
     */
    fun getDetectorNumber(): Int{
        return this.name.substring(2,4).toInt();
    }

    /**
     * Return number of pixel in detector like SC79-xx
     */
    fun getPixelNumber(): Int{
        return this.name.substring(5).toInt();
    }

    /**
     * Check if track crosses the pixel
     */
    fun isHit(track: Track): Boolean {
        //check central plane as well as upper and bottom planes of the layer
        val upperIntersection = upLayer.intersect(track);
        val bottomIntersection = bottomLayer.intersect(track);
        val upperHit = containsPoint(upperIntersection);
        val bottomHit = containsPoint(bottomIntersection);

//        return (upperHit||bottomHit||containsPoint(centralLayer.intersect(track))) && eff();
        if (!bottomHit && !upperHit) {
            return false;
        } else if (upperHit && bottomHit) {
            return eff();
        } else {
            val verticalHitPoint = when (upperHit) {
                true -> upperIntersection
                false -> bottomIntersection
            }
            val horizontalHitPoint = getHorizontalHitPoint(track);
            if (horizontalHitPoint == null) {
                //If horizontal intersection could not be found, it is near the rib and therefore length is always sufficient
                return true;
            } else {
                val length = verticalHitPoint.distance(horizontalHitPoint);
                return (length >= MINIMAL_TRACK_LENGTH) && eff();
            }
        }
    }

    fun getHorizontalHitPoint(track: Track): Vector3D? {
        for (p in sideLayers) {
            val intersection = p.intersection(track.line);
            //FIXME there is a problem with geometric tolerances here
            if (intersection != null && containsPoint(intersection, 100 * GEOMETRY_TOLERANCE)) {
                return intersection;
            }
        }
        return null;
    }

    private fun eff(): Boolean {
        return efficiency == 1.0 || rnd.nextDouble() < efficiency;
    }
}