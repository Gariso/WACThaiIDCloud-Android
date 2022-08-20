package com.wac.wacthaiidcloud.utils

import android.app.Application
import android.graphics.Bitmap

class AppSettings : Application() {
    companion object {

        var REFRESH_TOKEN: String = ""
        var ACCESS_TOKEN: String = ""
        var USER_ID: String = ""
        var UID: String = ""
        var USER_NAME: String = ""
        var USER_RULE: String = ""
        var ID_USER_NAME: String = ""
        var IS_OFFLINE: Boolean = false
        var URL: String = ""
        var PORT: String = ""
    }

    fun resetSettings() {
        REFRESH_TOKEN = ""
        ACCESS_TOKEN = ""
        USER_ID = ""
        UID = ""
        USER_NAME = ""
        USER_RULE = ""
        ID_USER_NAME = ""
        IS_OFFLINE = false
    }

    override fun onCreate() {
        super.onCreate()
        // initialization code here

    }
}