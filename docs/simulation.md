# Assumptions
- Muon tracks are straight lines
- All muons come from above
- There are no gaps between detectors
- Any muon with track length more than 1 cm in the detector could be registered
# Muon track parametrization
The track has 4 parameters:
- x coordinate in the central detector plane
- y coordinate in the central detector plane
- azimuthal angle $\varphi$ in range between 0 to 360 degrees
- zenith angle $\theta$ in range from 0 to 90 degrees
##Internal parametrization
Due to specifics of used libraries internal angle definition is slightly different:
- azimuthal angle $\varphi$ in range between $-\pi$ to $\pi$
- elevation angle $\theta$ in range from 0 to $-\pi/2$

The transformation from internal to external representation is made during result printing.
# Muon track generation
Track generation is made by `TrackGenerator` object. The default track generator uses uniform solid angle distribution and uniform (x,y) distribution:
- x is randomly distributed in central detector plane (only inside detector itself)
- the same for y
- azimuthal angle is randomly distributed from $-\pi$ to $\pi$
- elevation angle is distributed as $\pi/2 - acos(p)$ where p is uniformly distributed
```java
class UniformTrackGenerator(val maxX: Double = 4 * PIXEL_XY_SIZE,
  val maxY: Double = 4 * PIXEL_XY_SIZE) : TrackGenerator {
    override fun generate(rnd: RandomGenerator): Track {
        val x = (1 - rnd.nextDouble() * 2.0) * maxX;
        val y = (1 - rnd.nextDouble() * 2.0) * maxY;
        val phi = (1 - rnd.nextDouble() * 2.0) * Math.PI;
        val theta = Math.PI / 2 - Math.acos(rnd.nextDouble());
        return makeTrack(x, y, theta, phi);
    }
}
```
## Fixed angle generator
Fixed angle generator uses random x and y but fixed angles:
```java
class FixedAngleGenerator(val phi: Double, val theta: Double,
                          val maxX: Double = 4 * PIXEL_XY_SIZE,
                          val maxY: Double = 4 * PIXEL_XY_SIZE) : TrackGenerator {
    override fun generate(rnd: RandomGenerator): Track {
        val x = (1 - rnd.nextDouble() * 2.0) * maxX;
        val y = (1 - rnd.nextDouble() * 2.0) * maxY;
        return makeTrack(x, y, theta, phi);
    }
}
```
# Pixel definition
Pixels geometry is defined in a following way:
- pixels centers are imported from the table `map-RMM110.sc16`
- pixel placement inside detector is made according to Almaz's script
Pixel parameters are:
```java
val GEOMETRY_TOLERANCE = 0.01;
val PIXEL_XY_SIZE = 125.0;
val PIXEL_Z_SIZE = 30.0;
val CENTRAL_LAYER_Z = 0.0;
val UPPER_LAYER_Z = 166.0;
val LOWER_LAYER_Z = -180.0;
```
All sizes are in mm.
# Event generation
An `Event` consists of track (which is just a wrapper for a straight line) and set of pixels being hit.
Pixel hit is defined in the following way:
- either up or bottom pixel plane has intersection with track.
- if both up or bottom plane has intersection than track length is automatically more than 1 cm.
- if up or bottom intersection is not present then we search for intersection with side wall of the detector and calculate track length inside the detector. If this track length is more than 1 cm, it is considered to be possible hit.

**Important:** Due to specifics of geometry tolerances if side wall intersection is near the detector rib it is not always possible to find inside pixel. In such cases assume that intersection is close to the up or bottom plane and therefore path length is large.

```java
fun isHit(track: Track): Boolean {
    //check central plane as well as upper and bottom planes of the layer
    val upperIntersection = upLayer.intersect(track);
    val bottomIntersection = bottomLayer.intersect(track);
    val upperHit = containsPoint(upperIntersection);
    val bottomHit = containsPoint(bottomIntersection);


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
            return length >= MINIMAL_TRACK_LENGTH;
        }
    }
}
```
# Average direction
In order to average over muon tracks we take the direction unit vector from each track and calculate vector average
$$\left<r\right> = \frac{1}{N} \sum_{i=1..N}{r_i}$$
The error for average direction could be calculated in a following way:
$$\sigma_r = \sqrt{(r-\left<r\right>)^2}=\sqrt{\left<r^2\right> - \left<r\right>^2 }= \sqrt{1 - \left<r\right>^2}
