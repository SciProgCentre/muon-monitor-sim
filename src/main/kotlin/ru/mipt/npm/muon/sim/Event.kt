package ru.mipt.npm.muon.sim

import java.io.PrintStream
import java.util.*
import javax.json.Json
import javax.json.JsonArrayBuilder
import javax.json.JsonObject

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
fun printEventAsRaw(out: PrintStream, event: Event) {
    val hitMap = HashMap<Int, ArrayList<Int>>();
    for (pixel in event.hits) {
        val detector = pixel.getDetectorNumber();
        hitMap.computeIfAbsent(detector) { num -> ArrayList<Int>() }.add(pixel.getPixelNumber());
    }
    out.println("0 -1 -1 -1 -1 xxxxxxxxxxxxxxxx") // header raw
    for (entry in hitMap) {
        out.print("1 ${entry.key} -1 -1 -1 ")
        for (i in (0..15)) {
            if (entry.value.contains(i)) {
                out.print("1");
            } else {
                out.print("0");
            }
        }
        out.println();
    }
    val phi = event.track.getPhi() * 180 / Math.PI + 180;
    val theta = 90 - event.track.getTheta() * 180 / Math.PI;
    out.println("9 ${theta} ${phi} ${event.track.getX()} ${event.track.getY()} xxxxxxxxxxxxxxxx") // track data
}

/**
 * Convert event to JSON
 */
fun eventAsJson(event: Event): JsonObject {
    val phi = event.track.getPhi() * 180 / Math.PI + 180;
    val theta = 90 - event.track.getTheta() * 180 / Math.PI;


    val hitArray: JsonArrayBuilder = Json.createArrayBuilder()
    for (hit in event.hits) {
        hitArray.add(Json.createObjectBuilder()
                .add("det", hit.getDetectorNumber())
                .add("pix", hit.getPixelNumber()))
    }
    return Json.createObjectBuilder()
            .add("theta", theta)
            .add("phi", phi)
            .add("x", event.track.getX())
            .add("y", event.track.getY())
            .add("hits", hitArray)
            .build();
}