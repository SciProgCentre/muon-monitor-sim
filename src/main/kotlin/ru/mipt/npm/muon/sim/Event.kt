package ru.mipt.npm.muon.sim

import java.io.PrintStream
import java.util.*

/**
 * Created by darksnake on 11-May-16.
 */


data class Event(val track: Track, val hits: Collection<Pixel>) {
    /**
     * The unique identity for given set of hits. One identity could correspond to different tracks
     */
    fun getIdentity(): String {
        return hits.sortedBy { it -> it.name }
                .joinToString(separator = ", ", prefix = "[", postfix = "]", transform = { pixel -> pixel.name });
    }

}

fun buildEventByTrack(track: Track, hitResolver: (Track) -> Collection<Pixel> = defaultHitResolver): Event {
    return Event(track, hitResolver.invoke(track))
}

val defaultHitResolver: (Track) -> Collection<Pixel> = { track: Track ->
    val hits = HashSet<Pixel>();
    for (p in pixels.values) {
        if (p.isHit(track)) {
            hits.add(p);
        }
    }
    hits
}

/**
 * Print simulated event as raw experiment data
 */
fun printEventAsRaw(out: PrintStream, event: Event){
    val hitMap = HashMap<Int, ArrayList<Int>>();
    for(pixel in event.hits){
        val detector = pixel.getDetectorNumber();
        hitMap.computeIfAbsent(detector){num->ArrayList<Int>()}.add(pixel.getPixelNumber());
    }
    out.println("0 -1 -1 -1 -1 xxxxxxxxxxxxxxxx") // header raw
    for(entry in hitMap){
        out.print("1 ${entry.key} -1 -1 -1 ")
        for(i in (0..15)){
            if(entry.value.contains(i)){
                out.print("1");
            } else {
                out.print("0");
            }
        }
        out.println();
    }

}