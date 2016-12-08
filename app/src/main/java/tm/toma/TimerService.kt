package tm.toma

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.app.NotificationCompat
import kotlin.properties.Delegates

enum class Commands { PUBLISH_STATE, ALTER_STATE, TOGGLE_MAIN_ACTIVITY_ACTIVE }

val REMAINING_TIME: String = "remainingTime"

val CURRENT_STATE: String = "currentState"

class TimerService : Service(), Loggable {

    val mMediaPlayer: MediaPlayer by lazy {
        val mp = MediaPlayer.create(this, R.raw.bell)
        mp.setLooping(true)
        mp
    }

    private var mState: States by Delegates.observable(States.IDLE) { prop, old, new ->
        mLogger.debug("Got new state: {}, old state: {}", new, old)
        broadcastState()
        if (new in setOf(States.WORK, States.BREAK)) {
            mNotificationId = randomInt()
            startForeground(mNotificationId, mNotificationBuilder.build())
            startTask(new)
            mTimer?.start()
        } else {
            mTimer?.cancel()
            if (old in setOf(States.WORK, States.BREAK) && mMediaPlayer.isPlaying)
                mMediaPlayer.pause()
            stopForeground(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mNotificationManager.cancel(mNotificationId)
    }

    private var mMainActivityIsActive: Boolean = false

    private val mNotificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private var mTimer: CountDownTimer? = null

    private var mNotificationId: Int = 0

    private val mCurrentStateIntent: Intent = Intent(CURRENT_STATE)

    private val mRemainingTimeIntent: Intent = Intent(REMAINING_TIME)

    private val mLocalBroadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(this)
    }

    private fun alterStateAction(newState: States): NotificationCompat.Action {
        val (icon: Int, title: String) = when (newState) {
            States.IDLE -> Pair(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop))
            States.WORK -> Pair(android.R.drawable.ic_media_play, getString(R.string.work))
            States.BREAK-> Pair(android.R.drawable.ic_media_pause, getString(R.string.pause))
        }

        val alterStateIntent: Intent = Intent(this, TimerService::class.java)
                .putExtra("command", Commands.ALTER_STATE)
                .putExtra("newState", newState)

        val notificationPendingIntent: PendingIntent =
                PendingIntent.getService(this, 0, alterStateIntent, 0)

        return NotificationCompat.Action.Builder(icon, title, notificationPendingIntent).build()
    }

    private val mStartMainActivityPendingIntent: PendingIntent by lazy {
        PendingIntent.getService(this, 0, Intent(this, MainActivity::class.java), 0)
    }

    private val mWorkOrBreakNotificationBuilder: NotificationCompat.Builder

    private val mIdleNotificationBuilder: NotificationCompat.Builder

    private fun notificationBuilder(newState: States): NotificationCompat.Builder {
        return NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Toma")
                .addAction(alterStateAction(newState))
                .setContentIntent(mStartMainActivityPendingIntent)
    }

    private fun startTask(task: States) {
        mTimer?.cancel()
        if (task in setOf(States.WORK, States.BREAK)) {
            mTimer = object : CountDownTimer(duration(task), 1000) {
                override fun onTick(remaining: Long) {
                    mLogger.debug("Remaining time is {}", pretty(remaining))
                    broadcastRemainingTime(pretty(remaining))
                    mNotificationManager.notify(
                            mNotificationId,
                            mNotificationBuilder.setContentText(pretty(remaining)).build())
                }

                override fun onFinish() = ring()
            }
        }
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

    private fun broadcastRemainingTime(remainingTime: String) {
        mLogger.debug("Broadcasting remainig time: $remainingTime")
        mRemainingTimeIntent.putExtra("time", remainingTime)
        mLocalBroadcastManager.sendBroadcast(mRemainingTimeIntent)
    }

    private fun ring() {
        if (!mMediaPlayer.isPlaying) mMediaPlayer.start()
        if (!mMainActivityIsActive)
            startActivity(Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun duration(state: States): Long {
    return (when (state) {
        States.BREAK -> 5F
        States.WORK -> 45F
        else -> 0F
    } * 60 * 1000).toLong()
}

private fun pretty(time: Long): String {
    val min: Long = (time / 1000) / 60
    val sec: Long = (time / 1000) % 60
    return "$min:$sec"
}

private fun randomInt(): Int = (System.currentTimeMillis()%10000).toInt()
