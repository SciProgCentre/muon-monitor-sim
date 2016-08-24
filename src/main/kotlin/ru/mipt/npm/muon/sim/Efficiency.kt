package ru.mipt.npm.muon.sim

import java.io.File
import java.io.PrintStream

/**
 * Created by darksnake on 24-Aug-16.
 */


fun simulateSingleDirection(theta: Double, phi: Double, numCalls: Int = 10000,
                            predicate: (Counter) -> Boolean = { counter -> true }): Double {
    val sum = simulateN(numCalls, FixedAngleGenerator(phi, theta)).values
            .filter(predicate) // filter only specific events
            .sumBy { counter -> counter.count }
    return sum.toDouble() / numCalls; // calculate efficiency
}

fun updateProgress(progressPercentage: Double) {
    val width = 50 // progress bar width in chars

    print("\r[")
    var i = 0
    while (i <= (progressPercentage * width).toInt()) {
        print("=")
        i++
    }
    while (i < width) {
        print(" ")
        i++
    }
    print("]")
}

fun main(args: Array<String>) {
    val fileName = args.getOrNull(0);

    var outStream: PrintStream;
    if (fileName != null) {
        outStream = PrintStream(File(fileName));
    } else {
        outStream = System.out;
    }

    updateProgress(0.0);
    for (theta in 0..89 step 10) {
        for (phi in 0..360 step 10) {
            val eff = simulateSingleDirection(Math.PI / 180 * (90 - theta), Math.PI / 180 * (phi - 180)) { counter -> counter.multiplicity >=3 }
            outStream.printf("%d\t%d\t%g%n", theta, phi, eff);
        }
        updateProgress(theta.toDouble()/90);
    }

}