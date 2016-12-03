package tm.toma

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager

enum class Commands {
    PUBLISH_STATE
}

class TimerService : Service() {

    private var mState: States = States.IDLE

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
        when(intent.getSerializableExtra("what")) {
            Commands.PUBLISH_STATE ->   publishState()
        }
    }

    private fun publishState() {
        mCurrentStateIntent.putExtra("state", mState)
        mLocalBroadcastManager.sendBroadcast(mCurrentStateIntent)
    }
}
