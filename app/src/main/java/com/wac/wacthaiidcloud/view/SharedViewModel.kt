package com.wac.wac_bt_thaiid.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wacinfo.wacextrathaiid.Data.NativeCardInfo

class SharedViewModel : ViewModel() {

    val nativeCardInfo = MutableLiveData<NativeCardInfo>()
    fun setNativeCardInfo(CardInfo: NativeCardInfo) {
        nativeCardInfo.value = CardInfo
    }
}