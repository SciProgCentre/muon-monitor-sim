package ru.mipt.npm.muon.sim.scripts

import ru.mipt.npm.muon.sim.simulateN

/**
 * Created by darksnake on 13.06.2017.
 */


//A temporary solution because scripts do not work
fun main(vararg pars:String){
    val n = 1e6.toInt()
    val ratio = simulateN(n).values.filter { it.multiplicity >= 3 }.sumBy { it.count }.toDouble() / n

    println("The total solid angle is ${ratio}*2*Pi")
}