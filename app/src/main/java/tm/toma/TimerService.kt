package tm.toma

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import java.util.*
import kotlin.properties.Delegates

enum class Commands {
    PUBLISH_STATE, ALTER_STATE
}

enum class ActivityDurations(val minutes: Minute) {
    WORK_DURATION(45), BREAK_DURATION(5)
}

class TimerService : Service() {

    private val TAG: String = javaClass.name

    private var mState: States by Delegates.observable(States.IDLE) { prop, old, new ->
        Log.i(TAG, "Got new state: $new, old state: $old")
        start(new)
    }

    private val mTimer: Timer = Timer()

    private val mCurrentStateIntent: Intent = Intent("currentState")

    private val mLocalBroadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(this)
    }

    private fun start(newState: States) {
        mTimer.cancel()
        mTimer.schedule(NotifyActivityCompleteTask(), when (newState) {
            States.WORK     -> milliSecs(ActivityDurations.WORK_DURATION.minutes)
            States.BREAK    -> milliSecs(ActivityDurations.BREAK_DURATION.minutes)
            else            -> 0
        })
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
            Commands.PUBLISH_STATE  -> publishState()
            Commands.ALTER_STATE    -> mState = intent.getSerializableExtra("newState") as States
        }
    }

    private fun publishState() {
        mCurrentStateIntent.putExtra("state", mState)
        mLocalBroadcastManager.sendBroadcast(mCurrentStateIntent)
    }
}

typealias Minute = Int

private fun milliSecs(minutes: Minute): Long = (minutes * 60 * 1000).toLong()