package ru.mipt.npm.muon.sim

import org.apache.commons.math3.geometry.euclidean.threed.Line
import org.apache.commons.math3.geometry.euclidean.threed.Plane
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import org.apache.commons.math3.random.RandomGenerator

/**
 * Created by darksnake on 11-May-16.
 */

private val basePlane = Plane(Vector3D(0.0, 0.0, 0.0), Vector3D(0.0, 0.0, 1.0), GEOMETRY_TOLERANCE);

class Track(val line: Line) {

    /**
     * elevation angle
     */
    fun getTheta(): Double{
        return line.direction.delta;
    }

    /**
     * Azimuthal angle
     */
    fun getPhi(): Double{
        return line.direction.alpha;
    }

    /**
     *  x of intersection with base (central) plane
     */
    fun getX(): Double{
        return basePlane.intersection(line).x;
    }

    fun getY(): Double{
        return basePlane.intersection(line).y;
    }
}

fun makeTrack(start: Vector3D, direction: Vector3D): Track {
    return Track(Line(start, start.add(direction), GEOMETRY_TOLERANCE))
}

fun makeTrack(x: Double, y: Double, theta: Double, phi: Double): Track {
    //TODO check angle definitions
    return makeTrack(Vector3D(x, y, CENTRAL_LAYER_Z), Vector3D(phi, theta))
}

interface TrackGenerator {
    fun generate(rnd: RandomGenerator): Track;
}

/**
 * A uniform generator with track bases distributed in square in central plane, uniform phi and cos theta
 */
class UniformTrackGenerator(val maxX: Double = 2 * PIXEL_XY_SIZE, val maxY: Double = 2 * PIXEL_XY_SIZE) : TrackGenerator {
    override fun generate(rnd: RandomGenerator): Track {
        val x = (1 - rnd.nextDouble()*2.0)*maxX;
        val y = (1 - rnd.nextDouble()*2.0)*maxY;
        val phi = rnd.nextDouble()*2*Math.PI;
        val theta = Math.acos(rnd.nextDouble());
        return makeTrack(x,y,theta,phi);
    }

}