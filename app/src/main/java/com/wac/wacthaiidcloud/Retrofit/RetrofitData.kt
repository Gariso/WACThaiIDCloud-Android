package com.wac.wacthaiidcloud.Retrofit

import com.google.gson.annotations.SerializedName

class RetrofitData {

    class Login {
        @SerializedName("status")
        var status: String? = null

        @SerializedName("message")
        var message: ResponesMessage? = null

        class PostBody {
            @SerializedName("username")
            var username: String? = null

            @SerializedName("password")
            var password: String? = null

            @SerializedName("deviceId")
            var deviceId: String? = null
        }

        class ResponesMessage {
            @SerializedName("access_token")
            var access_token: String? = null

            @SerializedName("token_type")
            var token_type: String? = null

            @SerializedName("expires_in")
            var expires_in: String? = null

            @SerializedName("refresh_token")
            var refresh_token: String? = null

            @SerializedName("scope")
            var scope: String? = null
        }
    }

}