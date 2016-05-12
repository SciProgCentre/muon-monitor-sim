package ru.mipt.npm.muon.sim

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.apache.commons.math3.util.IntegerSequence
import java.util.*

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
        return event.hits.joinToString(separator = ", ", prefix = "[", postfix = "]")
    }

    /**
     * Simulate n events and count them by identities
     */
    fun simulateN(n: Int): Map<String, Int> {
        val map = HashMap<String, IntegerSequence.Incrementor>();
        for (i in 1..n) {
            val res = simulateOne();
            val id = res.getIdentity();
            if (!map.containsKey(id)) {
                map.put(id, IntegerSequence.Incrementor.create().withMaximalCount(Int.MAX_VALUE))
            }
            map[id]?.increment();
        }
        return map.mapValues { entry -> entry.value.count }
    }

}

interface TrackGenerator {
    fun generate(rnd: RandomGenerator): Track;
}

/**
 * A uniform generator with track bases distributed in square in central plane, uniform phi and cos theta
 */
class UniformTrackGenerator(val maxX: Double = 2 * PIXEL_XY_SIZE, val maxY: Double = 2 * PIXEL_XY_SIZE) : TrackGenerator {
    override fun generate(rnd: RandomGenerator): Track {
        val x = (1 - rnd.nextDouble() * 2.0) * maxX;
        val y = (1 - rnd.nextDouble() * 2.0) * maxY;
        val phi = rnd.nextDouble() * 2 * Math.PI;
        val theta = Math.acos(rnd.nextDouble());
        return makeTrack(x, y, theta, phi);
    }

}

fun main(args: Array<String>) {
    val sim = Simulation();

    sim.simulateN(1000000).forEach { entry -> println("${entry.key} : ${entry.value}") }
}