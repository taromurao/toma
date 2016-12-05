package tm.toma

import android.app.Service
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import java.util.*
import kotlin.properties.Delegates

enum class Commands { PUBLISH_STATE, ALTER_STATE }

enum class ActivityDurations(val minutes: Float) { WORK_DURATION(0.01F), BREAK_DURATION(0.01F) }

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
        publishState()
        mTimer.schedule(NotifyActivityCompleteTask(this), when (newState) {
            States.WORK     -> milliSecs(ActivityDurations.WORK_DURATION)
            States.BREAK    -> milliSecs(ActivityDurations.BREAK_DURATION)
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

    private fun milliSecs(duration: ActivityDurations): Long = (duration.minutes * 60 * 1000).toLong()

    class NotifyActivityCompleteTask(val mTimerService: TimerService) : TimerTask() {

        private val TAG: String = javaClass.name

        private val ringIntent by lazy {
            val intent = Intent(mTimerService, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("activityCompleted", true)
        }

        override fun run() {
            mTimerService.startActivity(ringIntent)
        }
    }
}


