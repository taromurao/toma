package tm.toma

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.app.NotificationCompat
import kotlin.properties.Delegates

enum class States { IDLE, WORK, BREAK }

enum class Commands { PUBLISH_STATE, ALTER_STATE, TOGGLE_MAIN_ACTIVITY_ACTIVE }

val REMAINING_TIME: String = "remainingTime"

val CURRENT_STATE: String = "currentState"

class TimerService : Service(), Loggable {

    val sConfigs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    val mMediaPlayer: MediaPlayer by lazy {
        val mp = MediaPlayer.create(this, R.raw.bell)
        mp.setLooping(true)
        mp
    }

    private var mState: States by Delegates.observable(States.IDLE) { _, old, new ->
        mLogger.debug("Got new state: {}, old state: {}", new, old)
        broadcastState()
        if (new in setOf(States.WORK, States.BREAK)) {
            mNotificationId = randomInt()
            startForeground(mNotificationId, mWorkOrBreakNotificationBuilder.build())
            startTask(new)
        } else {
            mTimer?.cancel()
            mNotificationManager.notify(mNotificationId, mIdleNotificationBuilder.build())
            if (mMediaPlayer.isPlaying) mMediaPlayer.pause()
            stopForeground(false)
        }
    }

    private var mMainActivityIsActive: Boolean = false

    private val mNotificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private var mTimer: CountDownTimer? = null

    private var mNotificationId: Int = 0

    private val mCurrentStateIntent: Intent = Intent(CURRENT_STATE)

    private val mRemainingTimeIntent: Intent = Intent(REMAINING_TIME)

    private val mLocalBroadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(this) }

    private fun alterStateAction(newState: States): NotificationCompat.Action {
        val (icon: Int, title: String) = when (newState) {
            States.IDLE -> Pair(R.drawable.ic_stop_black_24dp, getString(R.string.stop))
            States.WORK -> Pair(R.drawable.ic_play_arrow_black_24dp, getString(R.string.work))
            States.BREAK-> Pair(R.drawable.ic_free_breakfast_black_24dp, getString(R.string.pause))
        }

        val alterStateIntent: Intent = Intent(this, TimerService::class.java)
                .putExtra("command", Commands.ALTER_STATE)
                .putExtra("newState", newState)

        val notificationPendingIntent: PendingIntent =
                PendingIntent.getService(this, randomInt(), alterStateIntent, START_FLAG_REDELIVERY)

        return NotificationCompat.Action.Builder(icon, title, notificationPendingIntent).build()
    }

    private val mStartMainActivityPendingIntent: PendingIntent by lazy {
        PendingIntent.getService(this, 0, Intent(this, MainActivity::class.java), 0) }

    private val mWorkOrBreakNotificationBuilder: NotificationCompat.Builder by lazy { builder(null) }

    private val mIdleNotificationBuilder: NotificationCompat.Builder by lazy { builder(States.IDLE) }

    private fun builder(state: States?): NotificationCompat.Builder {
        val b: NotificationCompat.Builder = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_timer_black_24dp)
                .setContentTitle("Toma")
                .setContentIntent(mStartMainActivityPendingIntent)
        return when (state) {
            States.IDLE -> b.addAction(alterStateAction(States.WORK))
                    .addAction(alterStateAction(States.BREAK))
            else        -> b.addAction(alterStateAction(States.IDLE))
        }
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
                            mWorkOrBreakNotificationBuilder.setContentText(pretty(remaining)).build())
                }

                override fun onFinish() = ring()
            }.start()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) handleCommand(intent)
        return START_NOT_STICKY // START_NOT_STICKY avoids notification creation at application kill.
    }

    private fun handleCommand(intent: Intent) {
        if (0 in setOf(sConfigs.getInt(PREFS_BREAK_DURATION, 0), sConfigs.getInt(PREFS_BREAK_DURATION, 0)))
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        else
            when (intent.getSerializableExtra("command")) {
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

    private fun duration(state: States): Long {
        val mins: Int = sConfigs.getInt(when (state) {
            States.WORK -> PREFS_WORK_DURATION
            States.BREAK -> PREFS_BREAK_DURATION
            else -> ""
        }, 0)

        return (mins * 60 * 1000).toLong()
    }
}

private fun pretty(time: Long): String =
        listOf(minutes(time), secs(time)).joinToString(".") { it.toString().padStart(2, '0') }

private fun minutes(time: Long): Long = (time / 1000) / 60

private fun secs(time: Long): Long = (time / 1000) % 60

private fun randomInt(): Int = (System.currentTimeMillis()%10000).toInt()
