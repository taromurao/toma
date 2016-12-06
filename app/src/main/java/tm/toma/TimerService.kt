package tm.toma

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import java.util.*
import kotlin.properties.Delegates

enum class Commands { PUBLISH_STATE, ALTER_STATE, TOGGLE_MAIN_ACTIVITY_ACTIVE }

class TimerService : Service(), Loggable {

    val mMediaPlayer: MediaPlayer by lazy {
        val mp = MediaPlayer.create(this, R.raw.bell)
        mp.setLooping(true)
        mp
    }

    private var mState: States by Delegates.observable(States.IDLE) { prop, old, new ->
        mLogger.debug("Got new state: {}, old state: {}", new, old)
        broadcastState()
        if (new in setOf(States.WORK, States.BREAK))
            mTimer.schedule(NotifyActivityCompleteTask(this), duration(new))
        else
            mTimer.cancel()
            if (old in setOf(States.WORK, States.BREAK)) {
                if (mMediaPlayer.isPlaying) mMediaPlayer.pause()
            }
    }

    private var mMainActivityIsActive: Boolean = false

    private val mTimer: Timer = Timer()

    private val mCurrentStateIntent: Intent = Intent("currentState")

    private val mLocalBroadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) handleCommand(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleCommand(intent: Intent) {
        when(intent.getSerializableExtra("command")) {
            Commands.PUBLISH_STATE  -> broadcastState()
            Commands.ALTER_STATE    -> mState = intent.getSerializableExtra("newState") as States
            Commands.TOGGLE_MAIN_ACTIVITY_ACTIVE -> mMainActivityIsActive = intent.getBooleanExtra("active", false)
        }
    }

    private fun broadcastState() {
        mCurrentStateIntent.putExtra("state", mState)
        mLocalBroadcastManager.sendBroadcast(mCurrentStateIntent)
    }

    class NotifyActivityCompleteTask(val mTimerService: TimerService) : TimerTask(), Loggable {
        override fun run() {
            if (!mTimerService.mMediaPlayer.isPlaying) mTimerService.mMediaPlayer.start()
            if (!mTimerService.mMainActivityIsActive)
                mTimerService.startActivity(Intent(mTimerService, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

private fun duration(state: States): Long {
    return (when (state) {
        States.BREAK -> 0.3F
        States.WORK -> 45F
        else -> 0F
    } * 60 * 1000).toLong()
}

