package layout

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import tm.toma.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [StopFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [StopFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class StopFragment : Fragment(), Loggable {
    private val mChooseWorkOrBreakIntent: Intent by lazy {
        val intent: Intent = Intent(this.context, TimerService::class.java)
        intent.putExtra("command", Commands.ALTER_STATE)
        intent.putExtra("newState", States.IDLE)
    }

    private val sBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver(), Loggable {
        override fun onReceive(context: Context?, intent: Intent?) {
            val remainingTime: String? = intent?.getStringExtra("time")
            if (remainingTime != null)
                (view?.findViewById(R.id.remainingTimeTextView) as TextView?)?.text = remainingTime
        }
    }

    private val sIntentFilter: IntentFilter = IntentFilter(REMAINING_TIME)

    // TODO: Rename and change types of parameters
    private var mParam1: String? = null
    private var mParam2: String? = null

    private var mListener: OnFragmentInteractionListener? = null

    private val sLocalBroadcastManager: LocalBroadcastManager by lazy {
        LocalBroadcastManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view: View = inflater!!.inflate(R.layout.fragment_stop, container, false)
        view.findViewById(R.id.stopButton).setOnClickListener {
            context.startService(mChooseWorkOrBreakIntent)
        }
        return view
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onResume() {
        super.onResume()
        sLogger.debug("Registering receiver")
        sLocalBroadcastManager.registerReceiver(sBroadcastReceiver, sIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        sLocalBroadcastManager.unregisterReceiver(sBroadcastReceiver)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context as OnFragmentInteractionListener?
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment StopFragment.
         */
        fun newInstance(): StopFragment {
            val fragment = StopFragment()
            return fragment
        }
    }
}// Required empty public constructor
