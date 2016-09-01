package ru.mipt.npm.muon.sim

import java.io.File
import java.io.PrintStream
import java.util.*

/**
 * Reading Almaz experimental frequency files
 * Created by darksnake on 19-Jul-16.
 */

fun readData(fileName: String): Map<String, Int> {
    val res = HashMap<String, Int>();
    val dataFile = File(fileName);

    var count: Int = 0;
    var names = HashSet<String>();
    var blockBegin = true

    dataFile.forEachLine { line ->
        if (blockBegin) {
            count = line.split(' ')[1].toInt();
            blockBegin = false
        } else if (line.startsWith('#')) {
            res.put(names.sorted().joinToString(separator = ", ", prefix = "[", postfix = "]"), count);
            names.clear();
            blockBegin = true;
        } else {
            names.add("SC" + line.split(' ')[0])
        }

    }
    return res;
}

fun main(args: Array<String>) {
    val dataFileName = args[0];
    val n = args.getOrElse(1, { i -> "1000000" }).toInt();
    val fileName = args.getOrNull(2);
    val multiplicity = args.getOrElse(3, { i -> "3" }).toInt();

    println("Reading experiment data");
    val data = readData(dataFileName);

    println("Staring simulation");
    val simResults = simulateN(n);

    println("printing results");
    var outStream: PrintStream;
    if (fileName != null) {
        outStream = PrintStream(File(fileName));
    } else {
        outStream = System.out;
    }
    outStream.printf("%s\t%s\t%s\t%s\t%s\t%s%n",
            "name", "dataCounts", "simCounts", "phi",
            "theta", "angleErr");
    data.forEach { entry ->
        if (simResults.containsKey(entry.key)) {
            val counter = simResults[entry.key]!!;
            if (multiplicity < 0 || counter.multiplicity == multiplicity) {
                outStream.printf("%s\t%d\t%d\t%.3f\t%.3f\t%.3f%n",
                        entry.key, entry.value, counter.count, counter.getMeanPhi(),
                        Math.PI / 2 - counter.getMeanTheta(), counter.angleErr());
            }
        }
    }
}