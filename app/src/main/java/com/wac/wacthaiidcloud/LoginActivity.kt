package com.wac.wacthaiidcloud

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.afollestad.materialdialogs.MaterialDialog
import com.auth0.android.jwt.JWT
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tapadoo.alerter.Alerter
import com.wac.wacthaiidcloud.Retrofit.API
import com.wac.wacthaiidcloud.Retrofit.RetrofitData
import com.wac.wacthaiidcloud.Retrofit.Upload
import com.wac.wacthaiidcloud.databinding.ActivityLoginBinding
import com.wac.wacthaiidcloud.utils.AppSettings
import com.wac.wacthaiidcloud.utils.PublicFunction
import com.wac.wacthaiidcloud.view.SettingDialog
import com.wacinfo.wacextrathaiid.Data.NativeCardInfo
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*


class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(
            layoutInflater
        )
    }

    private val TAG = "Loginlog"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar!!.hide()

        initListener()

        val id: String = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        println("device_id: $id")
        println("getMobileIP: ${getIpv4HostAddress()}")

        val prfs = getSharedPreferences("AUTHENTICATION_FILE_NAME", MODE_PRIVATE)
        val username: String? = prfs.getString("Authentication_Id", "")
        val password: String? = prfs.getString("Authentication_Password", "")
        binding.usernameEdt.setText(username)
        binding.passwordEdt.setText(password)

        val prfsDns = getSharedPreferences("CONFIGURE_DNS_PORT", MODE_PRIVATE)
        val dnsOrIp: String? = prfsDns?.getString("CONFIGURE_IP_OR_DNS", "")
        val port: String? = prfsDns?.getString("CONFIGURE_PORT", "")

        if (dnsOrIp.isNullOrEmpty()) {
            AppSettings.URL = resources.getString(R.string.URL)
            AppSettings.PORT = resources.getString(R.string.PORT)
        } else {
            AppSettings.URL = dnsOrIp
            if (port.isNullOrEmpty()) {
                AppSettings.PORT = ""
            } else {
                AppSettings.PORT = port
            }
        }

    }

    private fun initListener() {
        binding.loginBtn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v == binding.loginBtn) {
            if (binding.usernameEdt.text.isNullOrEmpty() || binding.passwordEdt.text.isNullOrEmpty()) {
                Alerter.create(this@LoginActivity)
                    .setTitle("Alert")
                    .setText("Please enter your email id & password")
                    .setBackgroundColorRes(R.color.Red) // or setBackgroundColorInt(Color.CYAN)
                    .show()
            } else {
//                val sharedPref = applicationContext.getSharedPreferences("SharedPrefs${binding.usernameEdt.text.toString()}", MODE_PRIVATE)
//                sharedPref.edit().clear().apply()
                val deviceId: String = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                println("device_id: $deviceId")
                val body = RetrofitData.Login.PostBody()
                body.username = binding.usernameEdt.text.toString()
                body.password = binding.passwordEdt.text.toString()
                body.deviceId = deviceId
                login(body)
            }

        } else if (v == binding.settingBtn) {
            showSettingDialog()
        }
    }

    /** Get IP For mobile  */
//    fun getMobileIP(): String? {
//        try {
//            val en = NetworkInterface
//                .getNetworkInterfaces()
//            while (en.hasMoreElements()) {
//                val intf = en.nextElement() as NetworkInterface
//                val enumIpAddr = intf
//                    .inetAddresses
//                while (enumIpAddr.hasMoreElements()) {
//                    val inetAddress = enumIpAddr.nextElement()
//                    if (!inetAddress.isLoopbackAddress) {
//                        return inetAddress.hostAddress.toString()
//                    }
//                }
//            }
//        } catch (ex: java.lang.Exception) {
//            Log.e(TAG, "Exception in Get IP Address: " + ex.toString())
//        }
//        return null
//    }

    private fun showSettingDialog() {
        SettingDialog().show(supportFragmentManager, "settingDialog")
    }

    fun getIpv4HostAddress(): String {
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { return it.hostAddress }
        }
        return ""
    }

    private fun login(body: RetrofitData.Login.PostBody) {
        val dialog = PublicFunction().retrofitDialog(this)
        if (!dialog!!.isShowing) {
            runOnUiThread {
                dialog.show()
            }
        }

//        val url: String = resources.getString(R.string.URL) + resources.getString(R.string.PORT)
        val url: String = AppSettings.URL + AppSettings.PORT
        val apiname = resources.getString(R.string.API_Login)

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()

        retrofit.create(API::class.java)
            .postLogin(body, apiname)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<RetrofitData.Login> {
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: RetrofitData.Login) {
                    AppSettings.REFRESH_TOKEN = t.message?.refresh_token.toString()
                    token(AppSettings.REFRESH_TOKEN, dialog)
                }

                override fun onError(e: Throwable) {
                    if (e.message != null) {
                        val errorStr = e.message!!.substringAfterLast(":")
                        if (errorStr == " No address associated with hostname") {
                            dialog.cancel()
                            MaterialDialog(this@LoginActivity).show {
                                title(text = "คำเตือน")
                                message(text = "คุณต้องการใช้งานแบบ Offline หรือไม่")
                                negativeButton(text = "ยกเลิก")
                                positiveButton(text = "ดำเนินการต่อ", click = {
                                    AppSettings.ID_USER_NAME = binding.usernameEdt.text.toString()
                                    AppSettings.IS_OFFLINE = true
                                    val intent: Intent =
                                        Intent(this@LoginActivity, DeviceScanActivity::class.java)
                                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                    finish()
//                                    startActivity(Intent(this@LoginActivity,DeviceScanActivity::class.java))
                                })
                            }
                        } else {
                            PublicFunction().errorDialogSetTitle(dialog, errorStr).show()
                        }
                    } else {
                        PublicFunction().errorDialog(dialog).show()
                    }

                    e.printStackTrace()
                    if (e is HttpException) {
                        try {
                            val jObjError = JSONObject(e.response()!!.errorBody()?.string())
                            Log.d(TAG, jObjError.getString("message"))
                        } catch (e: Exception) {
                            Log.d(TAG, "onError: $e")
                        }
                    }

                }
            })
    }

    private fun Date.dateToString(format: String): String {
        //simple date formatter
        val dateFormatter = SimpleDateFormat(format, Locale.getDefault())

        //return the formatted date string
        return dateFormatter.format(this)
    }

    private fun uploadReader(index: Int, nativeCardInfo: NativeCardInfo) {

        val byteRawHex: ByteArray = ReaderActivity.hexStringToByteArray(nativeCardInfo.strPicture)
        val bitmapCard = BitmapFactory.decodeByteArray( byteRawHex,0,byteRawHex.size)

        val cardFile = File(this.cacheDir, "image")
        cardFile.createNewFile()
        val bos = ByteArrayOutputStream()
        bitmapCard.compress( Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/,bos )
        val bitmapData = bos.toByteArray()
        val fos = FileOutputStream(cardFile)
        fos.write(bitmapData)
        fos.flush()
        fos.close()

        val requestCardFile =
            cardFile.asRequestBody("image/png".toMediaTypeOrNull())

        /**create data for upload*/
        val request: MultipartBody.Builder =
            MultipartBody.Builder().setType(MultipartBody.FORM)
            request.addFormDataPart("mId", AppSettings.USER_ID)
            .addFormDataPart("uId", AppSettings.UID)
            .addFormDataPart(
                "smartDataImage",
                "image",
                requestCardFile
            )
            .addFormDataPart("bp1No", nativeCardInfo.bp1No)
            .addFormDataPart("chipId",  nativeCardInfo.chipId)
            .addFormDataPart("cardNumber",  nativeCardInfo.cardNumber)
            .addFormDataPart("thaiTitle",  nativeCardInfo.thaiTitle)
            .addFormDataPart("thaiFirstName", nativeCardInfo.thaiFirstName)
            .addFormDataPart("thaiMiddleName", nativeCardInfo.thaiMiddleName)
            .addFormDataPart("thaiLastName", nativeCardInfo.thaiLastName)
            .addFormDataPart("engTitle", nativeCardInfo.engTitle)
            .addFormDataPart("engFirstName", nativeCardInfo.engFirstName)
            .addFormDataPart("engMiddleName", nativeCardInfo.engMiddleName)
            .addFormDataPart("engLastName", nativeCardInfo.engLastName)
            .addFormDataPart("dateOfBirth", nativeCardInfo.dateOfBirth)
            .addFormDataPart("sex", nativeCardInfo.sex)
            .addFormDataPart("cardIssueNo", nativeCardInfo.cardIssueNo)
            .addFormDataPart("cardIssuePlace", nativeCardInfo.cardIssuePlace)
            .addFormDataPart("cardIssueDate", nativeCardInfo.cardIssueDate)
            .addFormDataPart("cardPhotoIssueNo", nativeCardInfo.cardPhotoIssueNo)
            .addFormDataPart("laserId", nativeCardInfo.laserId)
            .addFormDataPart("cardExpiryDate", nativeCardInfo.cardExpiryDate)
            .addFormDataPart("cardType", nativeCardInfo.cardType)
            .addFormDataPart("homeNo", nativeCardInfo.address!!.homeNo)
            .addFormDataPart("soi", nativeCardInfo.address!!.soi)
            .addFormDataPart("trok", nativeCardInfo.address!!.trok)
            .addFormDataPart("moo", nativeCardInfo.address!!.moo)
            .addFormDataPart("road", nativeCardInfo.address!!.road)
            .addFormDataPart("subDistrict", nativeCardInfo.address!!.subDistrict)
            .addFormDataPart("district", nativeCardInfo.address!!.district)
            .addFormDataPart("province", nativeCardInfo.address!!.province)
            .addFormDataPart("postalCode", nativeCardInfo.address!!.postalCode)
            .addFormDataPart("country", nativeCardInfo.address!!.country)
            .addFormDataPart("cardCountry", nativeCardInfo.cardCountry)
                ////////////
            .addFormDataPart("channel", "dipchip via MOBILE")
            .addFormDataPart("dipChipDateTime", nativeCardInfo.dipChipDateTime)
            .addFormDataPart("ipaddress", getIpv4HostAddress())

        val requestBody = request.build()

        var client = OkHttpClient()
        val builder = OkHttpClient.Builder()
        client = builder.build()
        val url: String = AppSettings.URL + AppSettings.PORT
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()

        retrofit.create<API>(API::class.java)
            .postUpload(
                AppSettings.ACCESS_TOKEN,
                getString(R.string.API_CardData),
                requestBody
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Upload.Response> {
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onNext(userResponse: Upload.Response) {
                    Log.i(TAG, "Request data " + Gson().toJson(userResponse))
                    if (index == getDataFromSharedPreferences()!!.size - 1) {
                        val sharedPref = applicationContext.getSharedPreferences("SharedPrefs${binding.usernameEdt.text.toString()}", MODE_PRIVATE)
                        sharedPref.edit().clear().apply()
                        val intent: Intent =
                            Intent(this@LoginActivity, DeviceScanActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    }
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        Log.d(TAG, "onError:HttpException ${e.localizedMessage}")
//                        errorDialog(dialog!!, e.localizedMessage)
                    } else {
                        Log.d(TAG, "onError: $e")
//                        errorDialog(dialog!!, e.toString())
                    }
                    e.printStackTrace()
                }
            })

    }
    private fun getDataFromSharedPreferences(): List<NativeCardInfo?>? {
        val gson = Gson()
        var productFromShared: List<NativeCardInfo?>? = ArrayList()
        val sharedPref = applicationContext.getSharedPreferences("SharedPrefs${binding.usernameEdt.text.toString()}", MODE_PRIVATE)
        val jsonPreferences = sharedPref.getString("NativeCardInfo", "")
        val type = object : TypeToken<List<NativeCardInfo?>?>() {}.type
        productFromShared = gson.fromJson<List<NativeCardInfo?>>(jsonPreferences, type)
        return productFromShared
    }

    private fun token(token: String, dialog: SweetAlertDialog) {
        val url: String = AppSettings.URL + AppSettings.PORT
        val apiname = resources.getString(R.string.API_Token)

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()

        retrofit.create(API::class.java)
            .postToken(token, apiname)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<RetrofitData.Login> {
                override fun onComplete() {
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: RetrofitData.Login) {

                    println(t)
                    println(t.message)
                    val parsedJWT = JWT(t.message?.refresh_token.toString())
                    val userIDData = parsedJWT.getClaim("userId")
                    val userID = userIDData.asString()
                    val ruleData = parsedJWT.getClaim("rule")
                    val rule = ruleData.asString()?.toLowerCase(Locale.getDefault())
                    val uidData = parsedJWT.getClaim("uId")
                    val uid = uidData.asString()

                    AppSettings.ACCESS_TOKEN = t.message?.access_token.toString()
                    AppSettings.USER_ID = userID!!
                    AppSettings.UID = uid!!
                    AppSettings.USER_NAME = binding.usernameEdt.text.toString()
                    AppSettings.USER_RULE = rule.toString().toUpperCase()
                    Log.d(TAG, "onNext: ${AppSettings.ACCESS_TOKEN}")
                    Log.d(TAG, "onNext: ${AppSettings.USER_ID}")
                    dialog.cancel()

                    val preferences =
                        getSharedPreferences("AUTHENTICATION_FILE_NAME", MODE_PRIVATE)
                    val editor = preferences.edit()
                    editor.putString("Authentication_Id", binding.usernameEdt.text.toString())
                    editor.putString("Authentication_Password",binding.passwordEdt.text.toString())
                    editor.apply()

                    if (getDataFromSharedPreferences() != null) {
                        if (getDataFromSharedPreferences()!!.isEmpty()) {
                            val intent: Intent =
                                Intent(this@LoginActivity, DeviceScanActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        } else {
                            for ((index, value) in getDataFromSharedPreferences()!!.withIndex()) {
                                if (value != null) {
                                    println("index: $index ${getDataFromSharedPreferences()!!.size}")
                                    uploadReader(index, value)
                                }
                            }
                        }
                    } else {
                        val intent: Intent =
                            Intent(this@LoginActivity, DeviceScanActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    }

                }

                override fun onError(e: Throwable) {
                    PublicFunction().errorDialog(dialog).show()
                    e.printStackTrace()
                    if (e is HttpException) {
                        try {
                            val jObjError = JSONObject(e.response()!!.errorBody()?.string())
                            Log.d(TAG, jObjError.getString("message"))
                        } catch (e: Exception) {
                            Log.d(TAG, e.toString())
                        }
                    } else {
                        PublicFunction().message(this@LoginActivity, "Sync data fail")
                    }


                }
            })
    }

}