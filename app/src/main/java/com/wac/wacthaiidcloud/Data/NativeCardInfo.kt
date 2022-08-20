package com.wacinfo.wacextrathaiid.Data

import com.google.gson.annotations.SerializedName
import okhttp3.RequestBody

class NativeCardInfo {
    @SerializedName("statusCode")
    var statusCode: String = ""

    @SerializedName("bp1No")
    var bp1No: String = ""

    @SerializedName("chipId")
    var chipId: String = ""

    @SerializedName("cardNumber")
    var cardNumber: String = ""

    @SerializedName("thaiTitle")
    var thaiTitle: String = ""

    @SerializedName("thaiFirstName")
    var thaiFirstName: String = ""

    @SerializedName("thaiMiddleName")
    var thaiMiddleName: String = ""

    @SerializedName("thaiLastName")
    var thaiLastName: String = ""

    @SerializedName("engTitle")
    var engTitle: String = ""

    @SerializedName("engFirstName")
    var engFirstName: String = ""

    @SerializedName("engMiddleName")
    var engMiddleName: String = ""

    @SerializedName("engLastName")
    var engLastName: String = ""

    @SerializedName("dateOfBirth")
    var dateOfBirth: String = ""

    @SerializedName("sex")
    var sex: String = ""

    @SerializedName("cardIssueNo")
    var cardIssueNo: String = ""

    @SerializedName("cardIssuePlace")
    var cardIssuePlace: String = ""

    @SerializedName("cardIssueDate")
    var cardIssueDate: String = ""

    @SerializedName("cardPhotoIssueNo")
    var cardPhotoIssueNo: String = ""

    @SerializedName("versionCard")
    var versionCard: String = ""

    @SerializedName("laserId")
    var laserId: String = ""

    @SerializedName("cardExpiryDate")
    var cardExpiryDate: String = ""

    @SerializedName("cardType")
    var cardType: String = ""

    @SerializedName("address")
    var address: CardAddress? = null

    @SerializedName("cardCountry")
    var cardCountry: String = ""

    @SerializedName("strPicture")
    var strPicture: String = ""

    @SerializedName("dipChipDateTime")
    var dipChipDateTime: String = ""
}