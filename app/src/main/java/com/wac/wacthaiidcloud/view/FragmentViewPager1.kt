package com.wac.wac_bt_thaiid.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.wac.wacthaiidcloud.R
import com.wac.wacthaiidcloud.databinding.FragmentViewPagerBinding
import eu.davidea.flexibleadapter.FlexibleAdapter

/**
 * A placeholder fragment containing a simple view.
 */
class FragmentViewPager1 : Fragment() {
    private var mSection = 0
    private var name = ""
    private val mAdapter: FlexibleAdapter<*>? = null
    private var nameth: TextView? = null
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var binding: FragmentViewPagerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mSection = arguments!!.getInt(ARG_SECTION_NUMBER)
            Log.d(TAG, "Creating new Fragment for Section $mSection")
        }

        // Contribution for specific action buttons in the Toolbar
        setHasOptionsMenu(true)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =  inflater.inflate(R.layout.fragment_view_pager, container, false)
        binding = FragmentViewPagerBinding.bind(root)




        return root
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        sharedViewModel = activity?.run {
            ViewModelProviders.of(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
        sharedViewModel.nativeCardInfo.observe(viewLifecycleOwner,{
            binding.txtCID.text = it.cardNumber
            binding.txtFullnameTH.text = "${it.thaiTitle} ${it.thaiFirstName} ${it.thaiMiddleName} ${it.thaiLastName}".trim()
            binding.txtFullnameEN.text = "${it.engTitle} ${it.engFirstName} ${it.engMiddleName} ${it.engLastName}"
            binding.txtDOB.text = it.dateOfBirth
            binding.txtGender.text = it.sex
            binding.txtAddress.text ="${it.address?.homeNo} ${it.address?.soi}" +
                    " ${it.address?.trok} ${it.address?.moo} ${it.address?.road}" +
                    " ${it.address?.subDistrict} ${it.address?.district} ${it.address?.province} ${it.address?.postalCode}"
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mAdapter?.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null && mAdapter != null) {
            mAdapter.onRestoreInstanceState(savedInstanceState)
        }
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"
        private val TAG = FragmentViewPager1::class.java.simpleName

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(sectionNumber: Int): FragmentViewPager1 {
            val fragment = FragmentViewPager1()
            val args = Bundle()
            args.putInt(ARG_SECTION_NUMBER, sectionNumber)
            fragment.arguments = args
            return fragment
        }
    }
}