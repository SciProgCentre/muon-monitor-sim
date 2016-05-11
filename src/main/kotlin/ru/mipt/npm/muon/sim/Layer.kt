package ru.mipt.npm.muon.sim

import org.apache.commons.math3.geometry.euclidean.threed.Plane
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import java.util.*

/**
 * Created by darksnake on 09-May-16.
 */

class Layer(val name: String = "", val z: Double) {
    val layerPlane: Plane = Plane(Vector3D(0.0, 0.0, z), Vector3D(0.0, 0.0, 1.0), GEOMETRY_TOLERANCE);

    fun intersect(track: Track): Vector3D {
        return layerPlane.intersection(track.line);
    }
}
