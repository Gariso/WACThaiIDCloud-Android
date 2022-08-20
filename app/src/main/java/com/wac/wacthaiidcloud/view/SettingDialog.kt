package com.wac.wacthaiidcloud.view

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.wac.wacthaiidcloud.R
import com.wac.wacthaiidcloud.utils.AppSettings

/**
 *
 */
class SettingDialog : DialogFragment() {
    /**
     *
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val view = requireActivity().layoutInflater.inflate(
                R.layout.dialog_setting,
                null
            )
            val dnsOrIpEdt = view.findViewById<View>(R.id.dns_or_ip_edt) as TextInputEditText
            val portEdt = view.findViewById<View>(R.id.port_edt) as TextInputEditText

            val prfs = activity?.getSharedPreferences("CONFIGURE_DNS_PORT",
                AppCompatActivity.MODE_PRIVATE
            )
            val dnsOrIp: String? = prfs?.getString("CONFIGURE_IP_OR_DNS", "")
            val port: String? = prfs?.getString("CONFIGURE_PORT", "")

            println("DNS: $dnsOrIp ")
            println("port: $port")

            if (dnsOrIp.isNullOrEmpty()) {
                dnsOrIpEdt.setText(resources.getString(R.string.URL))
                portEdt.setText(resources.getString(R.string.PORT))
            } else {
                dnsOrIpEdt.setText(dnsOrIp)
                portEdt.setText(port)
            }
            val alertDialog = AlertDialog.Builder(it)
            alertDialog.setView(view)
            alertDialog.setPositiveButton("ตกลง", DialogInterface.OnClickListener { dialog, id ->
                val preferences =
                    activity?.getSharedPreferences(("CONFIGURE_DNS_PORT"), AppCompatActivity.MODE_PRIVATE)
                val editor = preferences?.edit()
                editor?.putString("CONFIGURE_IP_OR_DNS", dnsOrIpEdt.text.toString())
                editor?.putString("CONFIGURE_PORT",portEdt.text.toString())
                editor?.apply()
                AppSettings.URL = dnsOrIpEdt.text.toString()
                AppSettings.PORT = portEdt.text.toString()
            })
            alertDialog.setNegativeButton("ค่่าเริ่มต้น", DialogInterface.OnClickListener { dialog, id ->
                dnsOrIpEdt.setText(resources.getString(R.string.URL))
                portEdt.setText(resources.getString(R.string.PORT))
                val sharedPref = activity?.getSharedPreferences("CONFIGURE_DNS_PORT", AppCompatActivity.MODE_PRIVATE )
                sharedPref?.edit()?.clear()?.apply()
                AppSettings.URL = dnsOrIpEdt.text.toString()
                AppSettings.PORT = portEdt.text.toString()
            })
            alertDialog.create()
        } ?: throw IllegalStateException("Activity is null!!")
    }
}