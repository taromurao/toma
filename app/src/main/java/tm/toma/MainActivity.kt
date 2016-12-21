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
import layout.StopFragment
import layout.WorkOrBreakFragment

class MainActivity : AppCompatActivity(), WorkOrBreakFragment.OnFragmentInteractionListener,
        StopFragment.OnFragmentInteractionListener, Loggable {

    private val sLocalBroadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(this) }

    private val sCurrentStateIntentFilter: IntentFilter = IntentFilter(CURRENT_STATE)

    private val sStateBroadcastReceiver: StateBroadcastReceiver by lazy {
        StateBroadcastReceiver(this) }

    val workOrBreakFragment: WorkOrBreakFragment = WorkOrBreakFragment()

    val mStopFragment: StopFragment = StopFragment.newInstance()

    private val sRequestCurrentStateIntent: Intent by lazy {
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
        sLocalBroadcastManager.registerReceiver(sStateBroadcastReceiver, sCurrentStateIntentFilter)
        toggleMainActivityActive(true)
        requestCurrentState()
    }

    override fun onPause() {
        super.onPause()
        toggleMainActivityActive(false)
        sLocalBroadcastManager.unregisterReceiver(sStateBroadcastReceiver)
    }

    private fun requestCurrentState() { startService(sRequestCurrentStateIntent) }

    private fun toggleMainActivityActive(active: Boolean) {
        startService(
                Intent(this, TimerService::class.java)
                        .putExtra("command", Commands.TOGGLE_MAIN_ACTIVITY_ACTIVE)
                        .putExtra("active", active))
    }

}

class StateBroadcastReceiver(val sActivity: MainActivity?): BroadcastReceiver() {

    val sHandler: Handler? by lazy { Handler(sActivity?.mainLooper) }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && sActivity != null) {
            setFragment(when (intent.getSerializableExtra("state")) {
                States.IDLE -> sActivity.workOrBreakFragment
                else        -> sActivity.mStopFragment
            })
        }
    }

    private fun setFragment(fragment: Fragment) {
        if (sActivity !=  null) {
            val shouldReplace: Boolean =
                    !(sActivity.supportFragmentManager
                            .findFragmentByTag(fragment.javaClass.name)?.isVisible ?: false)

            if (shouldReplace)
                sHandler?.post { sActivity.supportFragmentManager.beginTransaction()
                        .replace(R.id.container, fragment, fragment.javaClass.name)
                        .commit() }
        }
    }
}
