package verlintas.openvisum.core.player

import verlintas.openvisum.core.common.util.LanguageUtils
import verlintas.openvisum.core.player.model.PlayerTrack

object TrackSelector {

    fun selectAudio(
        tracks: List<PlayerTrack>,
        preferredLanguages: List<String>,
    ): PlayerTrack? {
        val candidates = tracks.filter { it.id >= 0 }
        if (candidates.isEmpty()) return null
        val preferred = preferredLanguages.mapNotNull { LanguageUtils.normalize(it) }
        val scored = candidates.map { track -> track to score(track, preferred) }
        val best = scored.filter { it.second > 0 }.maxByOrNull { it.second }
        if (best != null) return best.first
        return candidates.firstOrNull { !it.language.isNullOrBlank() } ?: candidates.first()
    }

    fun selectSubtitle(
        tracks: List<PlayerTrack>,
        preferredLanguages: List<String>,
    ): PlayerTrack? {
        val candidates = tracks.filter { it.id >= 0 }
        if (candidates.isEmpty() || preferredLanguages.isEmpty()) return null
        val preferred = preferredLanguages.mapNotNull { LanguageUtils.normalize(it) }
        return candidates
            .map { track -> track to score(track, preferred) }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun score(track: PlayerTrack, preferred: List<String>): Int {
        val language = LanguageUtils.normalize(track.language) ?: return 0
        val index = preferred.indexOfFirst { pref ->
            language == pref || language.startsWith("$pref-") || pref.startsWith("$language-")
        }
        if (index < 0) return 0
        return 1000 - index
    }
}
