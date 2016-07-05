package ru.mipt.npm.muon.sim

import org.apache.commons.math3.geometry.euclidean.threed.Plane
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D

/**
 * A single pixel
 * Created by darksnake on 09-May-16.
 */
class Pixel(val name: String, val center: Vector3D,
            val xSize: Double = PIXEL_XY_SIZE, val ySize: Double = PIXEL_XY_SIZE, val zSize: Double = PIXEL_Z_SIZE) {
    val layer: Layer = findLayer(center.z);
    private val upLayer = Layer("${name}_up", center.z + zSize / 2.0);
    private val bottomLayer = Layer("${name}_bottom", center.z - zSize / 2.0);

    //TODO add efficiency
    fun containsPoint(x: Double, y: Double, z: Double): Boolean {
        return x <= this.center.x + this.xSize / 2.0 && x >= this.center.x - this.xSize / 2.0 &&
                y <= this.center.y + this.ySize / 2.0 && y >= this.center.y - this.ySize / 2.0 &&
                z <= this.center.z + this.zSize / 2.0 && z >= this.center.z - this.zSize / 2.0;
    }

    /**
     * Check if pixel contains point
     */
    fun containsPoint(point: Vector3D): Boolean {
        return containsPoint(point.x, point.y, point.z);
    }

    /**
     * Check if track crosses the pixel
     * TODO add track length analysis
     */
    fun isHit(track: Track): Boolean {
        //check central plane as well as upper and bottom planes of the layer
        return containsPoint(layer.intersect(track))||containsPoint(upLayer.intersect(track))||containsPoint(bottomLayer.intersect(track));
    }
}