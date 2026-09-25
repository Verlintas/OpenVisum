package verlintas.openvisum.core.player.equalizer

import org.videolan.libvlc.MediaPlayer

object EqualizerPresets {

    val presetNames: List<String>
        get() = List(MediaPlayer.Equalizer.getPresetCount()) { index ->
            MediaPlayer.Equalizer.getPresetName(index)
        }

    val bandCount: Int
        get() = MediaPlayer.Equalizer.getBandCount()

    fun bandFrequency(index: Int): Float = MediaPlayer.Equalizer.getBandFrequency(index)

    fun amplitudesForPreset(index: Int): List<Float> {
        val equalizer = MediaPlayer.Equalizer.createFromPreset(index)
        return List(bandCount) { band -> equalizer.getAmp(band) }
    }
}
