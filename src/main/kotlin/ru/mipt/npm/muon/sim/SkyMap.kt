package ru.mipt.npm.muon.sim

import org.apache.commons.math3.distribution.EnumeratedRealDistribution
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipFile

/**
 * Methods to work with 2d angle histogram
 * Created by darksnake on 05-Mar-17.
 */

/**
 * Both theta and phi in degrees. Theta is zenith angle
 */
data class SkyMapEntry(val theta: Double, val phi: Double, val value: Double,
                       val thetaSize: Double = Math.PI / 180.0,
                       val phiSize: Double = Math.PI / 180.0);

internal fun loadMap(distributionFile: File): List<SkyMapEntry> {
    val rows = ArrayList<SkyMapEntry>();
    distributionFile.forEachLine {
        if (!it.startsWith("#")) {
            val split = it.split("\\s+".toPattern());
            rows.add(SkyMapEntry(split[0].toDouble(), split[1].toDouble(), split[2].toDouble()));
        }
    }
    return rows;
}

/**
 * Generating empirical distribution from a given file
 * @param angleStep pixel size in degrees
 */
class EmpiricalDistributionTrackGenerator(val rows: List<SkyMapEntry>, val maxX: Double = 4 * PIXEL_XY_SIZE, val maxY: Double = 4 * PIXEL_XY_SIZE, val angleStep: Double = 1.0) : TrackGenerator {

    private val distribution: EnumeratedRealDistribution;

    //initializing distribution
    init {
        val singletons = (0..rows.size - 1).map(Int::toDouble).toDoubleArray();
        val probs = rows.map { it.value }.toDoubleArray();
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

fun generateMap(eventNumber: Int = 1000000,
                trackGenerator: TrackGenerator = UniformTrackGenerator(),
                experimentData: Map<String, Int>? = null,
                step: Double = 1.0): List<SkyMapEntry> {
    fun convert(d: Double): Double = Math.floor(d * 180.0 / Math.PI / step) * step + step / 2.0;

    val map = ConcurrentHashMap<Pair<Double, Double>, AtomicReference<Double>>();

    val simResult = simulateN(eventNumber, trackGenerator, true);

    var totalFactor = 0.0
    var sum = 0.0
    var sim = 0.0

    simResult.forEach {
        //weight for each event. If data map exists weight is the number of data hits. Otherwise, weight equals 1
        val factor = ((experimentData?.getOrElse(it.key) { 0 } ?: 1).toDouble()) / it.value.count
        if (factor > 0) {
            totalFactor += factor
            it.value.events.forEach {
                sim++
                val coords = Pair(convert(it.track.getTheta()), convert(it.track.getPhi()));
                if (!map.containsKey(coords)) {
                    map.put(coords, AtomicReference<Double>(0.0))
                }
                map[coords]?.updateAndGet { it + factor };
                sum += factor
            }
        }
    }

    println("The total number of simulated particles: $eventNumber")
    println("The total number of used simulated particles: $sim")
    println("The total weighting factor: $totalFactor")
    println("The integral: $sum")

    return map.map { entry -> SkyMapEntry(90 - entry.key.first, entry.key.second, entry.value.get()) }
}

fun generateMap(parameters: Map<String, String>) {
    val step = 5.0

    val n = parameters.getOrElse("num") { "100000" }.toInt();

    val outStream = outputStream(parameters);

    println("Reading experiment data");
    val data: Map<String, Int>? = if (parameters.containsKey("dataFile") || parameters.containsKey("useData")) {
        val dataFileName = parameters.getOrElse("dataFile") { "data.zip" };
        println("Using $dataFileName for source data");
        if (dataFileName.endsWith("zip")) {
            val zipFile = ZipFile(dataFileName);
            //read first element from the zip file
            readData(zipFile.getInputStream(zipFile.entries().nextElement()))
        } else {
            readData(File(dataFileName).inputStream());
        }
    } else {
        null
    }

    var map = generateMap(eventNumber = n, experimentData = data, step = step)

    outStream.println("# Differential flux using $n simulated muons")

    if (parameters.containsKey("secondIteration")) {
        println("Starting second iteration")
        outStream.println("# Empirical initial distribution")
        val generator = EmpiricalDistributionTrackGenerator(map, angleStep = step)
        map = generateMap(eventNumber = n, trackGenerator = generator, experimentData = data, step = step);
    } else {
        outStream.println("# Uniform initial distribution")
    }


    outStream.println("# theta\tphi\tprobability")
    map.forEach {
        outStream.println("${it.theta}\t${it.phi}\t${it.value}")
    }
}