package ru.mipt.npm.muon.sim

import java.util.*

/**
 * Created by darksnake on 11-May-16.
 */

fun buildEventByTrack(track: Track, hitResolver: (Track) -> List<Pixel> = defaultHitResolver): Event {
    return Event(track, hitResolver.invoke(track))
}



data class Event(val track: Track, val hits: List<Pixel>){
    /**
     * Ge unique identity for given set of hits. One identity could correspond to different tracks
     */
    fun getIdentity(): String{
        return hits.joinToString(separator = ", ", prefix = "[", postfix = "]");
    }

}

val defaultHitResolver: (Track) -> List<Pixel> = { track: Track ->
    val hits = HashSet<Pixel>();
    for (p in pixels.values) {
        if (p.isHit(track)) {
            hits.add(p);
        }
    }
    hits.sortedBy { it -> it.name }
}