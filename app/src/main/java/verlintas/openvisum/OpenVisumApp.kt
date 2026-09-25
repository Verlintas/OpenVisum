package verlintas.openvisum

import android.app.Application
import android.content.Context
import verlintas.openvisum.core.player.PlaybackEngine
import verlintas.openvisum.core.player.VlcPlaybackEngine

class OpenVisumApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val playbackEngine: PlaybackEngine by lazy { VlcPlaybackEngine(appContext) }
}
