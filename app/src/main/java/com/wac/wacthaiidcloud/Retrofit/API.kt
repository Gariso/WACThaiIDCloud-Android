package com.wac.wacthaiidcloud.Retrofit

import io.reactivex.Observable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface API {

    @POST("{api}")
    fun postLogin(
        @Body data: RetrofitData.Login.PostBody, @Path(value = "api", encoded = true) api: String
    ): Observable<RetrofitData.Login>


    @POST("{api}")
    fun postToken(
        @Header("Authorization") authHeader: String,
        @Path(value = "api", encoded = true) api: String
    ): Observable<RetrofitData.Login>

    @POST("{api}")
    fun postUpload(
        @Header("Authorization") authHeader: String,
        @Path(value = "api", encoded = true) api: String,
        @Body data: RequestBody
    ): Observable<Upload.Response>
}