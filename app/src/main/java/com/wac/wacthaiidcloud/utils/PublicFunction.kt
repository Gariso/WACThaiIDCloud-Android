package com.wac.wacthaiidcloud.utils

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.core.content.ContextCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.wac.wacthaiidcloud.R

class PublicFunction {
    fun message(activity: Activity, msg: String?) {
        try {
            activity.runOnUiThread(Runnable {
                Toast.makeText(
                    activity.application,
                    msg,
                    Toast.LENGTH_SHORT
                ).show()
            })
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            Toast.makeText(
                activity.application,
                throwable.message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun retrofitDialog(context: Context): SweetAlertDialog? {
        return SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE)
            .setConfirmButtonBackgroundColor(ContextCompat.getColor(context, R.color.green_inactive))
            .setTitleText("Please wait...")
    }

    fun errorDialog(dialog: SweetAlertDialog): SweetAlertDialog {
        dialog.setTitleText("Fail!")
            .setConfirmText("Close")
            .setConfirmButtonBackgroundColor(ContextCompat.getColor(dialog.context, R.color.red_active))
            .setConfirmClickListener(null)
            .changeAlertType(SweetAlertDialog.ERROR_TYPE)
        return dialog
    }

    fun errorDialogSetTitle(dialog: SweetAlertDialog, title: String): SweetAlertDialog {
        dialog.setTitleText("Fail! $title")
            .setConfirmText("Close")
            .setConfirmButtonBackgroundColor(ContextCompat.getColor(dialog.context, R.color.red_active))
            .setConfirmClickListener(null)
            .changeAlertType(SweetAlertDialog.ERROR_TYPE)
        return dialog
    }

}