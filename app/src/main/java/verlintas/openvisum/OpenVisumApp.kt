/*
 * Copyright (C) 2026 Verlintas
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This file is part of OpenVisum.
 *
 * OpenVisum is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OpenVisum is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OpenVisum. If not, see <https://www.gnu.org/licenses/>.
 */

package verlintas.openvisum

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import verlintas.openvisum.core.data.MediaRepository
import verlintas.openvisum.core.data.NetworkRepository
import verlintas.openvisum.core.data.db.OpenVisumDatabase
import verlintas.openvisum.core.data.prefs.PreferencesRepository
import verlintas.openvisum.core.data.security.SecretCipher
import verlintas.openvisum.core.data.source.MediaStoreScanner
import verlintas.openvisum.core.data.source.SafFolderRepository
import verlintas.openvisum.core.data.source.SmbBrowser
import verlintas.openvisum.core.data.source.SubtitleFinder
import verlintas.openvisum.core.data.source.VideoFinder
import verlintas.openvisum.core.data.source.WebDavBrowser
import verlintas.openvisum.core.data.subtitle.OpenSubtitlesProvider
import verlintas.openvisum.core.data.subtitle.SubtitleSearchRepository
import verlintas.openvisum.core.player.PlaybackEngine
import verlintas.openvisum.core.player.receiver.DlnaReceiver
import verlintas.openvisum.dlna.DlnaRendererHost
import verlintas.openvisum.core.player.VlcPlaybackEngine

class OpenVisumApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    add(VideoFrameDecoder.Factory())
                    add(OkHttpNetworkFetcherFactory())
                }
                .crossfade(true)
                .build()
        }
    }
}

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: OpenVisumDatabase by lazy { OpenVisumDatabase.create(appContext) }

    val secretCipher: SecretCipher by lazy { SecretCipher() }

    val mediaRepository: MediaRepository by lazy {
        MediaRepository(
            context = appContext,
            database = database,
            scanner = MediaStoreScanner(appContext),
            safFolders = SafFolderRepository(appContext, database.safFolderDao()),
            subtitleFinder = SubtitleFinder(appContext),
            videoFinder = VideoFinder(appContext),
        )
    }

    val networkRepository: NetworkRepository by lazy {
        NetworkRepository(
            sourceDao = database.networkSourceDao(),
            streamDao = database.streamHistoryDao(),
            cipher = secretCipher,
            smbBrowser = SmbBrowser(),
            webDavBrowser = WebDavBrowser(),
        )
    }

    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(appContext, secretCipher)
    }

    val subtitleSearchRepository: SubtitleSearchRepository by lazy {
        SubtitleSearchRepository(
            providers = listOf(
                OpenSubtitlesProvider(appContext, preferencesRepository),
            ),
        )
    }

    val playbackEngine: PlaybackEngine by lazy { VlcPlaybackEngine(appContext) }

    val dlnaReceiver: DlnaReceiver by lazy {
        DlnaReceiver(appContext, DlnaRendererHost(appContext, playbackEngine))
    }
}
