package ru.mipt.npm.muon.sim

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.io.File
import java.io.PrintStream

/**
 * Created by darksnake on 17-Sep-16.
 */


fun main(args: Array<String>) {

    val options = Options().apply {
        addOption("o", "out", true, "Output directory. By default uses \"\\output\".")
        addOption("f", "file", true, "Output file name.")
        addOption("n", "num", true, "Number of simulated muons.")
        addOption("F", "format", true,
                "Format for simulation output. Can be one of  the list [table, raw, json]. By default uses table")
        addOption("d", "dataFile", true, "Input experiment data file")
        addOption("m", "multiplicity", true, "Filter output with given pixel multiplicity. By default uses -1 which menas no filtering is applyed")

    };


    if (args.isEmpty()) {
        HelpFormatter().printHelp(
                "muonsim <command> [options]",
                "Available commands are: [monitorFX, simulate, evalData, efficiency]",
                options,
                ""
        )
        return;
    }


    val clp = DefaultParser();

    val command = args[0];
    val truncateArgs = args.copyOfRange(0, args.size - 1);
    val cli = clp.parse(options, truncateArgs)

    when (command) {
        "monitorFX" -> MonitorFX.launch(MonitorFX::class.java,*truncateArgs);
        "simulate" -> runSimulation(cli);
        "evalData" -> evalData(cli);
        "efficiency" -> generateEfficiency(cli);
    }

}

fun outputStream(cli: CommandLine): PrintStream {
    val outputDir = cli.getOptionValue("o", "output");
    val fileName = cli.getOptionValue("file", "simulation.dat")
    if (!fileName.isEmpty()) {
        val outDir = File(outputDir);
        if(!outDir.exists()){
            outDir.mkdirs();
        }
        return PrintStream(File(outDir, fileName));
    } else {
        return System.out;
    }
}