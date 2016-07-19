package ru.mipt.npm.muon.sim

import java.util.*

/**
 * Created by darksnake on 11-May-16.
 */

fun buildEventByTrack(track: Track, hitResolver: (Track) -> Collection<Pixel> = defaultHitResolver): Event {
    return Event(track, hitResolver.invoke(track))
}


data class Event(val track: Track, val hits: Collection<Pixel>) {
    /**
     * The unique identity for given set of hits. One identity could correspond to different tracks
     */
    fun getIdentity(): String {
        return hits.sortedBy { it -> it.name }
                .joinToString(separator = ", ", prefix = "[", postfix = "]", transform = { pixel -> pixel.name });
    }

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