package ru.mipt.npm.muon.sim

import javafx.application.Application
import org.apache.commons.cli.*
import ru.mipt.npm.muon.sim.app.MonitorApp
import java.io.File
import java.io.PrintStream
import java.util.*

/**
 * Created by darksnake on 17-Sep-16.
 */


fun main(args: Array<String>) {

    val options = Options().apply {
        addOption("o", "outputDir", true, "Output directory. By default uses \"\\output\".")
        addOption(Option.builder("f")
                .longOpt("outputFile")
                .hasArgs()
                .desc("Output file name.")
                .build()
        )
        addOption("n", "num", true, "Number of simulated muons.")
        addOption("F", "format", true,
                "Format for simulation output. Can be one of  the list [table, raw, json]. By default uses table")
        addOption("d", "dataFile", true, "Input experiment data file")
        addOption("g", "generator", true, "File with binned distribution for generator in format of \"phi theta prob\". If not present, then the uniform distribution is used.")
        addOption("m", "multiplicity", true, "Filter output with given pixel multiplicity. By default uses -1 which menas no filtering is applyed")
        addOption(null, "secondIteration", false, " Perform a second iteration afrer first one")
    };


    if (args.isEmpty()) {
        HelpFormatter().printHelp(
                "muonsim <command> [options]",
                "Available commands are: [monitorFX, simulate, evalData, efficiency, map]",
                options,
                ""
        )
        return;
    }


    val clp = DefaultParser();

    val command = args[0];
    val truncateArgs = args.copyOfRange(0, args.size);
    val cli = clp.parse(options, truncateArgs)

    val parameters = getParameters(cli);
    when (command) {
        "monitorFX" -> Application.launch(MonitorApp::class.java, *truncateArgs);
        "simulate" -> runSimulation(parameters);
        "evalData" -> evalData(parameters);
        "efficiency" -> generateEfficiency(parameters);
        "map" -> generateMap(parameters)
    }

}

fun getParameters(cli: CommandLine): Map<String, String> {
    val map = HashMap<String, String>();
    for (opt in cli) {
        map.put(opt.longOpt, opt.valuesList.joinToString(separator = " "))
    }
    return map;
}

fun outputStream(parameters: Map<String, String>): PrintStream {
    val outputDir = parameters.getOrElse("outputDir") { "output" };
    val fileName = parameters.getOrElse("outputFile") { "simulation.dat" }
    if (!fileName.isEmpty()) {
        val outDir = File(outputDir);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        return PrintStream(File(outDir, fileName));
    } else {
        return System.out;
    }
}