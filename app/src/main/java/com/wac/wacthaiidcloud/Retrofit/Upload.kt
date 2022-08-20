package com.wac.wacthaiidcloud.Retrofit

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody


class Upload {

    class Response {
        @SerializedName("status")
        var status: String? = null

        @SerializedName("message")
        var message: String? = null
    }

    class Data {

        @SerializedName("mId")
        var mId : String = ""

        @SerializedName("uId")
        var uId : String = ""

        @SerializedName("smartDataImage")
        var smartDataImage : MultipartBody.Part? = null

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

        @SerializedName("cardIssuerNo")
        var cardIssuerNo: String = ""

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

        @SerializedName("photo")
        var photo: CardPhoto? = null

        class CardAddress {
            @SerializedName("homeNo")
            var homeNo: String = ""

            @SerializedName("soi")
            var soi: String = ""

            @SerializedName("trok")
            var trok: String = ""

            @SerializedName("moo")
            var moo: String = ""

            @SerializedName("road")
            var road: String = ""

            @SerializedName("subDistrict")
            var subDistrict: String = ""

            @SerializedName("district")
            var district: String = ""

            @SerializedName("province")
            var province: String = ""

            @SerializedName("postalCode")
            var postalCode: String = ""

            @SerializedName("country")
            var country: String = ""
        }

        class CardPhoto {
            @SerializedName("statusCode")
            var statusCode: String = ""

            @SerializedName("photo")
            var photo: String = ""
        }
    }

}