package ru.mipt.npm.muon.sim

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import java.io.File
import java.io.PrintStream
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream

/**
 * Simulation controls
 * Created by darksnake on 11-May-16.
 */
class Simulation {
    var rnd: RandomGenerator = JDKRandomGenerator();
    var trackGenerator: TrackGenerator = UniformTrackGenerator();

    /**
     * Simulate single track and returns corresponding event
     */
    fun simulateOne(): Event {
        val track = trackGenerator.generate(rnd);
        return buildEventByTrack(track);
    }

    private fun eventToString(event: Event): String {
        return event.hits.sortedBy { it -> it.name }.joinToString(separator = ", ", prefix = "[", postfix = "]")
    }

    /**
     * Simulate n events and count them by identities
     */
    fun simulateN(n: Int): Map<String, Counter> {
        val map = ConcurrentHashMap<String, Counter>();
        //generating stream in parallel
        Stream.generate { -> simulateOne() }.limit(n.toLong()).parallel().forEach {
            val res = simulateOne();
            val id = res.getIdentity();
            if (!map.containsKey(id)) {
                map.put(id, Counter(id, res.hits.size))
            }
            map[id]?.putEvent(res);
        }
        return map
    }

    /**
     * A counter for events with specific set id
     * @param id : set id
     * @param multiplicity : number of pixels in set
     */
    class Counter(val id: String, val multiplicity: Int) {
        var count: Int = 0;
            private set
        private var sum: Vector3D = Vector3D(0.0, 0.0, 0.0);


        /**
         * Using center of mass for averaging
         */
        fun putEvent(event: Event) {
            count++;
            sum = sum.add(event.track.getDirection())
        }

        fun average(): Vector3D {
            return sum.scalarMultiply(1.0 / count);
        }

        /**
         * <(r-<r>)^2> = <r^2> - <r>^2 = 1 - <r>^2
         *
         */
        fun angleErr(): Double {
            return Math.sqrt(1.0 - average().normSq);
        }

        fun getMeanPhi(): Double {
            return sum.alpha;
        }

        fun getMeanTheta(): Double {
            return sum.delta;
        }

//        private var phiSum: Double = 0.0;
//        private var phi2Sum: Double = 0.0;
//        private var thetaSum: Double = 0.0;
//        private var theta2Sum: Double = 0.0;
//
//        fun getMeanPhi(): Double {
//            return phiSum / count - 2.0 * Math.PI;
//        }
//
//        fun getPhiErr(): Double {
//            return Math.sqrt(phi2Sum / count - Math.pow(phiSum / count, 2.0));
//        }
//
//        fun getMeanTheta(): Double {
//            return thetaSum / count;
//        }
//
//        fun getThetaErr(): Double {
//            return Math.sqrt(theta2Sum / count - Math.pow(getMeanTheta(), 2.0));
//        }
//
//        fun putEvent(event: Event) {
//            count++;
//            phiSum += event.track.getPhi() + 2.0 * Math.PI;
//            phi2Sum += Math.pow(event.track.getPhi() + 2.0 * Math.PI, 2.0);
//            thetaSum += event.track.getTheta();
//            theta2Sum += Math.pow(event.track.getTheta(), 2.0);
//        }
//
//        override fun toString(): String {
//            return String.format("%s: %d; phi = %.3f\u00B1%.3f; theta = %.3f\u00B1%.3f;",
//                    id, count, getMeanPhi(), getPhiErr(), getMeanTheta(), getThetaErr())
//        }

    }

}

interface TrackGenerator {
    fun generate(rnd: RandomGenerator): Track;
}

/**
 * A uniform generator with track bases distributed in square in central plane, uniform phi and cos theta
 */
class UniformTrackGenerator(val maxX: Double = 4 * PIXEL_XY_SIZE, val maxY: Double = 4 * PIXEL_XY_SIZE) : TrackGenerator {
    override fun generate(rnd: RandomGenerator): Track {
        val x = (1 - rnd.nextDouble() * 2.0) * maxX;
        val y = (1 - rnd.nextDouble() * 2.0) * maxY;
        val phi = (1 - rnd.nextDouble() * 2.0) * Math.PI;
        val theta = Math.acos(rnd.nextDouble());
        return makeTrack(x, y, theta, phi);
    }

}

fun main(args: Array<String>) {
    val sim = Simulation();
    val n = args.getOrElse(0, { i -> "100000" }).toInt();
    val fileName = args.getOrNull(1);

    var outStream: PrintStream;
    if (fileName != null) {
        outStream = PrintStream(File(fileName));
    } else {
        outStream = System.out;
    }
    println("Staring simulation with ${n} particles");

    outStream.printf("%s\t%s\t%s\t%s\t%s%n",
            "name", "simCounts", "phi", "theta", "angleErr");

    sim.simulateN(n).values.sortedByDescending { it.count }.forEach { counter ->
        // print only 3-s
//        if (entry.multiplicity <= 3) {
//            outStream.println(entry)
//        }
        if (counter.multiplicity <= 3) {
            outStream.printf("%s\t%d\t%.3f\t%.3f\t%.3f%n",
                    counter.id, counter.count, counter.getMeanPhi(),
                    counter.getMeanTheta(), counter.angleErr());
        }
    }
}