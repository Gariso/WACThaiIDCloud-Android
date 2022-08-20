package com.wacinfo.wacextrathaiid.Data

import com.google.gson.annotations.SerializedName


class PostalCode(
    id: String,
    zip: String,
    province: String,
    district: String,
    lat: String,
    lng: String
) {

    @SerializedName("id")
    val id: String = ""

    @SerializedName("zip")
    val zip: String = ""

    @SerializedName("province")
    val province: String = ""

    @SerializedName("district")
    val district: String = ""

    @SerializedName("lat")
    val lat: String = ""

    @SerializedName("lng")
    val lng: String = ""

}
