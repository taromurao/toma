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
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.LocalBroadcastManager
import layout.WorkOrBreakFragment

class MainActivity : AppCompatActivity(), WorkOrBreakFragment.OnFragmentInteractionListener {

    private val mLocalBroadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(this)
    }

    private val mCurrentStateIntentFilter: IntentFilter = IntentFilter("currentState")

    private val mStateBroadcastReceiver: StateBroadcastReceiver by lazy {
        StateBroadcastReceiver(this)
    }

    val mWorkOrBreakFragment: WorkOrBreakFragment = WorkOrBreakFragment()

    private val mRequestCurrentStateIntent: Intent by lazy {
        val intent = Intent(this, TimerService::class.java)
        intent.putExtra("what", Commands.PUBLISH_STATE)
    }

    override fun onFragmentInteraction(uri: Uri) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        mLocalBroadcastManager.registerReceiver(mStateBroadcastReceiver, mCurrentStateIntentFilter)
        requestCurrentState()
    }

    override fun onPause() {
        super.onPause()
        mLocalBroadcastManager.unregisterReceiver(mStateBroadcastReceiver)
    }

    private fun requestCurrentState() { startService(mRequestCurrentStateIntent) }

}

class StateBroadcastReceiver(val mActivity: MainActivity): BroadcastReceiver() {

    private val mHandler: Handler by lazy { Handler(mActivity.mainLooper) }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            setFragment(when (intent.getSerializableExtra("state")) {
                States.IDLE -> mActivity.mWorkOrBreakFragment
                else        -> mActivity.mWorkOrBreakFragment
            })
        }
    }

    private fun setFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = mActivity.supportFragmentManager.beginTransaction()
        mHandler.post {
            transaction.replace(R.id.activity_main, fragment)
            transaction.commit()
        }
    }
}
