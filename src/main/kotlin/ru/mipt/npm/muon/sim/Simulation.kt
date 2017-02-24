package ru.mipt.npm.muon.sim

import org.apache.commons.math3.distribution.EnumeratedRealDistribution
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.json.Json
import javax.json.JsonObject
import javax.json.stream.JsonGenerator


/**
 * Simulate single track and returns corresponding event
 */
fun simulateOne(trackGenerator: TrackGenerator = UniformTrackGenerator()): Event {
    val track = trackGenerator.generate();
    return buildEventByTrack(track);
}

private fun eventToString(event: Event): String {
    return event.hits.sortedBy { it -> it.name }.joinToString(separator = ", ", prefix = "[", postfix = "]")
}

/**
 * Simulate n events and count them by identities
 */
fun simulateN(n: Int, trackGenerator: TrackGenerator = UniformTrackGenerator()): Map<String, Counter> {
    val map = ConcurrentHashMap<String, Counter>();
    //generating stream in parallel
    Stream.generate { -> simulateOne(trackGenerator) }.limit(n.toLong()).parallel().forEach {
        val id = it.toString();
        if (!map.containsKey(id)) {
            map.put(id, Counter(id, it.hits.size))
        }
        map[id]?.putEvent(it);
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

interface TrackGenerator {
    fun generate(): Track;
}

/**
 * A uniform generator with track bases distributed in square in central plane, uniform phi and cos theta
 */
class UniformTrackGenerator(val maxX: Double = 4 * PIXEL_XY_SIZE, val maxY: Double = 4 * PIXEL_XY_SIZE) : TrackGenerator {
    override fun generate(): Track {
        val x = (1 - rnd.nextDouble() * 2.0) * maxX;
        val y = (1 - rnd.nextDouble() * 2.0) * maxY;
        val phi = (1 - rnd.nextDouble() * 2.0) * Math.PI;
        val theta = Math.PI / 2 - Math.acos(rnd.nextDouble());
        return makeTrack(x, y, theta, phi);
    }
}

/**
 * Generating empirical distribution from a given file
 * @param angleStep pixel size in degrees
 */
class EmpiricalDistributionTrackGenerator(distributionFile: File, val maxX: Double = 4 * PIXEL_XY_SIZE, val maxY: Double = 4 * PIXEL_XY_SIZE, val angleStep: Double = 1.0) : TrackGenerator {

    private data class Row(val phi: Double, val theta: Double, val prob: Double);

    private val rows = ArrayList<Row>();
    private val distribution: EnumeratedRealDistribution;

    //reading data file
    init {
        distributionFile.forEachLine {
            if (!it.startsWith("#")) {
                val split = it.split("\\s+".toPattern());
                rows.add(Row(split[0].toDouble(), split[1].toDouble(), split[2].toDouble()));
            }
        }
        val singletons = (0..rows.size - 1).map(Int::toDouble).toDoubleArray();
        val probs = rows.map { it.prob }.toDoubleArray();

        distribution = EnumeratedRealDistribution(singletons, probs);
    }

    override fun generate(): Track {
        //random x and y
        val x = (1 - rnd.nextDouble() * 2.0) * maxX;
        val y = (1 - rnd.nextDouble() * 2.0) * maxY;

        //get random row
        val row = rows[distribution.sample().toInt()];
        val theta = Math.PI / 180.0 * (90 - row.theta);
        val phi = Math.PI / 180.0 * (row.phi);

        //uniformly distributed angles inside pixels
        val dTheta = Math.PI / 180.0 * angleStep / 2.0 * (1.0 - 2 * rnd.nextDouble());
        val dPhi = Math.PI / 180.0 * angleStep / 2.0 * (1.0 - 2 * rnd.nextDouble());


        return makeTrack(x, y, theta + dTheta, phi + dPhi);
    }
}

class FixedAngleGenerator(val phi: Double, val theta: Double,
                          val maxX: Double = 4 * PIXEL_XY_SIZE,
                          val maxY: Double = 4 * PIXEL_XY_SIZE) : TrackGenerator {
    override fun generate(): Track {
        val x = (1 - rnd.nextDouble() * 2.0) * maxX;
        val y = (1 - rnd.nextDouble() * 2.0) * maxY;
        return makeTrack(x, y, theta, phi);
    }
}

/**
 * Generating surface distribution using accept-reject method
 */
class Cos2TrackGenerator(val power: Double = 2.0, val maxX: Double = 4 * PIXEL_XY_SIZE, val maxY: Double = 4 * PIXEL_XY_SIZE) : TrackGenerator {
    override fun generate(): Track {
        val x = (1 - rnd.nextDouble() * 2.0) * maxX;
        val y = (1 - rnd.nextDouble() * 2.0) * maxY;
        val phi = (1 - rnd.nextDouble() * 2.0) * Math.PI;


        for (i in 0..500) {
            val thetaCandidate = Math.acos(rnd.nextDouble());
            val u = rnd.nextDouble();
            val sin = Math.sin(thetaCandidate);
            if (u < Math.pow(sin, power) / sin) {
                return makeTrack(x, y, thetaCandidate, phi);
            }
        }
        throw RuntimeException("Failed to generate theta from distribution");
    }
}

fun directionMap(): Map<String, Vector3D> {
    val mapFile = File("direction.map");
    if (mapFile.exists()) {
        try {
            val ois = ObjectInputStream(mapFile.inputStream());
            val res = ois.readObject() as? Map<String, Vector3D>;
            if (res != null) {
                return res;
            }
        } catch (ex: Exception) {
            println("Failed to load direction map. Recalculating");
        }
    }

    val map = simulateN(1e7.toInt()).mapValues { entry -> entry.value.average() }
    val oos = ObjectOutputStream(mapFile.outputStream());
    oos.writeObject(map);
    oos.close()
    return map;
}

enum class outputType {
    table, raw, json
}

fun runSimulation(parameters: Map<String, String>) {
    val n = parameters.getOrElse("num") { "100000" }.toInt();

    val outputFormat = outputType.valueOf(parameters.getOrElse("format") { "table" });

    val outStream = outputStream(parameters);

    val multiplicity = parameters.getOrElse("multiplicity") { "-1" }.toInt();

    println("Staring simulation with $n particles");

    val generator: TrackGenerator = if (parameters.containsKey("generator")) {
        val generatorFile = parameters.get("generator");
        println("Using muon angle distribution from $generatorFile")
        EmpiricalDistributionTrackGenerator(File(generatorFile))
    } else {
        UniformTrackGenerator();
    }

    when (outputFormat) {
        outputType.table -> {
            outStream.printf("%s\t%s\t%s\t%s\t%s%n",
                    "name", "simCounts", "phi", "theta", "angleErr");

            simulateN(n, generator).values.sortedByDescending { it.count }.forEach { counter ->
                if (multiplicity < 0 || counter.multiplicity == multiplicity) {
                    outStream.printf("%s\t%d\t%.3f\t%.3f\t%.3f%n",
                            counter.id, counter.count, counter.getMeanPhi(),
                            Math.PI / 2 - counter.getMeanTheta(), counter.angleErr());
                }
            }
        }
        outputType.raw -> {
            Stream.generate { -> simulateOne(generator) }.limit(n.toLong()).forEach {
                printEventAsRaw(outStream, it)
            }
        }
        outputType.json -> {
            val json = Json.createArrayBuilder();
            Stream.generate { -> eventAsJson(simulateOne(generator)) }.parallel().limit(n.toLong())
                    .collect(Collectors.toList<JsonObject>()).forEach { it: JsonObject -> json.add(it) };
            val writer = Json.createWriterFactory(mapOf(JsonGenerator.PRETTY_PRINTING to true)).createWriter(outStream);
            writer.write(json.build())
        }
    }
}