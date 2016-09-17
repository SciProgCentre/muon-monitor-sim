package ru.mipt.npm.muon.sim

import org.apache.commons.cli.CommandLine
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.zip.ZipFile

/**
 * Reading Almaz experimental frequency files
 * Created by darksnake on 19-Jul-16.
 */

fun readData(stream: InputStream): Map<String, Int> {
    val res = HashMap<String, Int>();

    val dataFile = stream.bufferedReader();

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

fun evalData(cli: CommandLine) {

    val n = cli.getOptionValue("n", "100000").toInt();

    val dataFileName = cli.getOptionValue("d","data.zip");
    val multiplicity = cli.getOptionValue("m","-1").toInt();

    val outStream = outputStream(cli);

    println("Reading experiment data");
    val data: Map<String, Int> = if (dataFileName.endsWith("zip")) {
        val zipFile = ZipFile(dataFileName);
        //read first element from the zip file
        readData(zipFile.getInputStream(zipFile.entries().nextElement()))
    } else {
        readData(File(dataFileName).inputStream());
    }

    println("Staring simulation");
    val simResults = simulateN(n);

    println("printing results");
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