package com.wac.wacthaiidcloud

import amlib.ccid.Reader
import amlib.ccid.ReaderException
import amlib.ccid.SCError
import amlib.hw.HWType
import amlib.hw.HardwareInterface
import amlib.hw.ReaderHwException
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.afollestad.materialdialogs.MaterialDialog
import com.auth0.android.jwt.JWT
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wac.wac_bt_thaiid.view.FragmentViewPager1
import com.wac.wac_bt_thaiid.view.SharedViewModel
import com.wac.wacthaiidcloud.Retrofit.API
import com.wac.wacthaiidcloud.Retrofit.RetrofitData
import com.wac.wacthaiidcloud.Retrofit.Upload
import com.wac.wacthaiidcloud.utils.AppSettings
import com.wac.wacthaiidcloud.utils.PublicFunction
import com.wacinfo.wacextrathaiid.Data.CardAddress
import com.wacinfo.wacextrathaiid.Data.CardPhoto
import com.wacinfo.wacextrathaiid.Data.NativeCardInfo
import com.wacinfo.wacextrathaiid.Data.PostalCode
import com.wacinfo.wacextrathaiid.FragmentViewPager2
import com.wacinfo.wacextrathaiid.FragmentViewPager3
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.UnsupportedEncodingException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.experimental.or

class UsbReaderActivity : AppCompatActivity(), View.OnClickListener,
CompoundButton.OnCheckedChangeListener {

    //pcsc
    private var mReader: Reader? = null
    private var mMyDev: HardwareInterface? = null
    private var mUsbDev: UsbDevice? = null
    private var mManager: UsbManager? = null
    var mSlotDialog: AlertDialog.Builder? = null
    var nativeCardInfo: NativeCardInfo? = NativeCardInfo()
    var cardPhoto: CardPhoto? = CardPhoto()
    private var mConnectState = UsbReaderActivity.STATE_DISCONNECTED

    //Builder  mPowerDialog;
    private var mSlotNum: Byte = 0
    private var mPermissionIntent: PendingIntent? = null
    private var mOpenButton: Button? = null
    private var mCloseButton: Button? = null
    private var mReaderAdapter: ArrayAdapter<String>? = null
    private var mDeviceName: String? = "USB Reader"
    private var mTextViewResult: TextView? = null
    private var mCloseProgress: ProgressDialog? = null
//    private var mReaderSpinner: Spinner? = null
    private var mStrMessage: String? = null
    var mContext: Context? = null

    var mClear: Button? = null

    //---Personal Info
    private var txtCID: TextView? = null
    private var txtFullnameTH: TextView? = null
    private var txtFullnameEN: TextView? = null
    private var txtDOB: TextView? = null
    private var txtGender: TextView? = null
    private var txtreq_number: TextView? = null
    private var txtIssuePlace: TextView? = null
    private var txtIssueCode: TextView? = null
    private var txtIssue: TextView? = null
    private var txtExpire: TextView? = null
    private var txtCardTypeCode: TextView? = null
    private var txtAddress: TextView? = null
    private var txtVersioncard: TextView? = null
    private var imgPhoto: ImageView? = null
    private var txtImgcode: TextView? = null
    private var txtofficer_license: TextView? = null
    private val imgBase64String: String? = null

    //----------------
    //private File dowloadLocate = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    private var sd: File? = null
    private var sharedPreferences: SharedPreferences? = null
    private var btn_refresh: Button? = null
    private var btn_menu: Button? = null

    private var mReadAll: Button? = null
    private var mViewPager: ViewPager? = null
    private var adapter: FragmentPagerAdapter? = null
    private lateinit var sharedViewModel: SharedViewModel

    var dialog: SweetAlertDialog? = null

    fun <P, R> CoroutineScope.executeAsyncTask(
        onPreExecute: () -> Unit,
        doInBackground: suspend (suspend (P) -> Unit) -> R,
        onPostExecute: (R) -> Unit,
        onProgressUpdate: (P) -> Unit,
    ) = launch {
        onPreExecute()

        val result = withContext(Dispatchers.IO) {
            doInBackground {
                withContext(Dispatchers.Main) { onProgressUpdate(it) }
            }
        }
        onPostExecute(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usb_reader)
        sharedViewModel = run { ViewModelProviders.of(this)[SharedViewModel::class.java]}

        sharedPreferences = getSharedPreferences(
            getString(R.string.PrefsSetting),
            MODE_PRIVATE
        )

        //imgPhoto = (ImageView) findViewById(R.id.idcard);
        txtFullnameTH = findViewById<View>(R.id.txtFullnameTH) as TextView
        txtFullnameEN = findViewById<View>(R.id.txtFullnameEN) as TextView
        txtCID = findViewById<View>(R.id.txtCID) as TextView
        txtDOB = findViewById<View>(R.id.txtDOB) as TextView
        txtGender = findViewById<View>(R.id.txtGender) as TextView
        txtreq_number = findViewById<View>(R.id.txtreq_number) as TextView
        txtIssuePlace = findViewById<View>(R.id.txtIssueplace) as TextView
        txtIssueCode = findViewById<View>(R.id.txtIssuecode) as TextView
        txtIssue = findViewById<View>(R.id.txtIssue) as TextView
        txtExpire = findViewById<View>(R.id.txtExpire) as TextView
        txtCardTypeCode = findViewById<View>(R.id.txtCardTypecode) as TextView
        txtAddress = findViewById<View>(R.id.txtAddress) as TextView
        txtVersioncard = findViewById<View>(R.id.txtVersioncard) as TextView
        txtImgcode = findViewById<View>(R.id.txtImgCode) as TextView
        txtofficer_license = findViewById<View>(R.id.txtOfficer_license) as TextView
        //        imgPhoto = (ImageView) findViewById(R.id.idcard);
        btn_refresh = findViewById<View>(R.id.btn_refresh) as Button
        btn_refresh!!.setOnClickListener(this)
//        btn_menu = findViewById<View>(R.id.menu_btn) as Button
//        btn_menu!!.setOnClickListener(this)

        initViewPager()
//        supportActionBar?.hide()

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        //---
        setupViews()

        // Start USB interface
        mSlotNum = 0.toByte()
        mContext = applicationContext
        try {
            mMyDev = HardwareInterface(HWType.eUSB, this.applicationContext)
            //mMyDev.setLog(mContext,true, 0xff);
        } catch (e: Exception) {
            mStrMessage = "Get Exception : " + e.message
            Log.e(TAG, mStrMessage!!)
            (TAG + " :: " + mStrMessage!!)

            return
        }
        // Get USB manager
        Log.d(TAG, " mManager")
        (TAG + " :: " + " mManager")
        mManager = getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager
        findDevice()

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )
        supportActionBar!!.title = mDeviceName
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        /* Inflate the menu; this adds items to the action bar if it is present. */
        menuInflater.inflate(R.menu.reader, menu)
        if (mConnectState == STATE_CONNECTED) {
            menu.findItem(R.id.menu_connect).isVisible = false
            menu.findItem(R.id.menu_connecting).isVisible = false
            menu.findItem(R.id.menu_disconnect).isVisible = true
        } else if (mConnectState == STATE_CONNECTING) {
            menu.findItem(R.id.menu_connect).isVisible = false
            menu.findItem(R.id.menu_connecting).isVisible = true
            menu.findItem(R.id.menu_disconnect).isVisible = false
            menu.findItem(R.id.menu_refresh).setActionView(
                R.layout.actionbar_indeterminate_progress
            )
        } else {
            menu.findItem(R.id.menu_connect).isVisible = true
            menu.findItem(R.id.menu_connecting).isVisible = false
            menu.findItem(R.id.menu_disconnect).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_connect -> {
                /* Connect Bluetooth reader */
                Log.v(ReaderActivity.TAG, "Start to connect!!!")
                updateConnectionState(STATE_CONNECTING)
                connectClick()
                return true
            }
            R.id.menu_connecting, R.id.menu_disconnect -> {
                /* Disconnect Bluetooth reader */
                Log.v(ReaderActivity.TAG, "Start to disconnect!!!")
                updateConnectionState(STATE_DISCONNECTING)
                closeReaderUp()
                closeReaderBottom()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.menu_about -> {
                val manager = this@UsbReaderActivity.packageManager
                val info = manager.getPackageInfo(
                    this@UsbReaderActivity.packageName,
                    PackageManager.GET_ACTIVITIES
                )

                MaterialDialog(this@UsbReaderActivity).show {
                    title(text = "About ${getString(R.string.app_name)}")
                    val message = "Version : " + info.versionName +
                            "\nDevice id : " + sharedPreferences!!.getString(
                        (getString(R.string.DeviceID)),
                        ""
                    ) +
                            "\nKey : " + sharedPreferences!!.getString(
                        (getString(R.string.key)),
                        ""
                    )
                    message(text = message)
                    positiveButton(R.string.ok)
                }
                return true
            }
            R.id.menu_logout -> {
                val intent: Intent =
                    Intent(this@UsbReaderActivity, LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateConnectionState(connectState: Int) {
        mConnectState = connectState
        invalidateOptionsMenu()
    }

    //Create 3 view pager for show information in thaiID card
    private fun initViewPager() {
        adapter = PagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById<View>(R.id.viewpager) as ViewPager
        mViewPager!!.adapter = adapter

        val tabLayout = findViewById<View>(R.id.tabs) as TabLayout
        tabLayout.setupWithViewPager(mViewPager)
    }

    //Handle view pager
    class PagerAdapter(fm: FragmentManager?) : FragmentPagerAdapter(fm!!) {
        override fun getItemPosition(`object`: Any): Int {
            return super.getItemPosition(`object`)
        }

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a FragmentViewPager (defined as a static inner class below).
            return if (position + 1 == 1) {
                return FragmentViewPager1.newInstance(position + 1)
            } else if (position + 1 == 2) {
                return FragmentViewPager2.newInstance(position + 1)
            } else if (position + 1 == 3) {
                return FragmentViewPager3.newInstance(position + 1)
            } else {
                FragmentViewPager1.newInstance(position + 1)
            }

        }

        override fun getCount(): Int {
            // Show 3 total pages.
            return 3
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return "SECTION 1"
                1 -> return "SECTION 2"
                2 -> return "SECTION 3"
            }
            return null
        }
    }

    //read zipcode.json in assets folder
    fun AssetManager.readFile(fileName: String) = open(fileName)
        .bufferedReader()
        .use { it.readText() }


    private fun findDevice() {
        toRegisterReceiver()
        EnumeDev()
    }


    override fun onClick(v: View) {
        if (v == mReadAll) {

            dialog = PublicFunction().retrofitDialog(this)
            if (!dialog!!.isShowing) {
                runOnUiThread {
                    dialog!!.show()
                }
            }
            ReadCard()
//            val dialog = ProgressDialog(this)
//            dialog.setTitle(R.string.reading)
//            dialog.setMessage(getString(R.string.do_not_eject))
//            lifecycleScope.executeAsyncTask(
//                onPreExecute = {
//                    // ... runs in Main Thread
//                    dialog.show();
//                }, doInBackground = { publishProgress: suspend (progress: Int) -> Unit ->
//                    ReadCard()
//                    "Result" // send data to "onPostExecute"
//                }, onPostExecute = {
//                    dialog.dismiss();
//                    // runs in Main Thread
//                    // ... here "it" is a data returned from "doInBackground"
//                }, onProgressUpdate = {
//
//                    // runs in Main Thread
//                    // ... here "it" contains progress
//                }
//            )

        }
        if (v == btn_refresh) {
            findDevice()
            nativeCardInfo = NativeCardInfo()
            nativeCardInfo!!.address = CardAddress()
            sharedViewModel.setNativeCardInfo(nativeCardInfo!!)
        }

//        if (v == btn_menu) {
//            showMenu(v, R.menu.popup_menu)
//        }
    }

    // Disconnect USB device when exit app
    override fun onDestroy() {
        unregisterReceiver(mReceiver)
        if (mMyDev != null) mMyDev!!.Close()
        super.onDestroy()
    }

    fun setupViews() {
        //String[] pMode = new String[] {mode2, mode3};
        mOpenButton = findViewById(R.id.buttonOpen)
        mCloseButton = findViewById(R.id.buttonClose)


        mOpenButton!!.visibility = View.GONE
        btn_refresh!!.visibility = View.GONE
        mReadAll = findViewById(R.id.buttonReadAll)
        mReadAll?.setOnClickListener(this)
        onCreateButtonSetup()
        mTextViewResult = findViewById(R.id.textResult)
//        setupReaderSpinner()
        setReaderSlotView()
        mClear = findViewById(R.id.buttonClear)
        txtFullnameTH = findViewById(R.id.txtFullnameTH)
        txtDOB = findViewById(R.id.txtDOB)
        txtAddress = findViewById(R.id.txtAddress)
        imgPhoto = findViewById(R.id.idcard)
    }

//    private fun showMenu(v: View, @MenuRes menuRes: Int) {
//        val popup = PopupMenu(this, v)
//        popup.menuInflater.inflate(menuRes, popup.menu)
//
//        popup.setOnMenuItemClickListener(object :
//            androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener,
//            PopupMenu.OnMenuItemClickListener {
//            override fun onMenuItemClick(item: MenuItem): Boolean {
//                return when (item.itemId) {
//                    R.id.howto -> {
//                        MaterialDialog(this@UsbReaderActivity).show {
//                            title(R.string.howtousetitle)
//                            val message = "1.เสียบเครื่องอ่านที่ช่อง USB" + "\n" +
//                                    "2.กดปุ่ม Refresh รอจนมีชื่อของ USB แสดงบนมุมซ้าย" + "\n" +
//                                    "3.กดปุ่ม OPEN จากนั้นกดปุ่ม Read" + "\n" +
//                                    "4.รอจนกว่าข้อมูลจะแสดงจนครบ"
//                            message(text = message)
//                            positiveButton(R.string.agree)
//                        }
//                        true
//                    }
//                    R.id.about -> {
//                        // แสดง app version , device id, key activate
//                        val manager = this@MainActivity.packageManager
//                        val info = manager.getPackageInfo(
//                            this@MainActivity.packageName,
//                            PackageManager.GET_ACTIVITIES
//                        )
//
//                        MaterialDialog(this@MainActivity).show {
//                            title(text = "About ${getString(R.string.app_name)}")
//                            val message = "Version : " + info.versionName +
//                                    "\nDevice id : " + sharedPreferences!!.getString(
//                                (getString(R.string.DeviceID)),
//                                ""
//                            ) +
//                                    "\nKey : " + sharedPreferences!!.getString(
//                                (getString(R.string.key)),
//                                ""
//                            )
//                            message(text = message)
//                            positiveButton(R.string.agree)
//                        }
//                        true
//                    }
//                    else -> false
//                }
//            }
//        })
//
//        popup.setOnDismissListener {
//            // Respond to popup being dismissed.
//        }
//        // Show the popup menu.
//        popup.show()
//    }


    private fun toRegisterReceiver() {
        // Register receiver for USB permission
        mPermissionIntent = PendingIntent.getBroadcast(
            this, 0, Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(mReceiver, filter)
    }

//    private fun setupReaderSpinner() {
//        // Initialize reader spinner list
//        mReaderAdapter = ArrayAdapter(
//            this,
//            android.R.layout.simple_spinner_item
//        )
////        mReaderSpinner = findViewById(R.id.spinnerDevice)
//        mReaderSpinner?.adapter = mReaderAdapter
//        mReaderSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(
//                arg0: AdapterView<*>?,
//                arg1: View,
//                position: Int,
//                arg3: Long
//            ) {
//
//            }
//
//            override fun onNothingSelected(arg0: AdapterView<*>?) {
//
//            }
//        }
//    }

    //set usb reader to list item
    private fun setReaderSlotView() {
        val arraySlot = arrayOf("slot:0", "Slot:1")
        mSlotDialog = AlertDialog.Builder(this)
        val Select = DialogInterface.OnClickListener { dialog, which -> mSlotNum = which.toByte() }
        val OkClick = DialogInterface.OnClickListener { dialog, which -> requestDevPerm() }
        mSlotDialog!!.setPositiveButton("OK", OkClick)
        mSlotDialog!!.setTitle("Select Slot Number")
        mSlotDialog!!.setSingleChoiceItems(arraySlot, 0, Select)
    }

    private fun checkSlotNumber(uDev: UsbDevice) {
        if (uDev.productId == 0x9522 || uDev.productId == 0x9525 ||
            uDev.productId == 0x9526 || uDev.productId == 0x9572
        )
            mSlotDialog!!.show() else {
            mSlotNum = 0.toByte()
            requestDevPerm()
        }
    }

    private fun updateViewReader() {
        //int pid = 0;
        //int vid = 0;
        try {
            //pid = mUsbDev.getProductId();
            //vid = mUsbDev.getVendorId();
            mStrMessage = "updateViewReader"
            Log.e(TAG, mStrMessage!!)
            (" updateViewReader  :: $mStrMessage")
        } catch (e: NullPointerException) {
            mStrMessage = "Get Exception : " + e.message
            Log.e(TAG, mStrMessage!!)
            ("mStrupdateView Get Exception :: $e.message")
            return
        }
    }

    private fun updateReaderList(intent: Intent) {
        // Update reader list
//        mReaderAdapter!!.clear()
        for (device in mManager!!.deviceList.values) {
            Log.d(TAG, "Update reader list : " + device.deviceName)
            if (isAlcorReader(device)) mDeviceName = device.deviceName
            supportActionBar!!.title = mDeviceName
        }
        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)

    }


    //Select usb device on connected
    private fun getSpinnerSelect(): UsbDevice? {
        val deviceName: String? = mDeviceName

        if (deviceName != null) {
            // For each device
            for (device in mManager!!.deviceList.values) {
                if (deviceName == device.deviceName) {
                    supportActionBar!!.title = mDeviceName
                    return device
                }
            }
        }
        return null
    }


    //Select usb device on connected and check request permission to use device
    private fun requestDevPerm() {
        val dev = getSpinnerSelect()
        if (dev != null)  {
            mManager!!.requestPermission(dev, mPermissionIntent)
        } else  {
            Log.e( TAG, "selected not found")
            updateConnectionState(STATE_DISCONNECTED)
        }
    }

    private fun connectClick() {
        Log.d(TAG, "ConnectOnClick")
        val dev = getSpinnerSelect()
        if (dev != null) {
            checkSlotNumber(dev)
        } else {
            updateConnectionState(STATE_DISCONNECTED)
        }
    }

    fun OpenOnClick(view: View?) {
        Log.d(TAG, "OpentOnClick")
        val dev = getSpinnerSelect()
        if (dev != null) {
            checkSlotNumber(dev)
        }
    }//Log.d(TAG,"cmd OK  mSlotStatus = " +mSlotStatus);

    /*detect card hotplug events*/
    private fun getSlotStatus(): Int {
        var ret = SCError.READER_NO_CARD
        val pCardStatus = ByteArray(1)

        /*detect card hotplug events*/
        ret = mReader!!.getCardStatus(pCardStatus)
        if (ret == SCError.READER_SUCCESSFUL) {
            ret = if (pCardStatus[0] == Reader.SLOT_STATUS_CARD_ABSENT) {
                SCError.READER_NO_CARD
            } else if (pCardStatus[0] == Reader.SLOT_STATUS_CARD_INACTIVE) {
                SCError.READER_CARD_INACTIVE
            } else {
                SCError.READER_SUCCESSFUL
            }
        }
        return ret
    }

    // *** Send APDUcommand to ic card
    private fun SendAPDUcommand(strAPDU: String): ByteArray? {
        val pSendAPDU: ByteArray
        val pRecvRes = ByteArray(300)
        val pRevAPDULen = IntArray(1)
        val apduStr: String
        val sendLen: Int
        val result: Int
        pRevAPDULen[0] = 300
        apduStr = strAPDU.trim { it <= ' ' }
        pSendAPDU = toByteArray(apduStr)
        sendLen = pSendAPDU.size
        return try {
            result = mReader!!.transmit(pSendAPDU, sendLen, pRecvRes, pRevAPDULen)
            if (result == SCError.READER_SUCCESSFUL) {
                pRecvRes
            } else {
                Log.e(
                    TAG, "Fail to Send APDU: " + Integer.toString(result)
                            + "(" + Integer.toHexString(mReader!!.cmdFailCode) + ")"
                )
                null
            }
        } catch (e: Exception) {
            mStrMessage = "Get Exception : " + e.message
            Log.e(TAG, mStrMessage!!)
            mTextViewResult!!.text = mStrMessage
            null
        }
    }

    //Clear view layout
    private fun PersonalInfoTextViewClear() {

        runOnUiThread {
            imgPhoto!!.setImageResource(R.drawable.ic_id_card_svgrepo_com)
            //imgPhoto.setImageDrawable(null);

            txtFullnameTH!!.text = getString(R.string.name_th)
            txtFullnameEN!!.text = getString(R.string.name_en)
            txtCID!!.text = getString(R.string.CID)
            txtDOB!!.text = getString(R.string.DOB)
            txtGender!!.text = getString(R.string.gender)
            txtreq_number!!.text = getString(R.string.Request_number)
            txtIssuePlace!!.text = getString(R.string.Issueplace)
            txtIssueCode!!.text = getString(R.string.Issuecode)
            txtIssue!!.text = getString(R.string.Issue)
            txtExpire!!.text = getString(R.string.Expire)
            txtCardTypeCode!!.text = getString(R.string.CardTypecode)
            txtAddress!!.text = getString(R.string.Address)
            txtVersioncard!!.text = getString(R.string.VersionCard)
            txtImgcode!!.text = getString(R.string.imgcode)
            txtofficer_license!!.text = getString(R.string.Officer_license)

        }

    }

    fun ClearClick(view: View?) {
        PersonalInfoTextViewClear()
    }

    /*
        เริ่มการอ่านบัตรประชาชน
    - เคลียข้อมูลที่มีอยู่บนหน้าจอทั้งหมด
    - ส่งคำสั่งจ่ายไฟเข้าบัตรโดยเรียกฟังก์ชั่น poweron()
    - ถ้าเปิดเครื่องสำเร็จ ให้เริ่มส่งคำสั่งเริ่มการอ่าน SELECT COMMAND(00A4040008A000000054480001) ต้องใช้คำสั่งนี้ทุกครั้งก่อนเริ่มอ่านข้อมูลอื่นๆ
    - ส่งคำสั่งอ่านข้อมูลต่างๆ โดยคำสั่งจะแบ่งเป็น 2 ชุดทุกข้อมูล เช่น เลขประจำตัวประชาชน 1.[80B0000402000D]-คำสั่งระบุตำแหน่งข้อมูล 2.[00C000000D]-คำสั่งระบุขนาดข้อมูล
    - เมื่อส่งคำสั่งไป บัตรจะตอบกลับมาเป็น ByteArray หากสำเร็จเราจะใช้คำตอบของคำสั่งชุดที่ 2 มาแปลงเป็น String และใช้รูปแบบอักขระเป็น charset("TIS620")
    - เมื่อแปลงเป็นอักขระภาษาไทยได้แล้ว จะต้องทำการจัดเรียงข้อมูลใหม่ โดยรูปแบบที่บัตรเก็บไว้จะใช้ # การเว้นวรรค การจัดเรียงข้อมูลให้ดูตามในฟังก์ชั่นไก้เลย
    - เมื่ออ่านข้อมูลเสร็จแล้ว จะทำการข้อมูลรูปภาพต่อโดยวิธีการอ่านในลักษณะเดียวกันแต่จะต้องอ่านข้อมูลทั้ง 20 ช่องแล้วเอามารวมกันก่อนถึงจะแปลงเป็นรูปภาพได้ ดูวิธีจาก ReadPicClick()
    - หลังจากอ่านข้อมูลทั้งหมดเสร็จเรียบร้อย จะทำการแสดงบนหน้าจอในช่องต่างๆ
    - ทำการบันทึกข้อมูลลงบนไฟล์รายงาน excel ใน writeToExcel()
    - ทำการบันทึกรูปลงใน Document\WACThaiID Android ในฟังก์ชั่น generateNoteOnSD
    */
    @SuppressLint("SetTextI18n")
    fun ReadCard() {

        PersonalInfoTextViewClear()
        mTextViewResult!!.text = "DONE."

        //****  SET POWER ON ****//
        val ret: Int
        ret = poweron()
        if (ret == SCError.READER_SUCCESSFUL) {
//***** GET ATR*********//
            val atr: String
            try {
                atr = mReader!!.atrString
                //mTextViewResult.setText(" ATR:"+ atr);
            } catch (e: Exception) {
                mStrMessage = "Get Exception : " + e.message
                mTextViewResult!!.text = mStrMessage
            }
            var recByte: ByteArray? = null
            //***********************//
            ////Chip ID
            recByte = SendAPDUcommand("80ca9f7f00")
            recByte = SendAPDUcommand("80ca9f7f2d")
            val chip_card = StringBuilder()
            for (i in 0..7) {
                chip_card.append(String.format("%02X", recByte!![i + 13]))
            }
//            txtChipCard!!.text = getString(R.string.ChipCard) + " : $chip_card"
            nativeCardInfo!!.chipId = chip_card.toString()
            ////////Laser ID
            recByte = SendAPDUcommand("00a4040008a000000084060002")
            recByte = SendAPDUcommand("8000000017")
            val buffer = ByteArray(recByte!!.size - 2)
            System.arraycopy(recByte, 0, buffer, 0, recByte.size - 2)
            val laserID: String = String(recByte, 7, 12)
            nativeCardInfo!!.laserId = laserID

            recByte = SendAPDUcommand("00A4040008A000000054480001") //SELECT COMMAND
            val arrayInfo: Array<String>

            recByte = SendAPDUcommand("80B0000402000D") //CID
            recByte = SendAPDUcommand("00C000000D") //CID

            var str_CID: String? = null
            try {
                str_CID = String(recByte!!, charset("TIS620"))
                str_CID = str_CID.substring(0, 13)
                txtCID!!.text = str_CID
                nativeCardInfo!!.cardNumber = str_CID
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            recByte = SendAPDUcommand("80B00011020064") //FULL NANE TH
            recByte = SendAPDUcommand("00C0000064") //FULL NANE TH
            var fn_th: Array<String>? = null
            try {
                fn_th =
                    String(recByte!!, charset("TIS620")).substring(0, 35).split("#").toTypedArray()
                var tilteNameTh = fn_th[0] // คำนำหน้า
                var firstNameTh = fn_th[1] // ชื่อ
                var middleNameTh = fn_th[2] // ว่าง
                var lastNameTh = fn_th[3].replaceFirst(" ", "") // last name

                txtFullnameTH!!.text = "$tilteNameTh $firstNameTh $middleNameTh $lastNameTh"
                nativeCardInfo!!.thaiTitle = tilteNameTh
                nativeCardInfo!!.thaiFirstName = firstNameTh
                nativeCardInfo!!.thaiMiddleName = middleNameTh
                nativeCardInfo!!.thaiLastName = lastNameTh


            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            recByte = SendAPDUcommand("80B00075020064") //FULL NANE EN
            recByte = SendAPDUcommand("00C0000064") //FULL NANE EN
            var fn_en: Array<String>? = null
            try {
                fn_en =
                    String(recByte!!, charset("TIS620")).substring(0, 35).split("#").toTypedArray()
                var tilteNameEn = fn_en[0] // คำนำหน้า
                var firstNameEn = fn_en[1] // ชื่อ
                var middleNameEn = fn_en[2] // ว่าง
                var lastNameEn = fn_en[3].replaceFirst(" ", "") // last name
                txtFullnameEN!!.text =
                    "$tilteNameEn $firstNameEn $middleNameEn $lastNameEn"
                nativeCardInfo!!.engTitle = tilteNameEn
                nativeCardInfo!!.engFirstName = firstNameEn
                nativeCardInfo!!.engMiddleName = middleNameEn
                nativeCardInfo!!.engLastName = lastNameEn
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }


            recByte = SendAPDUcommand("80B000D9020008") //DOB
            recByte = SendAPDUcommand("00C0000008") //DOB
            var str_DOB: String? = null
            try {
                str_DOB = String(recByte!!, charset("TIS620"))
                str_DOB = str_DOB.substring(
                    0,
                    8
                )
                str_DOB =
                    str_DOB.substring(6, 8) + "/" + str_DOB.substring(
                        4,
                        6
                    ) + "/" + str_DOB.substring(
                        0,
                        4
                    )
                txtDOB!!.text = "$str_DOB"
                nativeCardInfo!!.dateOfBirth = str_DOB
            } catch (e: UnsupportedEncodingException) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
                ("UnsupportedEncodingException :: $e")
                e.printStackTrace()
            }

            recByte = SendAPDUcommand("80B000E1020001") //GENDER
            recByte = SendAPDUcommand("00C0000001") //GENDER
            var str_Gender: String? = null
            try {
                str_Gender = String(recByte!!, charset("TIS620"))
                val b = str_Gender.startsWith("1")
                str_Gender = if (b == true) {
                    "ชาย"
                } else {
                    "หญิง"
                }
                txtGender!!.text = "$str_Gender"
                nativeCardInfo!!.sex = str_Gender
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            recByte = SendAPDUcommand("80B000E2020014") //RequestNum
            recByte = SendAPDUcommand("00C0000014") //RequestNum
            var str_RequestNum: String? = null
            try {
                str_RequestNum = String(recByte!!, charset("TIS620"))
                txtreq_number!!.text = formatString(str_RequestNum)

                nativeCardInfo!!.bp1No = formatString(str_RequestNum)
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
            recByte = SendAPDUcommand("80B000F6020064") //Issue place
            recByte = SendAPDUcommand("00C0000064") //Issue place
            var str_Issueplace: String? = null
            try {
                str_Issueplace = String(recByte!!, charset("TIS620"))
                txtIssuePlace!!.text = formatString(str_Issueplace)
                nativeCardInfo!!.cardIssuePlace = formatString(str_Issueplace)
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            recByte = SendAPDUcommand("80B0015A02000D") //Issue code
            recByte = SendAPDUcommand("00C000000D") //Issue code
            var str_Issuecode: String? = null
            try {
                str_Issuecode = String(recByte!!, charset("TIS620"))
                txtIssueCode!!.text = formatString(str_Issuecode)
                nativeCardInfo!!.cardIssueNo = formatString(str_Issuecode)
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            recByte = SendAPDUcommand("80B00167020012") //ISSUE/EXPIRE
            recByte = SendAPDUcommand("00C0000012") //ISSUE/EXPIRE

            var str_ISSUEEXPIRE: String? = null
            try {
                str_ISSUEEXPIRE = String(recByte!!, charset("TIS620"))
                var strIssue = str_ISSUEEXPIRE.substring(0, 8)
                var strExpire = str_ISSUEEXPIRE.substring(8, 16)
                strIssue = strIssue.substring(6, 8) + "/" + strIssue.substring( 4, 6) + "/" + strIssue.substring(0, 4)
                txtIssue!!.text = "$strIssue"
                nativeCardInfo!!.cardIssueDate = strIssue

                strExpire = strExpire.substring(6, 8) + "/" + strExpire.substring( 4, 6) + "/" + strExpire.substring(0, 4)
                txtExpire!!.text = "$strExpire"
                nativeCardInfo!!.cardExpiryDate = strExpire
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            recByte = SendAPDUcommand("80B00177020002") //Card Type
            recByte = SendAPDUcommand("00C0000002") //Card Type
            var str_CardType: String? = null
            try {
                str_CardType = String(recByte!!, charset("TIS620"))
                txtCardTypeCode!!.text = "${
                    formatString(
                        str_CardType
                    )
                }"
                nativeCardInfo!!.cardType = formatString(
                    str_CardType
                )
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            recByte = SendAPDUcommand("80B01579020064") //Address
            recByte = SendAPDUcommand("00C0000064") //Address
            var strAddress: Array<String>? = null
            var Address: String = ""
            try {
                Address = String(recByte!!, charset("TIS620"))
                Address = Address.replace("#", " ")
                Address = Address.replace("\\s+".toRegex(), " ")
                Address = Address.replace("\\0", "").replace("\u0000", "").replace(" \u0090", "")
                strAddress = String(recByte, charset("TIS620")).split("#").toTypedArray()
                val enpart12: String = strAddress[0] // บ้านเลขที่

                val enpart13: String = strAddress.get(1) // หมู่ที่

                val enpart14: String = strAddress.get(2)
                val enpart15: String = strAddress.get(3)
                val enpart16: String = strAddress.get(4)
                val enpart17: String = strAddress[5] // ตำบล

                val enpart18: String = strAddress.get(6) // อำเภอ

                val enpart19: String = formatString(strAddress.get(7))// จังหวัด

                val zipcode = findPostalCode(
                    enpart19,
                    enpart18
                )

                txtAddress!!.text =
                    "$enpart12 $enpart13 $enpart14 $enpart15 $enpart16 $enpart17 $enpart18 $enpart19 ${zipcode}"
                var address = CardAddress()
                address.homeNo = enpart12
                address.moo = enpart13
                address.trok = enpart14
                address.soi = enpart15
                address.road = enpart16
                address.subDistrict = enpart17
                address.district = enpart18
                address.province = enpart19
                address.postalCode = zipcode
                address.country = "ประเทศไทย"

                nativeCardInfo!!.address = address
                nativeCardInfo!!.cardCountry = address.country


            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            recByte = SendAPDUcommand("80b0161902000e") //Image code
            recByte = SendAPDUcommand("00c000000e") //Image code
            var str_ImgCode: String? = null
            try {
                str_ImgCode = String(recByte!!, charset("TIS620"))
                txtImgcode!!.text = "${formatString(str_ImgCode)}"
                nativeCardInfo!!.cardPhotoIssueNo = formatString(str_ImgCode)
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            recByte = SendAPDUcommand("80B00000020004") //Version
            recByte = SendAPDUcommand("00C0000004") //Version
            var str_Version: String? = null
            try {
                str_Version = String(recByte!!, charset("TIS620"))
                txtVersioncard!!.text =
                    "${formatString(str_Version)}"
                nativeCardInfo!!.versionCard = txtVersioncard!!.text.toString()
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            nativeCardInfo!!.statusCode = "1"
            nativeCardInfo!!.cardIssueNo = "null"

            ReadPicClick()


        } else if (ret == SCError.READER_NO_CARD) {
            dialog!!.cancel()
            runOnUiThread {
                Toast.makeText(mContext, "Card Absent", Toast.LENGTH_SHORT).show()
            }

            nativeCardInfo!!.statusCode = "0"
            nativeCardInfo!!.cardIssueNo = "null"

            poweroff()

        } else {
            dialog!!.cancel()
            runOnUiThread {
                Toast.makeText(mContext, "Power on fail", Toast.LENGTH_SHORT).show()
            }
            nativeCardInfo!!.statusCode = "0"
            nativeCardInfo!!.cardIssueNo = "null"

            poweroff()

        }
    }

    private fun onSuccess() {
        nativeCardInfo!!.statusCode = "1"
        nativeCardInfo!!.cardIssueNo = "null"

        runOnUiThread {
            sharedViewModel.setNativeCardInfo(nativeCardInfo!!)
        }
    }

    private fun findPostalCode(province: String, district: String): String {
        val jsonString = assets.readFile("zipcode.json")
        val gson = Gson()
        val arrayTutorialType = object : TypeToken<Array<PostalCode>>() {}.type
        val tutorials: Array<PostalCode> = gson.fromJson("""[$jsonString]""", arrayTutorialType)
        val findlist = (tutorials.filter {
            it.province == province.replace(
                "จังหวัด",
                ""
            ) && it.district == district.replace(
                "อำเภอ",
                ""
            )
        })
        var zipCode = ""
        for (i in findlist.indices) {
            zipCode = findlist[i].zip
        }
        return zipCode
    }


    fun ReadPicClick() {
        mTextViewResult!!.text = "DONE."
        val ret: Int
        ret = poweron()
        if (ret == SCError.READER_SUCCESSFUL) {

        } else if (ret == SCError.READER_NO_CARD) {
            runOnUiThread {
                Toast.makeText(mContext, "Card Absent", Toast.LENGTH_SHORT).show()
            }
            nativeCardInfo!!.statusCode = "0"
            nativeCardInfo!!.cardIssueNo = "null"

            poweroff()

        } else {
            runOnUiThread {
                Toast.makeText(mContext, "Power on fail", Toast.LENGTH_SHORT).show()
            }
            nativeCardInfo!!.statusCode = "0"
            nativeCardInfo!!.cardIssueNo = "null"

            poweroff()

        }
        var recByte: ByteArray? = null
        recByte = SendAPDUcommand("00A4040008A000000054480001") //SELECT COMMAND

        //#Photo
        val pRevAPDULen = IntArray(1)
        var COMMAND: ByteArray
        var recvBuffer = ByteArray(300)
        var hexstring = ""
        var tmp: String
        var r: Int
        r = 0
        pRevAPDULen[0] = 300
        while (r <= 20) {
            COMMAND = byteArrayOf(
                0x80.toByte(),
                0xB0.toByte(),
                (0x01.toByte() + r).toByte(),
                (0x7B.toByte() - r).toByte(),
                0x02.toByte(),
                0x00.toByte(),
                0xFF.toByte()
            )
            recvBuffer = ByteArray(2)
            mReader!!.transmit(COMMAND, COMMAND.size, recvBuffer, pRevAPDULen)

            COMMAND = byteArrayOf(
                0x00.toByte(),
                0xC0.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0xFF.toByte()
            )
            recvBuffer = ByteArray(257)
            mReader!!.transmit(COMMAND, COMMAND.size, recvBuffer, pRevAPDULen)
            val recvBufferClone = Arrays.copyOfRange(recvBuffer, 0, 255)
            tmp = bytesToHex(recvBufferClone)
            hexstring = hexstring + tmp
            r++
        }
        val byteRawHex = hexStringToByteArray(hexstring)
        val imgBase64String = Base64.encodeToString(byteRawHex, Base64.NO_WRAP)
        val bitmapCard = BitmapFactory.decodeByteArray(byteRawHex, 0, byteRawHex.size)
        cardPhoto!!.statusCode = "1"
        cardPhoto!!.photo = imgBase64String
        runOnUiThread {
            imgPhoto!!.setImageBitmap(bitmapCard)
        }
        var cardimage: MultipartBody.Part? = null

        val cardfile = File(this.cacheDir, "image")
        cardfile.createNewFile()
        val bos = ByteArrayOutputStream()
        bitmapCard.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos)
        val bitmapdata = bos.toByteArray()
        val fos = FileOutputStream(cardfile)
        fos.write(bitmapdata)
        fos.flush()
        fos.close()

        val requestcardFile: RequestBody = cardfile.asRequestBody("image/png".toMediaTypeOrNull())
        cardimage = MultipartBody.Part.createFormData(
            "image1",
            cardfile.name,
            requestcardFile
        )

        nativeCardInfo!!.strPicture = hexstring

        /**set information to data Class before upload*/
        val uploaddata = Upload.Data()
        uploaddata.statusCode = nativeCardInfo!!.statusCode
        uploaddata.smartDataImage = cardimage
        uploaddata.chipId = nativeCardInfo!!.chipId
        uploaddata.laserId = nativeCardInfo!!.laserId
        uploaddata.bp1No = nativeCardInfo!!.bp1No
        uploaddata.cardNumber = nativeCardInfo!!.cardNumber
        uploaddata.thaiTitle = nativeCardInfo!!.thaiTitle
        uploaddata.thaiFirstName = nativeCardInfo!!.thaiFirstName
        uploaddata.thaiMiddleName = nativeCardInfo!!.thaiMiddleName
        uploaddata.thaiLastName = nativeCardInfo!!.thaiLastName
        uploaddata.engTitle = nativeCardInfo!!.engTitle
        uploaddata.engFirstName = nativeCardInfo!!.engFirstName
        uploaddata.engMiddleName = nativeCardInfo!!.engMiddleName
        uploaddata.engLastName = nativeCardInfo!!.engLastName
        uploaddata.dateOfBirth = nativeCardInfo!!.dateOfBirth
        uploaddata.sex = nativeCardInfo!!.sex
        uploaddata.cardPhotoIssueNo = nativeCardInfo!!.cardPhotoIssueNo
        uploaddata.cardIssuePlace = nativeCardInfo!!.cardIssuePlace
//                            uploaddata.cardIssuerNo = nativeCardInfo!!.cardIssuerNo
        uploaddata.cardIssuerNo = ""
        uploaddata.cardIssueNo = nativeCardInfo!!.cardIssueNo
        uploaddata.cardIssueDate = nativeCardInfo!!.cardIssueDate
        uploaddata.cardExpiryDate = nativeCardInfo!!.cardExpiryDate
        uploaddata.cardType = nativeCardInfo!!.cardType
        uploaddata.versionCard = nativeCardInfo!!.versionCard


        val addressdata = Upload.Data.CardAddress()
        addressdata.homeNo = nativeCardInfo!!.address?.homeNo.toString()
        addressdata.soi = nativeCardInfo!!.address?.soi.toString()
        addressdata.trok = nativeCardInfo!!.address?.trok.toString()
        addressdata.moo = nativeCardInfo!!.address?.moo.toString()
        addressdata.road = nativeCardInfo!!.address?.road.toString()
        addressdata.subDistrict =
            nativeCardInfo!!.address?.subDistrict.toString()
        addressdata.district = nativeCardInfo!!.address?.district.toString()
        addressdata.province = nativeCardInfo!!.address?.province.toString()
        addressdata.postalCode = nativeCardInfo!!.address?.postalCode.toString()
        addressdata.country = nativeCardInfo!!.address?.country.toString()

        uploaddata.address = addressdata
        uploaddata.cardCountry = nativeCardInfo!!.cardCountry

        val photodata = Upload.Data.CardPhoto()
        photodata.statusCode = cardPhoto!!.statusCode
        photodata.photo = cardPhoto!!.photo

        uploaddata.photo = photodata

        val date = Date()
        val dipChipDateTime = date.dateToString("yyyy-MM-dd hh:mm:ss")
        nativeCardInfo!!.dipChipDateTime = dipChipDateTime

        /**create data for upload*/
        val builder: MultipartBody.Builder =
            MultipartBody.Builder().setType(MultipartBody.FORM)
        builder.addFormDataPart("mId", AppSettings.USER_ID)
            .addFormDataPart("uId", AppSettings.UID)
            .addFormDataPart(
                "smartDataImage",
                "image",
                requestcardFile
            )
            .addFormDataPart("bp1No", nativeCardInfo!!.bp1No)
            .addFormDataPart("chipId",  nativeCardInfo!!.chipId)
            .addFormDataPart("cardNumber",  nativeCardInfo!!.cardNumber)
            .addFormDataPart("thaiTitle",  nativeCardInfo!!.thaiTitle)
            .addFormDataPart("thaiFirstName", nativeCardInfo!!.thaiFirstName)
            .addFormDataPart("thaiMiddleName", nativeCardInfo!!.thaiMiddleName)
            .addFormDataPart("thaiLastName", nativeCardInfo!!.thaiLastName)
            .addFormDataPart("engTitle", nativeCardInfo!!.engTitle)
            .addFormDataPart("engFirstName", nativeCardInfo!!.engFirstName)
            .addFormDataPart("engMiddleName", nativeCardInfo!!.engMiddleName)
            .addFormDataPart("engLastName", nativeCardInfo!!.engLastName)
            .addFormDataPart("dateOfBirth", nativeCardInfo!!.dateOfBirth)
            .addFormDataPart("sex", nativeCardInfo!!.sex)
            .addFormDataPart("cardIssueNo", nativeCardInfo!!.cardIssueNo)
            .addFormDataPart("cardIssuePlace", nativeCardInfo!!.cardIssuePlace)
            .addFormDataPart("cardIssueDate", nativeCardInfo!!.cardIssueDate)
            .addFormDataPart("cardPhotoIssueNo", nativeCardInfo!!.cardPhotoIssueNo)
            .addFormDataPart("laserId", nativeCardInfo!!.laserId)
            .addFormDataPart("cardExpiryDate", nativeCardInfo!!.cardExpiryDate)
            .addFormDataPart("cardType", nativeCardInfo!!.cardType)
            .addFormDataPart("homeNo", nativeCardInfo!!.address!!.homeNo)
            .addFormDataPart("soi", nativeCardInfo!!.address!!.soi)
            .addFormDataPart("trok", nativeCardInfo!!.address!!.trok)
            .addFormDataPart("moo", nativeCardInfo!!.address!!.moo)
            .addFormDataPart("road", nativeCardInfo!!.address!!.road)
            .addFormDataPart("subDistrict", nativeCardInfo!!.address!!.subDistrict)
            .addFormDataPart("district", nativeCardInfo!!.address!!.district)
            .addFormDataPart("province", nativeCardInfo!!.address!!.province)
            .addFormDataPart("postalCode", nativeCardInfo!!.address!!.postalCode)
            .addFormDataPart("country", nativeCardInfo!!.address!!.country)
            .addFormDataPart("cardCountry", nativeCardInfo!!.cardCountry)

        val exp = nativeCardInfo!!.cardExpiryDate
        val christ = exp.substring(6, 10).toInt() - 543
        val newStrExpire = exp.substring(0, 6) + christ.toString().substring(2,4)

        val expireDateTime = LocalDate.parse(newStrExpire, DateTimeFormatter.ofLocalizedDate(
            FormatStyle.SHORT
            ).withLocale(Locale("th"))
        )
        val currentDate = LocalDate.now()
        if (currentDate > expireDateTime) {
            dialog!!.cancel()
            MaterialDialog(this@UsbReaderActivity).show {
                title(text = "คำเตือน")
                message(text = "บัตรหมดอายุ")
                negativeButton(text = "ยกเลิก")
                positiveButton(text = "ดำเนินการต่อ", click = {
                    if (AppSettings.IS_OFFLINE) {
                        addInJSONArray(nativeCardInfo!!)
                        val data = getDataFromSharedPreferences()
                        data?.let { println(" data ref: ${it.size}") }
                    } else {
                        val requestBody: RequestBody = builder.build()
                        uploadReader(requestBody)
                    }
                })
            }
        } else {
            dialog!!.cancel()
            println(nativeCardInfo!!.strPicture)
            if (AppSettings.IS_OFFLINE) {
                addInJSONArray(nativeCardInfo!!)
                val data = getDataFromSharedPreferences()
                data?.let { println(" data ref: ${it.size}") }
            } else {
                val requestBody: RequestBody = builder.build()
                uploadReader(requestBody)
            }
        }

        onSuccess()
    }

    private fun Date.dateToString(format: String): String {
        //simple date formatter
        val dateFormatter = SimpleDateFormat(format, Locale.getDefault())

        //return the formatted date string
        return dateFormatter.format(this)
    }

    fun getIpv4HostAddress(): String {
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { return it.hostAddress }
        }
        return ""
    }

    private fun getDataFromSharedPreferences(): List<NativeCardInfo?>? {
        val gson = Gson()
        var productFromShared: List<NativeCardInfo?>? = ArrayList()
        val sharedPref = applicationContext.getSharedPreferences("SharedPrefs${AppSettings.ID_USER_NAME}", MODE_PRIVATE)
        val jsonPreferences = sharedPref.getString("NativeCardInfo", "")
        val type = object : TypeToken<List<NativeCardInfo?>?>() {}.type
        productFromShared = gson.fromJson<List<NativeCardInfo?>>(jsonPreferences, type)
        return productFromShared
    }

    private fun addInJSONArray(productToAdd: NativeCardInfo) {
        val dialog = retrofitDialog(this)
        dialog?.show()
        val gson = Gson()
        val sharedPref = applicationContext.getSharedPreferences("SharedPrefs${AppSettings.ID_USER_NAME}", MODE_PRIVATE)
        val jsonSaved = sharedPref.getString("NativeCardInfo", "")
        val jsonNewproductToAdd = gson.toJson(productToAdd)
        var jsonArrayProduct = JSONArray()
        try {
            if (jsonSaved!!.length != 0) {
                jsonArrayProduct = JSONArray(jsonSaved)
            }
            jsonArrayProduct.put(JSONObject(jsonNewproductToAdd))
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        //SAVE NEW ARRAY
        val editor = sharedPref.edit()
        editor.putString("NativeCardInfo", jsonArrayProduct.toString())
        editor.commit()
        successDialog(dialog!!, "Add Data Success!!")
    }

    private fun checkForInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    fun CloseOnClick(view: View?) {
        PersonalInfoTextViewClear()
        CloseTask().execute()
    }

    object DateFormat {
        const val ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    }

    private fun String.toLocalDateTime(pattern: String = DateFormat.ISO_8601): LocalDateTime? = try {
        LocalDateTime.parse(this, DateTimeFormatter.ofPattern(pattern))
    } catch (exception: Exception) {
        null
    }

    //    ตัวจัดการเปิดการทำงาน USB reader
    private inner class OpenTask : AsyncTask<UsbDevice?, Void?, Int>() {
        override fun doInBackground(vararg p0: UsbDevice?): Int? {
            var status = 0
            try {
                status = InitReader()
                if (status != 0) {
                    ("fail to initial reader")
                    Log.e(TAG, "fail to initial reader")
                    return status
                }
                //status = mReader.connect();
            } catch (e: Exception) {
                mStrMessage = "Get Exception : " + e.message
                ("Open fail :: " + "Get Exception : " + e.message)
                mTextViewResult!!.text = mStrMessage
            }
            return status
        }

        override fun onPostExecute(result: Int) {
            if (result != 0) {
                mTextViewResult!!.text = "Open fail: " + Integer.toString(result)
                Log.e(TAG, "Open fail: " + Integer.toString(result))
                Toast.makeText(
                    this@UsbReaderActivity,
                    "Open fail: " + Integer.toString(result),
                    Toast.LENGTH_SHORT
                ).show()
                ("Open fail dataTop:: ${result.toString()} ::")
                onCloseButtonSetup()
                updateConnectionState(STATE_DISCONNECTED)
            } else {
                onOpenButtonSetup()
                mTextViewResult!!.text = "Open successfully"
                Log.e(TAG, "Open successfully")
                updateConnectionState(STATE_CONNECTED)
                PersonalInfoTextViewClear()
            }
        }


    }

    //ปิดการใช้งานเครื่องอ่านบัตร
    private fun closeReaderUp(): Int {
        Log.d(TAG, "Closing reader...")
        var ret = 0
        if (mReader != null) {
            ret = mReader!!.close()
        }
        return ret
    }

    //ปิดการเชื่อมต่อ USB
    private fun closeReaderBottom() {
        onCloseButtonSetup()
        cleanText()
        mMyDev!!.Close()
        mSlotNum = 0.toByte()
        updateConnectionState(STATE_DISCONNECTED)
    }

    private fun setUpCloseDialog() {
        mCloseProgress = ProgressDialog(this@UsbReaderActivity)
        mCloseProgress!!.setMessage("Closing Reader")
        mCloseProgress!!.setCancelable(false)
        mCloseProgress!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        mCloseProgress!!.show()
    }


    //ปิดการทำงาน USB reader
    private inner class CloseTask : AsyncTask<Void?, Void?, Int>() {
        override fun onPreExecute() {
            setUpCloseDialog()
        }

        override fun doInBackground(vararg p0: Void?): Int? {
            var status = 0
            try {
                do {
                    status = closeReaderUp()
                } while (status == SCError.READER_CMD_BUSY)
            } catch (e: Exception) {
                mStrMessage = "Get Exception : " + e.message
                mTextViewResult!!.text = mStrMessage
            }
            return status
        }

        override fun onPostExecute(result: Int) {
            if (result != 0) {
                mTextViewResult!!.text = "Close fail: " + Integer.toString(result)
                Log.e(TAG, "Close fail: " + Integer.toString(result))
                Toast.makeText(this@UsbReaderActivity, "cT", Toast.LENGTH_SHORT).show()
            } else {
                mTextViewResult!!.text = "Close successfully"
                Log.e(TAG, "Close successfully")
                Toast.makeText(this@UsbReaderActivity, "cB", Toast.LENGTH_SHORT).show()
            }
            Toast.makeText(this@UsbReaderActivity, "cA", Toast.LENGTH_SHORT).show()
            closeReaderBottom()
            mCloseProgress!!.dismiss()
        }
    }

    //จ่ายไฟเข้าบัตรก่อนทำการอ่านบัตร จะเป็นการเช็คว่าบัตรถูกเสียบอย่างถูกต้อง พร้อมอ่านหรือไม่
    private fun poweron(): Int {
        var result = SCError.READER_SUCCESSFUL
        Log.d(TAG, "poweron " + getSlotStatus())
        //check slot status first
        result = getSlotStatus()
        when (result) {
            SCError.READER_NO_CARD -> {
                mTextViewResult!!.text = "Card Absent"
                Log.d(TAG, "Card Absent")
                return SCError.READER_NO_CARD
            }
            SCError.READER_CARD_INACTIVE, SCError.READER_SUCCESSFUL -> {
            }
            else -> return result
        }
        result = mReader!!.setPower(Reader.CCID_POWERON)
        Log.d(TAG, "power on exit")
        return result
    }

    //ปิดการจ่ายไฟเข้าบัตรก่อนทำการอ่านบัตร
    private fun poweroff(): Int {
        var result = SCError.READER_SUCCESSFUL
        Log.d(TAG, "poweroff")
        result = getSlotStatus()
        when (result) {
            SCError.READER_NO_CARD -> {
                mTextViewResult!!.text = "Card Absent"
                Log.d(TAG, "Card Absent")
                return SCError.READER_NO_CARD
            }
            SCError.READER_CARD_INACTIVE, SCError.READER_SUCCESSFUL -> {
            }
            else -> return result
        }
        //----------poweroff card------------------
        result = mReader!!.setPower(Reader.CCID_POWEROFF)
        return result
    }

    fun cleanText() {
        mTextViewResult!!.text = ""
    }

    private fun onCreateButtonSetup() {
        mOpenButton!!.isEnabled = true
        mReadAll!!.isEnabled = false
        mCloseButton!!.isEnabled = false
    }

    private fun onOpenButtonSetup() {
        mOpenButton!!.isEnabled = false
        mCloseButton!!.isEnabled = true
        mReadAll!!.isEnabled = true
    }

    private fun onCloseButtonSetup() {
        mOpenButton!!.isEnabled = true
        mCloseButton!!.isEnabled = false
        mReadAll!!.isEnabled = false
    }

    private fun onDevPermit(dev: UsbDevice) {
        mUsbDev = dev
        try {
            updateViewReader()
            OpenTask().execute(dev)
        } catch (e: Exception) {
            mStrMessage = "Get Exception : " + e.message
            Log.e(TAG, mStrMessage!!)
        }
    }

    private fun onDetache(intent: Intent) {
        val udev = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
        if (udev != null) {
            if (udev == mUsbDev) {
                closeReaderUp()
                closeReaderBottom()
            }
        } else {
            Log.d(TAG, "usb device is null")
        }
    }

    //ทำงานเช็คสถานะการเชื่อมต่อ reader และเช็คอนุญาตการใช้งาน
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Broadcast Receiver")
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device =
                        intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                        if (device != null) {
                            onDevPermit(device)
                        } else {

                        }
                    } else {
                        Log.d(TAG, "Permission denied for device " + device!!.deviceName)
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                Log.d(TAG, "Device Detached")
                onDetache(intent)
                synchronized(this) { updateReaderList(intent) }
            }
        } /*end of onReceive(Context context, Intent intent) {*/
    }

    //Check USB device is AlcorReader
    private fun isAlcorReader(udev: UsbDevice?): Boolean {
        if (udev!!.vendorId == 0x058f
            && (udev.productId == 0x9540
                    || udev.productId == 0x9520 || udev.productId == 0x9522
                    || udev.productId == 0x9525 || udev.productId == 0x9526)
        ) return true else if (udev.vendorId == 0x2CE3
            && (udev.productId == 0x9571 || udev.productId == 0x9572
                    || udev.productId == 0x9563) || udev.productId == 0x9573
        ) {
            return true
        }
        return false
    }

    //Get device information and check device is alcorReader?
    private fun EnumeDev(): Int {
        var device: UsbDevice? = null
        val manager = getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager
        val deviceList = manager.deviceList
        val deviceIterator: Iterator<UsbDevice> = deviceList.values.iterator()
        Log.d(TAG, " EnumeDev")
//        mReaderAdapter!!.clear()
        while (deviceIterator.hasNext()) {
            device = deviceIterator.next()
            Log.d(
                TAG,
                " " + Integer.toHexString(device.vendorId) + " " + Integer.toHexString(device.productId)
            )
            if (isAlcorReader(device)) {
                Log.d(TAG, "Found Device")
//                mReaderAdapter!!.add(device.deviceName)
                mDeviceName = device.deviceName
            }
        }
        requestDevPerm()
        return 0
    }

    //    สั่งเปิดใช้งาน USB reader ที่เชื่อมต่อและผ่านการอนุญาตการใช้งานแล้ว
    private fun InitReader(): Int {
        var Status = 0
        val init: Boolean //
        Log.d(TAG, "InitReader")
        try {
            init = mMyDev?.Init(mManager, mUsbDev)!!

            if (!init) {
                Log.e(TAG, "Device init fail")
                return -1
            }
        } catch (e: ReaderHwException) {
            Log.e(
                TAG,
                "Get ReaderHwException : " + e.message
            )
            return -1
        }
        try {
            mReader = Reader(mMyDev)
            Status = mReader!!.open()
        } catch (e: ReaderException) {
            Log.e(
                TAG,
                "InitReader fail " + "Get Exception : " + e.message
            )
            return -1
        }
        mReader!!.setSlot(mSlotNum)
        return Status
    }

    private fun toByteArray(hexString: String): ByteArray {
        val hexStringLength = hexString.length
        var byteArray: ByteArray? = null
        var count = 0
        var c: Char
        var i: Int

        // Count number of hex characters
        i = 0
        while (i < hexStringLength) {
            c = hexString[i]
            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || (c >= 'a'
                        && c <= 'f')
            ) {
                count++
            }
            i++
        }
        byteArray = ByteArray((count + 1) / 2)
        var first = true
        var len = 0
        var value: Int
        i = 0
        while (i < hexStringLength) {
            c = hexString[i]
            value = if (c >= '0' && c <= '9') {
                c - '0'
            } else if (c >= 'A' && c <= 'F') {
                c - 'A' + 10
            } else if (c >= 'a' && c <= 'f') {
                c - 'a' + 10
            } else {
                -1
            }
            if (value >= 0) {
                if (first) {
                    byteArray[len] = (value shl 4).toByte()
                } else {
                    byteArray[len] = byteArray[len] or value.toByte()
                    len++
                }
                first = !first
            }
            i++
        }
        return byteArray
    }

    companion object {
        const val STATE_DISCONNECTED = 0
        const val STATE_DISCONNECTING = 3
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
        private const val TAG = "Alcor-Test"
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
        const val MULTIPLE_PERMISSIONS = 100
        private val hexArray = "0123456789ABCDEF".toCharArray()
        private fun bytesToHex(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2)
            for (j in bytes.indices) {
                val v = bytes[j].toInt() and 0xFF // Here is the conversion
                hexChars[j * 2] = hexArray[v.ushr(4)]
                hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            }
            return String(hexChars)
        }

        private fun hexStringToByteArray(s: String): ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                        + Character.digit(s[i + 1], 16)).toByte()
                i += 2
            }
            return data
        }

        private fun formatString(raw: String): String {
            var str_format = raw.replace(" ".toRegex(), "")
            str_format = str_format.replace("\n".toRegex(), "")
            str_format = str_format.replace("\u0090".toRegex(), "")
            str_format = str_format.replace("##".toRegex(), " ")
            str_format = str_format.replace("#".toRegex(), " ")
            str_format = str_format.replace("\\s".toRegex(), " ")
            str_format = str_format.replace("\\0", "").replace("\u0000", "")

            return str_format
        }

    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {

    }

    private fun uploadReader(data: RequestBody) {

//        data.mId = AppSettings.USER_ID
//        data.mId = AppSettings.UID

//        val dialog = retrofitDialog(this)
//        dialog?.show()

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
                data
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
                    successDialog(dialog!!, userResponse.message)
                    val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this@UsbReaderActivity)
                    alertDialog.apply {
                        setTitle("Success")
                        setMessage("Upload complete")
                        setPositiveButton("okay.") { _: DialogInterface?, _: Int ->
                        }
                    }.create().show()
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        Log.d(TAG, "onError:HttpException ${e.localizedMessage}")
                        try {
                            val jObjError = JSONObject(e.response()!!.errorBody()?.string())
                            Log.d(ReaderActivity.TAG, jObjError.getString("message"))
                            if (jObjError.getString("message") == "Unauthorized") {
                                token(AppSettings.REFRESH_TOKEN, data)
                            } else if ((jObjError.getString("message")) == "No address associated with hostname") {
                                errorDialog(dialog!!, "เกิดข้อผิดพลาด เพื่อให้ใช้งานต่อได้ กรุณา login ใหม่เพื่อเปิดใช้โหมด OffLine ")
                            } else {
                                PublicFunction().errorDialogSetTitle(dialog!!, (jObjError.getString("message"))).show()
                            }
                        } catch (e: Exception) {
                            Log.d(ReaderActivity.TAG, e.toString())
                            PublicFunction().errorDialogSetTitle(dialog!!, e.localizedMessage).show()
                        }
                    } else {
                        Log.d(TAG, "onError: $e")
                        errorDialog(dialog!!, "เกิดข้อผิดพลาด เพื่อให้ใช้งานต่อได้ กรุณา login ใหม่เพื่อเปิดใช้โหมด OffLine ")
                    }
                    e.printStackTrace()
                }
            })
    }

    private fun token(token: String, data: RequestBody) {
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
                    AppSettings.USER_NAME = AppSettings.ID_USER_NAME
                    AppSettings.USER_RULE = rule.toString().toUpperCase()
                    Log.d(TAG, "onNext: ${AppSettings.ACCESS_TOKEN}")
                    Log.d(TAG, "onNext: ${AppSettings.USER_ID}")

                    uploadReader(data)
                }

                override fun onError(e: Throwable) {
                    val intent: Intent =
                        Intent(this@UsbReaderActivity, LoginActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
            })
    }


    fun successDialog(dialog: SweetAlertDialog, message: String?): SweetAlertDialog {
        dialog.setTitleText(message)
            .setConfirmText("Close")
            .setConfirmButtonBackgroundColor(
                ContextCompat.getColor(
                    dialog.context,
                    R.color.Green_ForestGreen
                )
            )
            .setConfirmClickListener(null)
            .changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
        return dialog
    }
    fun errorDialog(dialog: SweetAlertDialog, message: String?): SweetAlertDialog {
        dialog.setTitleText(message)
            .setConfirmText("Close")
            .setConfirmButtonBackgroundColor(
                ContextCompat.getColor(
                    dialog.context,
                    R.color.Red_Crimson
                )
            )
            .setConfirmClickListener(null)
            .changeAlertType(SweetAlertDialog.ERROR_TYPE)
        return dialog
    }
    fun retrofitDialog(context: Context): SweetAlertDialog? {
        return SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE)
            .setConfirmButtonBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.Green_MediumSpringGreen
                )
            )
            .setTitleText("Please wait...")
    }
}
