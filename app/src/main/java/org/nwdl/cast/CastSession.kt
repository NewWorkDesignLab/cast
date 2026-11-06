package org.nwdl.cast

import android.content.Context
import android.content.Intent
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.audio.ScreenAudioCapturer
import io.livekit.android.room.Room
import io.livekit.android.room.participant.VideoTrackPublishDefaults
import io.livekit.android.room.track.CustomVideoPreset
import io.livekit.android.room.track.LocalAudioTrack
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.LocalVideoTrackOptions
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoCaptureParameter
import io.livekit.android.room.track.VideoCodec
import io.livekit.android.room.track.VideoEncoding
import io.livekit.android.room.track.screencapture.ScreenCaptureParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Casts what the wearer sees (screen share), with microphone and app sound, to the cast room. Lives as long as the process. */
object CastSession {
    sealed interface State {
        data object Idle : State
        data object Starting : State
        data object Casting : State
        data class Failed(val reason: String) : State
    }

    private const val FPS = 60
    private const val APP_SOUND_GAIN = 0.5f

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    private var room: Room? = null

    // Three layers so that the server can give each viewer what its connection and tile size allow.
    private val roomOptions = RoomOptions(
        dynacast = true,
        screenShareTrackCaptureDefaults = LocalVideoTrackOptions(
            isScreencast = true,
            captureParams = VideoCaptureParameter(1280, 720, FPS),
        ),
        screenShareTrackPublishDefaults = VideoTrackPublishDefaults(
            videoEncoding = VideoEncoding(3_000_000, FPS),
            videoCodec = VideoCodec.H264.codecName, // hardware encoder on the Quest
            simulcast = true,
            simulcastLayers = listOf(
                CustomVideoPreset(VideoCaptureParameter(320, 180, 30), VideoEncoding(300_000, 30)),
                CustomVideoPreset(VideoCaptureParameter(640, 360, FPS), VideoEncoding(1_200_000, FPS)),
            ),
        ),
    )

    /** [projectionData] is the result of the MediaProjection consent dialog. */
    fun start(context: Context, projectionData: Intent) {
        if (_state.value == State.Starting || _state.value == State.Casting) return
        _state.value = State.Starting
        val appContext = context.applicationContext
        scope.launch {
            try {
                val config = DeviceConfig.load(appContext).getOrThrow()
                val (url, token) = withContext(Dispatchers.IO) { config.fetchToken() }
                CastService.start(appContext)
                val room = LiveKit.create(appContext, roomOptions).also { room = it }
                room.connect(url, token)
                val local = room.localParticipant
                local.setScreenShareEnabled(true, ScreenCaptureParams(projectionData, onStop = { stop(appContext) }))
                local.setMicrophoneEnabled(true)
                mixAppSound(room)
                _state.value = State.Casting
            } catch (e: Exception) {
                release(appContext)
                _state.value = State.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun stop(context: Context) {
        release(context.applicationContext)
        _state.value = State.Idle
    }

    // App sound goes into the microphone track; the viewer plays both together.
    private fun mixAppSound(room: Room) {
        val local = room.localParticipant
        val screen = local.getTrackPublication(Track.Source.SCREEN_SHARE)?.track as? LocalVideoTrack ?: return
        val mic = local.getTrackPublication(Track.Source.MICROPHONE)?.track as? LocalAudioTrack ?: return
        val capturer = ScreenAudioCapturer.createFromScreenShareTrack(screen) ?: return
        capturer.gain = APP_SOUND_GAIN
        mic.setAudioBufferCallback(capturer)
    }

    private fun release(context: Context) {
        room?.let {
            (it.localParticipant.getTrackPublication(Track.Source.MICROPHONE)?.track as? LocalAudioTrack)?.setAudioBufferCallback(null)
            it.disconnect()
            it.release()
        }
        room = null
        CastService.stop(context)
    }
}
