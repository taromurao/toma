package tm.toma

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.TextView
import layout.StopFragment
import layout.WorkOrBreakFragment

class MainActivity : AppCompatActivity(), WorkOrBreakFragment.OnFragmentInteractionListener,
        StopFragment.OnFragmentInteractionListener, Loggable {

    private val mLocalBroadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(this) }

    private val mCurrentStateIntentFilter: IntentFilter = IntentFilter(CURRENT_STATE)

    private val mRemainingTimeIntentFilter: IntentFilter = IntentFilter(REMAINING_TIME)

    private val mStateBroadcastReceiver: StateBroadcastReceiver by lazy {
        StateBroadcastReceiver(this) }

    private val mRemainingTimeBroadcastReceiver: RemainingTimeBroadcastReceiver by lazy {
        RemainingTimeBroadcastReceiver(this) }

    val mWorkOrBreakFragment: WorkOrBreakFragment = WorkOrBreakFragment()

    val mStopFragment: StopFragment = StopFragment()

    private val mRequestCurrentStateIntent: Intent by lazy {
        val intent = Intent(this, TimerService::class.java)
        intent.putExtra("command", Commands.PUBLISH_STATE)
    }

    override fun onFragmentInteraction(uri: Uri) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake up user device when inactive
        window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)
        val myToolbar: Toolbar = findViewById(R.id.my_toolbar) as Toolbar
        setSupportActionBar(myToolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_settings    -> {
                startActivity(Intent(this, SettingsActivity::class.java)); true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        mLocalBroadcastManager.registerReceiver(mStateBroadcastReceiver, mCurrentStateIntentFilter)
        mLocalBroadcastManager.registerReceiver(
                mRemainingTimeBroadcastReceiver, mRemainingTimeIntentFilter)
        toggleMainActivityActive(true)
        requestCurrentState()
    }

    override fun onPause() {
        super.onPause()
        toggleMainActivityActive(false)
        mLocalBroadcastManager.unregisterReceiver(mStateBroadcastReceiver)
        mLocalBroadcastManager.unregisterReceiver(mRemainingTimeBroadcastReceiver)
    }

    private fun requestCurrentState() { startService(mRequestCurrentStateIntent) }

    private fun toggleMainActivityActive(active: Boolean) {
        startService(
                Intent(this, TimerService::class.java)
                        .putExtra("command", Commands.TOGGLE_MAIN_ACTIVITY_ACTIVE)
                        .putExtra("active", active))
    }

}

abstract class PostableBroadcastReceiver(val mActivity: MainActivity?) : BroadcastReceiver() {
    protected val mHandler: Handler by lazy { Handler(mActivity?.mainLooper) }
}

class RemainingTimeBroadcastReceiver(mActivity: MainActivity?) :
        PostableBroadcastReceiver(mActivity), Loggable {
    override fun onReceive(context: Context?, intent: Intent?) {
        val remainingTime: String? = intent?.getStringExtra("time")
        if (remainingTime != null && mActivity != null)
            mHandler.post {
                (mActivity.findViewById(R.id.remainingTimeTextView) as TextView).text =
                        remainingTime }
    }
}

class StateBroadcastReceiver(mActivity: MainActivity): PostableBroadcastReceiver(mActivity) {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && mActivity != null) {
            setFragment(when (intent.getSerializableExtra("state")) {
                States.IDLE -> mActivity.mWorkOrBreakFragment
                else        -> mActivity.mStopFragment
            })
        }
    }

    private fun setFragment(fragment: Fragment) {
        if (mActivity !=  null) {
            val shouldReplace: Boolean =
                    !(mActivity.supportFragmentManager
                            .findFragmentByTag(fragment.javaClass.name)?.isVisible ?: false)

            if (shouldReplace)
                mHandler.post { mActivity.supportFragmentManager.beginTransaction()
                        .replace(R.id.container, fragment, fragment.javaClass.name)
                        .commit() }
        }
    }
}
