package layout

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import tm.toma.Commands

import tm.toma.R
import tm.toma.States
import tm.toma.TimerService

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [WorkOrBreakFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [WorkOrBreakFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WorkOrBreakFragment : Fragment() {

    private val mStartWorkIntent: Intent by lazy {
        val intent = Intent(this.context, TimerService::class.java)
        intent.putExtra("command", Commands.ALTER_STATE)
        intent.putExtra("newState", States.WORK)
    }

    private val mStartBreakIntent: Intent by lazy {
        val intent = Intent(this.context, TimerService::class.java)
        intent.putExtra("command", Commands.ALTER_STATE)
        intent.putExtra("newState", States.BREAK)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view: View = inflater!!.inflate(R.layout.fragment_work_or_break, container, false)
        view.findViewById(R.id.workButton).setOnClickListener { context?.startService(mStartWorkIntent) }
        view.findViewById(R.id.breakButton).setOnClickListener { context?.startService(mStartBreakIntent) }
        return view
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
         * @return A new instance of fragment WorkOrBreakFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(): WorkOrBreakFragment = WorkOrBreakFragment()
    }
}// Required empty public constructor
