package com.wacinfo.wacextrathaiid

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.wac.wac_bt_thaiid.view.SharedViewModel
import com.wac.wacthaiidcloud.R
import com.wac.wacthaiidcloud.databinding.FragmentViewPager3Binding
//import eu.davidea.flexibleadapter.FlexibleAdapter

/**
 * A placeholder fragment containing a simple view.
 */
class FragmentViewPager3 : Fragment() {
    private var mSection = 0
//    private val mAdapter: FlexibleAdapter<*>? = null
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var binding: FragmentViewPager3Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mSection = arguments!!.getInt(ARG_SECTION_NUMBER)
            Log.d(TAG, "Creating new Fragment for Section $mSection")
        }

        // Contribution for specific action buttons in the Toolbar
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root=inflater.inflate(R.layout.fragment_view_pager3, container, false)
        binding = FragmentViewPager3Binding.bind(root)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        sharedViewModel = activity?.run {
            ViewModelProviders.of(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
        sharedViewModel.nativeCardInfo.observe(viewLifecycleOwner,{
            binding.txtCardTypecode.text = it.cardType
            binding.txtVersioncard.text = it.versionCard
            binding.txtLaserID.text = it.laserId
            binding.txtChipCard.text = it.chipId
            binding.txtImgCode.text = it.cardPhotoIssueNo
        })
    }

//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        mAdapter?.onSaveInstanceState(outState)
//    }
//
//    override fun onViewStateRestored(savedInstanceState: Bundle?) {
//        super.onViewStateRestored(savedInstanceState)
//        if (savedInstanceState != null && mAdapter != null) {
//            mAdapter.onRestoreInstanceState(savedInstanceState)
//        }
//    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"
        private val TAG = FragmentViewPager3::class.java.simpleName

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(sectionNumber: Int): FragmentViewPager3 {
            val fragment = FragmentViewPager3()
            val args = Bundle()
            args.putInt(ARG_SECTION_NUMBER, sectionNumber)
            fragment.arguments = args
            return fragment
        }
    }
}