package com.wac.wacthaiidcloud

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.acs.bluetooth.*
import com.acs.bluetooth.BluetoothReaderManager.OnReaderDetectionListener
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
import mpay.sdk.lib.BTLib
import mpay.sdk.lib.interfaces.BluetoothListener
import mpay.sdk.lib.interfaces.CommandListener
import mpay.sdk.lib.model.DevItem
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.Buffer
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
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.*
import java.time.format.FormatStyle
import java.util.*
import kotlin.experimental.or


class ReaderActivity : AppCompatActivity(), BluetoothListener, CommandListener,
    View.OnClickListener {
    private var sharedPreferences: SharedPreferences? = null
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var countdata = 1
    private var countphoto = 1
    private var strpicture = ""
    var byteRawHex: ByteArray? = null
    private var mConnectState = STATE_DISCONNECTED
    private var mProgressDialog: ProgressDialog? = null

    // BT Lib
    var btLib: BTLib? = null
    private var progressBar: ProgressBar? = null
    private var read_btn: Button? = null
    private var cardpicture: ImageView? = null

    private var mViewPager: ViewPager? = null
    private var adapter: FragmentPagerAdapter? = null
    private lateinit var sharedViewModel: SharedViewModel
    var nativeCardInfo: NativeCardInfo? = NativeCardInfo()
    var cardPhoto: CardPhoto? = CardPhoto()

    /* Detected reader. */
    private var mBluetoothReader: BluetoothReader? = null

    /* ACS Bluetooth reader library. */
    private var mBluetoothReaderManager: BluetoothReaderManager? = null
    private var mGattCallback: BluetoothReaderGattCallback? = null

//    private var mProgressDialog: ProgressDialog? = null

    /* Bluetooth GATT client. */
    private var mBluetoothGatt: BluetoothGatt? = null
    var countRead = 1

    var dialog: SweetAlertDialog? = null

    private val mBroadcastReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                var bluetoothAdapter: BluetoothAdapter? = null
                var bluetoothManager: BluetoothManager? = null
                val action = intent.action
                if (mBluetoothReader !is Acr3901us1Reader) {
                    /* Only ACR3901U-S1 require bonding. */
                    return
                }
                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                    Log.i(TAG, "ACTION_BOND_STATE_CHANGED")

                    /* Get bond (pairing) state */if (mBluetoothReaderManager == null) {
                        Log.w(
                            TAG,
                            "Unable to initialize BluetoothReaderManager."
                        )
                        return
                    }
                    bluetoothManager =
                        getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
                    if (bluetoothManager == null) {
                        Log.w(TAG, "Unable to initialize BluetoothManager.")
                        return
                    }
                    bluetoothAdapter = bluetoothManager.adapter
                    if (bluetoothAdapter == null) {
                        Log.w(TAG, "Unable to initialize BluetoothAdapter.")
                        return
                    }
                    val device = bluetoothAdapter
                        .getRemoteDevice(mDeviceAddress) ?: return
                    val bondState = device.bondState

                    // TODO: remove log message
                    Log.i(
                        TAG, "BroadcastReceiver - getBondState. state = "
                                + getBondingStatusString(bondState)
                    )

                    /* Enable notification */if (bondState == BluetoothDevice.BOND_BONDED) {
                        if (mBluetoothReader != null) {
                            (mBluetoothReader as Acr3901us1Reader).enableNotification(
                                true
                            )
                        }
                    }

                    /* Progress Dialog */if (bondState == BluetoothDevice.BOND_BONDING) {
                        mProgressDialog = ProgressDialog.show(
                            context,
                            "ACR3901U-S1", "Bonding..."
                        )
                    } else {
                        if (mProgressDialog != null) {
                            mProgressDialog!!.dismiss()
                            mProgressDialog = null
                        }
                    }

                    /*
                 * Update bond status and show in the connection status field.
                 */runOnUiThread {
                        //                        mTxtConnectionState.setText(getBondingStatusString(bondState));
                    }
                }
            }
        }

    override fun onResume() {
        Log.i(TAG, "onResume()")
        super.onResume()
        val intentFilter = IntentFilter()

        /* Start to monitor bond state change */intentFilter.addAction(
            BluetoothDevice.ACTION_BOND_STATE_CHANGED
        )
        registerReceiver(mBroadcastReceiver, intentFilter)

        /* Clear unused dialog. */
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
            mProgressDialog = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)
        sharedViewModel = run { ViewModelProviders.of(this)[SharedViewModel::class.java] }
        val intent = intent
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)
        sharedPreferences = getSharedPreferences(
            getString(R.string.PrefsSetting),
            MODE_PRIVATE
        )
        try {
            if (btLib == null) {
                btLib = BTLib(this, true)
                //                btLib = btLib;
                btLib!!.setCommandListener(this)
                btLib!!.setBTListener(this)
                Log.d(TAG, "BTlib create!!")
                btLib!!.btStart()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "BT initial failed : $e")
            btLib = null
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
//            ActivityCompat.requestPermissions(
//                this, arrayOf(
//                    Manifest.permission.ACCESS_COARSE_LOCATION,
//                    Manifest.permission.ACCESS_FINE_LOCATION,
//                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
//                    Manifest.permission.BLUETOOTH_ADMIN,
//                    Manifest.permission.BLUETOOTH,
//                    Manifest.permission.BLUETOOTH_SCAN
//                ),
//                REQUEST_ACCESS_COARSE_LOCATION
//            )
            return
        }

        /* Initialize BluetoothReaderGattCallback. */
        mGattCallback = BluetoothReaderGattCallback()

        /* Register BluetoothReaderGattCallback's listeners */
        mGattCallback!!.setOnConnectionStateChangeListener { gatt, state, newState ->
            runOnUiThread(Runnable {
                if (state != BluetoothGatt.GATT_SUCCESS) {
                    /** Show the message on fail to * connect/disconnect.*/
                    mConnectState = BluetoothReader.STATE_DISCONNECTED
                    if (newState == BluetoothReader.STATE_CONNECTED) {
                        //                                        mTxtConnectionState.setText(R.string.connect_fail);
                    } else if (newState == BluetoothReader.STATE_DISCONNECTED) {
                        //                                        mTxtConnectionState.setText(R.string.disconnect_fail);
                    }
                    invalidateOptionsMenu()
                    return@Runnable
                }
                updateConnectionState(newState)
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    /* Detect the connected reader. */
                    mBluetoothReaderManager?.detectReader(
                        gatt, mGattCallback
                    )
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    mBluetoothReader = null
                    /** Release resources occupied by Bluetooth * GATT client.*/
                    if (mBluetoothGatt != null) {
                        mBluetoothGatt!!.close()
                        mBluetoothGatt = null
                    }
                }
            })
        }
        /** Initialize mBluetoothReaderManager. */
        mBluetoothReaderManager = BluetoothReaderManager()
        /** Register BluetoothReaderManager's listeners */
        mBluetoothReaderManager!!.setOnReaderDetectionListener(
            OnReaderDetectionListener { reader ->
                if (reader is Acr3901us1Reader) {
                    /* The connected reader is ACR3901U-S1 reader. */
                    Log.v(TAG, "On Acr3901us1Reader Detected.")
                } else if (reader is Acr1255uj1Reader) {
                    /* The connected reader is ACR1255U-J1 reader. */
                    Log.v(TAG, "On Acr1255uj1Reader Detected.")
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "The device is not supported!",
                            Toast.LENGTH_SHORT
                        ).show()

                        /* Disconnect Bluetooth reader */Log.v(
                        TAG,
                        "Disconnect reader!!!"
                    )
                        disconnectReader()
                        updateConnectionState(BluetoothReader.STATE_DISCONNECTED)
                    }
                    return@OnReaderDetectionListener
                }
                mBluetoothReader = reader
                setListener(reader)
                activateReader(reader)
            })

        initView()
        initViewPager()
        connectReader()
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )
        supportActionBar!!.title = mDeviceName
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    private fun initView() {
        progressBar = findViewById(R.id.progress)
        progressBar!!.visibility = View.GONE
        read_btn = findViewById(R.id.read_btn)
        read_btn!!.setOnClickListener(this)

        cardpicture = findViewById(R.id.cardpicture)
    }

    private fun initViewPager() {
        adapter = PagerAdapter(supportFragmentManager)
        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById<View>(R.id.viewpager) as ViewPager
        mViewPager!!.adapter = adapter

        val tabLayout = findViewById<View>(R.id.tabs) as TabLayout
        tabLayout.setupWithViewPager(mViewPager)
    }

    private fun updateConnectionState(connectState: Int) {
        mConnectState = connectState
        if (connectState == BluetoothReader.STATE_CONNECTING) {
//            mTxtConnectionState.setText(R.string.connecting);
        } else if (connectState == BluetoothReader.STATE_CONNECTED) {
//            mTxtConnectionState.setText(R.string.connected);
        } else if (connectState == BluetoothReader.STATE_DISCONNECTING) {
//            mTxtConnectionState.setText(R.string.disconnecting);
        } else {
//            mTxtConnectionState.setText(R.string.disconnected);
        }
        invalidateOptionsMenu()
    }

    fun getIpv4HostAddress(): String {
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { return it.hostAddress }
        }
        return ""
    }

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

    private fun AssetManager.readFile(fileName: String) = open(fileName)
        .bufferedReader()
        .use { it.readText() }

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

    override fun onPause() {
        super.onPause()
        /* Stop to monitor bond state change */
        unregisterReceiver(
            mBroadcastReceiver
        )

        /* Disconnect Bluetooth reader */
        disconnectReader()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_connect -> {
                /* Connect Bluetooth reader */Log.v(TAG, "Start to connect!!!")
                connectReader()
                return true
            }

            R.id.menu_connecting, R.id.menu_disconnect -> {
                /* Disconnect Bluetooth reader */Log.v(TAG, "Start to disconnect!!!")
                disconnectReader()
                return true
            }

            android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.menu_about -> {
                val manager = this@ReaderActivity.packageManager
                val info = manager.getPackageInfo(
                    this@ReaderActivity.packageName,
                    PackageManager.GET_ACTIVITIES
                )

                MaterialDialog(this@ReaderActivity).show {
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
                    Intent(this@ReaderActivity, LoginActivity::class.java)
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

    private fun activateReader(reader: BluetoothReader?) {
        if (reader == null) {
            return
        }
        if (reader is Acr3901us1Reader) {
            /* Start pairing to the reader. */
            (mBluetoothReader as Acr3901us1Reader).startBonding()
        } else if (mBluetoothReader is Acr1255uj1Reader) {
            /* Enable notification. */
            (mBluetoothReader as Acr1255uj1Reader).enableNotification(true)
        }
    }

    private fun disconnectReader() {
        val deviceName = mDeviceName!!.substring(0, 3)
        if (deviceName == "ACR") {
            if (mBluetoothGatt == null) {
                updateConnectionState(BluetoothReader.STATE_DISCONNECTED)
                return
            }
            updateConnectionState(BluetoothReader.STATE_DISCONNECTING)
            mBluetoothGatt!!.disconnect()
        } else {
            if (btLib != null) {
                btLib!!.disconnectBTDevice()
            }
            updateConnectionState(BluetoothReader.STATE_DISCONNECTING)
        }
    }

    private fun connectReader() {
        val devicename = mDeviceName!!.substring(0, 3)
        println("devicename $devicename")
        if (devicename == "ACR") {
            /* Only ACR3901U-S1 require bonding. */
//            return
            val bluetoothManager =
                getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            if (bluetoothManager == null) {
                Log.w(TAG, "Unable to initialize BluetoothManager.")
                return
            }
            val bluetoothAdapter = bluetoothManager.adapter
            if (bluetoothAdapter == null) {
                Log.w(TAG, "Unable to obtain a BluetoothAdapter.")
                return
            }

            /*
             * Connect Device.
             */
            /* Clear old GATT connection. */if (mBluetoothGatt != null) {
                Log.i(TAG, "Clear old GATT connection")
                mBluetoothGatt!!.disconnect()
                mBluetoothGatt!!.close()
                mBluetoothGatt = null
            }

            /* Create a new connection. */
            val device = bluetoothAdapter
                .getRemoteDevice(mDeviceAddress)
            if (device == null) {
                Log.w(TAG, "Device not found. Unable to connect.")
                return
            }

            /* Connect to GATT server. */
            updateConnectionState(BluetoothReader.STATE_CONNECTING)
            mBluetoothGatt = device.connectGatt(this, true, mGattCallback)
            return
        } else {
            val name = mDeviceName
            val address = mDeviceAddress
            if (address!!.isNotEmpty()) {
                if (!btLib!!.connectBTDeviceByAddress(address)) {
                    Log.d(TAG, "Bluetooth Mode - Connect to $address fail")
                } else {
                    Log.d(TAG, "Bluetooth Mode - Connect to $address success")
                }
            } else if (name!!.isNotEmpty()) {
                if (!btLib!!.connectBTDeviceByName(name)) {
                    Log.d(TAG, "Bluetooth Mode - Connect to $name fail")
                } else {
                    Log.d(TAG, "Bluetooth Mode - Connectting to $name ")
                }
            } else {
                if (!btLib!!.scanBTDevice()) {
                    Log.d(TAG, "Bluetooth Mode - Scan failed ")
                } else  //                createScanningWindow();
                    Log.v(
                        TAG,
                        "btLib.scanBTDevice()" + if (btLib!!.scanBTDevice()) "true" else "false"
                    )
            }
        }
    }

    // Smart card index:0
    private fun sendSmartCard() {
        if (btLib != null) {
            Log.d(TAG, "Set Smart Card Port. ")
            btLib!!.cmdSetICCPort(0, 5000)
        }
    }

    // Sam card index: 1
    private fun sendSamCard() {
        if (btLib != null) {
//            PutMessage(">>>>>> Set Sam Card Port.");
            btLib!!.cmdSetICCPort(1, 5000)
        }
    }

    private fun sendICCStatus() {
        if (btLib != null) {
//            PutMessage(">>>>>> ICC Status.");
            btLib!!.cmdICCStatus(3000)
            Log.d(TAG, "sendICCStatus: success")
        } else {
            Log.d(TAG, "sendICCStatus: fail")
        }
    }

    private fun sendICCPowerOn() {
        if (btLib != null) {
            Log.d(TAG, ">>>>>> ICC Power On.")
            btLib!!.cmdICCPowerOn(3000)
        } else {
            Log.d(TAG, "sendICCPowerOn: fail")
        }
    }

    private fun sendICCPowerOff() {
        if (btLib != null) {
            Log.d(TAG, ">>>>>> ICC Power Off.")
            btLib!!.cmdICCPowerOff(3000)
        } else {
            Log.d(TAG, "sendICCPowerOff: fail")
        }
    }

    private fun sendICCAccess(cmd: String) {
        if (btLib != null) {
//             c_APDU = edit_ICC_Access.getText().toString().trim().replaceAll(" ", "");
            btLib!!.cmdICCAccess(cmd, 15000)
            Log.d(TAG, "sendICCAccess: success")
        } else {
            Log.d(TAG, "sendICCAccess: fail")
        }
    }

    override fun onBluetoothState(enable: Boolean) {}
    override fun onBluetoothDeviceScaning() {}
    override fun onBluetoothDeviceFound(item: DevItem) {}
    override fun onBluetoothDeviceScanOver() {}
    override fun onBluetoothDeviceBounding() {}
    override fun onBluetoothDeviceBoundSuccess() {}
    override fun onBluetoothDeviceBoundFailed() {}
    override fun onBluetoothDeviceConnecting() {
        Log.d(TAG, "onBluetoothDeviceConnecting: ")
        mConnectState = STATE_CONNECTING
        read_btn!!.isEnabled = false

        invalidateOptionsMenu()
    }

    override fun onBluetoothDeviceConnected() {
        Log.d(TAG, "onBluetoothDeviceConnected: ")
        mConnectState = STATE_CONNECTED
        read_btn!!.isEnabled = true
        invalidateOptionsMenu()
        sendSmartCard()
    }

    override fun onBluetoothDeviceConnectFailed() {}
    override fun onBluetoothDeviceDisconnected() {
        Log.d(TAG, "onBluetoothDeviceDisconnected: ")
        mConnectState = STATE_DISCONNECTED
        read_btn!!.isEnabled = false

        invalidateOptionsMenu()
    }

    override fun onReaderResponse(returnCode: Int, returnMessage: String, functionName: String) {
        Log.d(
            TAG, """Reader Response : 
               Function : $functionName
               Return Code : $returnCode
               Return Message : $returnMessage"""
        )

        if (returnMessage == "Data error" && returnCode == 36) {
            dialog!!.cancel()
            read_btn!!.text = "Read"
            read_btn!!.isEnabled = true
            progressBar!!.visibility = View.GONE
        }

    }

    override fun onSDKResponse(returnCode: Int, returnMessage: String, functionName: String) {
        // TODO Auto-generated method stub
//        PutMessage(">> " + "SDK Response : " + "\n   Function : " + functionName + "\n   Return Code : " + returnCode
//                + "\n   Return Message : " + returnMessage);
        Log.d(
            TAG, """SDK Response : 
   Function : $functionName
   Return Code : $returnCode
   Return Message : $returnMessage"""
        )
    }

    override fun onGetVersion(status: Boolean, version: String) {
        // TODO Auto-generated method stub
        Log.d(
            TAG, """onGetVersion Response : 
   status : $status
   version : $version"""
        )
    }

    override fun onICCStatus(status: Boolean, iccStatus: String) {
        Log.d(TAG, "onICCStatus: $status $iccStatus")
        if (status) {
            if (iccStatus.length > 1) {
                if (iccStatus == STATUS_INSERTED) {
                    read_btn!!.text = ""
                    read_btn!!.isEnabled = false

                    countdata = 1
                    countphoto = 1
                    strpicture = ""
                    progressBar!!.visibility = View.VISIBLE

                    sendICCPowerOn()

//                    sendICCPowerOff()
                } else if (iccStatus == STATUS_NOT_INSERT) {
                    val alertDialog = AlertDialog.Builder(this)
//                    Alerter.create(this)
//                        .setTitle("Alert")
//                        .setText(STATUS_NOT_INSERT)
//                        .setBackgroundColorRes(R.color.Red_Crimson) // or setBackgroundColorInt(Color.CYAN)
//                        .show()
                    alertDialog.apply {
                        setTitle("Alert")
                        setMessage(STATUS_NOT_INSERT)
                        setPositiveButton("okay.") { _: DialogInterface?, _: Int ->
                        }
                    }.create().show()
                }
            }
        }
    }

    override fun onICCPowerOn(status: Boolean, atr: String) {
        // TODO Auto-generated method stub
        if (status) {
            if (atr.length > 1) {
                Log.d(TAG, "onICCPowerOn: >> ATR : $atr")
//                PutMessage(">> ATR : " + atr);
                sendICCAccess(CHIP_ID_APDU_COMMAND)
//                    sendICCAccess(CHIP_ID_APDU_LE)
                sendICCAccess(LASER_ID_APDU_COMMAND)
                sendICCAccess(LASER_ID_APDU_LE)
                sendICCAccess(DEFAULT_3901_APDU_COMMAND)
                sendICCAccess(IDCARD_APDU_COMMAND)
                sendICCAccess(IDCARD_APDU_LE)
                sendICCAccess(NAME_TH_APDU_COMMAND)
                sendICCAccess(NAME_TH_APDU_LE)
                sendICCAccess(NAME_EN_APDU_COMMAND)
                sendICCAccess(NAME_EN_APDU_LE)
                sendICCAccess(GENDER_APDU_COMMAND)
                sendICCAccess(GENDER_APDU_LE)
                sendICCAccess(DOB_APDU_COMMAND)
                sendICCAccess(DOB_APDU_LE)
                sendICCAccess(Address_APDU_COMMAND)
                sendICCAccess(Address_APDU_LE)
                sendICCAccess(RequestNum_APDU_COMMAND)
                sendICCAccess(RequestNum_APDU_LE)
                sendICCAccess(Issue_place_APDU_COMMAND)
                sendICCAccess(Issue_place_APDU_LE)
                sendICCAccess(Issue_code_APDU_COMMAND)
                sendICCAccess(Issue_code_APDU_LE)
                sendICCAccess(ISSUE_EXPIRE_APDU_COMMAND)
                sendICCAccess(ISSUE_EXPIRE_APDU_LE)
                sendICCAccess(Card_Type_APDU_COMMAND)
                sendICCAccess(Card_Type_APDU_LE)
                sendICCAccess(Version_APDU_COMMAND)
                sendICCAccess(Version_APDU_LE)
                sendICCAccess(Image_code_APDU_COMMAND)
                sendICCAccess(Image_code_APDU_LE)
                sendICCAccess(DEFAULT_3901_APDU_COMMAND)
                for (s in codephoto) {
                    sendICCAccess(s)
                    sendICCAccess(Code_APDU_LE)
                }
            }
        }
    }

    override fun onICCPowerOff(status: Boolean) {
        // TODO Auto-generated method stub
        if (status) {
            Log.d(TAG, "onICCPowerOff: >> ATR : $status")
//            PutMessage(">>	" + "Success");
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onICCAccess(status: Boolean, rAPUD: String) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onICCAccess: $status $status")
        if (status) {
            if (rAPUD.length > 1) {
                if (
                    rAPUD == "610A" || rAPUD == "610D" || rAPUD == "6164" || rAPUD == "6101" || rAPUD == "6108" ||
                    rAPUD == "6114" || rAPUD == "6112" || rAPUD == "6102" || rAPUD == "6104" || rAPUD == "610E" ||
                    rAPUD == "61FF"
                ) {
                } else {
                    var str: String? = null
                    if (countdata == 1) {
                        val chip_card = StringBuilder()
                        for (i in 0..15) {
                            println(rAPUD[i + 26])
                            chip_card.append(rAPUD[i + 26])
                        }
                        nativeCardInfo!!.chipId = chip_card.toString()
                    }
                    try {
                        val bytes = hexStringToByteArray(rAPUD)
                        str = String(bytes, Charset.forName("TIS620"))
                        println(str.toString())
                        str = str.replace("\n".toRegex(), "")
                        println(str.toString())
                        str = str.replace("\u0090".toRegex(), "")
                        println(str.toString())
                        str = str.replace("\u0010".toRegex(), "")
                        println(str.toString())
                        str = str.replace("\\s+".toRegex(), " ")
                        println(str.toString())
                        str = str.replace("\\0", "").replace("\u0000", "")
                        println(str.toString())
                        if (str == "1") {
                            str = "ชาย"
                        }
                        if (str == "2") {
                            str = "หญิง"
                        }
                    } catch (e: UnsupportedEncodingException) {
                        Log.d(TAG, e.toString())
                        e.printStackTrace()
                    }
                    if (countdata == 2) {
                        str = str!!.replace("[|?*<\":>+\\[\\]/\b']".toRegex(), "")
                        nativeCardInfo!!.laserId =
                            str.toString().replace("\b", "")
                                .trimStart().trimEnd()
                    } else if (countdata == 3) {
                        nativeCardInfo!!.cardNumber = str.toString().trim()
                    } else if (countdata == 4) {
                        var fn_th: Array<String>? = str?.split("#")?.toTypedArray()
                        nativeCardInfo!!.thaiTitle = fn_th?.get(0).toString()
                        nativeCardInfo!!.thaiFirstName = fn_th?.get(1).toString()
                        nativeCardInfo!!.thaiMiddleName = fn_th?.get(2).toString()
                        nativeCardInfo!!.thaiLastName =
                            fn_th?.get(3).toString().replaceFirst(" ", "")
                    } else if (countdata == 5) {
                        var fn_en: Array<String>? = str?.split("#")?.toTypedArray()
                        nativeCardInfo!!.engTitle = fn_en?.get(0).toString()
                        nativeCardInfo!!.engFirstName = fn_en?.get(1).toString()
                        nativeCardInfo!!.engMiddleName = fn_en?.get(2).toString()
                        nativeCardInfo!!.engLastName =
                            fn_en?.get(3).toString().replaceFirst(" ", "")
                    } else if (countdata == 6) {
                        nativeCardInfo!!.sex = str.toString()
                    } else if (countdata == 7) {
                        str = str!!.substring(0, 8)
                        nativeCardInfo!!.dateOfBirth =
                            str.substring(6, 8) + "/" + str.substring(4, 6) + "/" + str.substring(
                                0,
                                4
                            )
                    } else if (countdata == 8) {
                        var strAddress: Array<String>? = str!!.split("#").toTypedArray()
                        var address = CardAddress()
                        address.homeNo = strAddress!![0]
                        address.moo = strAddress[1]
                        address.trok = strAddress[2]
                        address.soi = strAddress[3]
                        address.road = strAddress[4]
                        address.subDistrict = strAddress[5]
                        address.district = strAddress[6]
                        address.province = formatString(strAddress[7])
                        address.postalCode = findPostalCode(
                            address.province,
                            address.district
                        )
                        address.country = "ประเทศไทย"
                        nativeCardInfo!!.address = address
                        nativeCardInfo!!.cardCountry = address.country
                    } else if (countdata == 9) {
                        nativeCardInfo!!.bp1No = str.toString()
                    } else if (countdata == 10) {
                        nativeCardInfo!!.cardIssuePlace = str.toString().trim()
                    } else if (countdata == 11) {
                        nativeCardInfo!!.cardIssueNo = str.toString().trim()
                    } else if (countdata == 12) {
                        var strIssue = str!!.substring(0, 8)
                        var strExpire = str.substring(8, 16)
                        strIssue = strIssue.substring(6, 8) + "/" + strIssue.substring(
                            4,
                            6
                        ) + "/" + strIssue.substring(0, 4)
                        nativeCardInfo!!.cardIssueDate = strIssue
                        strExpire = strExpire.substring(6, 8) + "/" + strExpire.substring(
                            4,
                            6
                        ) + "/" + strExpire.substring(0, 4)
                        nativeCardInfo!!.cardExpiryDate = strExpire

                    } else if (countdata == 13) {
                        nativeCardInfo!!.cardType = str.toString()
                    } else if (countdata == 14) {
                        nativeCardInfo!!.versionCard = str.toString()
                    } else if (countdata == 15) {
                        nativeCardInfo!!.cardPhotoIssueNo = str.toString()
                    } else if (countdata > 15) {
                        val bytes = hexStringToByteArray(rAPUD)
                        val newResult = ByteArray(255)
                        print("newResult: $newResult")
                        for (i in 0..254) {
                            newResult[i] = bytes[i % bytes.size]
                        }
                        println("newResult: $newResult")
                        strpicture += bytesToHex(newResult)
                        countphoto++
                        if (countphoto == 21) {
                            println("strpicture: $strpicture")
                            byteRawHex = hexStringToByteArray(strpicture)
                            val imgBase64String = Base64.encodeToString(byteRawHex, Base64.NO_WRAP)
                            val bitmapCard =
                                BitmapFactory.decodeByteArray(byteRawHex, 0, byteRawHex!!.size)

                            cardPhoto!!.statusCode = "1"
                            cardPhoto!!.photo = imgBase64String
                            cardpicture!!.setImageBitmap(bitmapCard)

                            var cardimage: MultipartBody.Part? = null

                            val cardfile = File(this.cacheDir, "image")
                            cardfile.createNewFile()
                            val bos = ByteArrayOutputStream()
                            bitmapCard.compress(CompressFormat.PNG, 0 /*ignored for PNG*/, bos)
                            val bitmapdata = bos.toByteArray()
                            val fos = FileOutputStream(cardfile)
                            fos.write(bitmapdata)
                            fos.flush()
                            fos.close()

                            val requestcardFile: RequestBody =
                                cardfile.asRequestBody("image/png".toMediaTypeOrNull())
                            cardimage = MultipartBody.Part.createFormData(
                                "image1",
                                cardfile.name,
                                requestcardFile
                            )

                            nativeCardInfo!!.strPicture = strpicture

                            read_btn!!.text = "Read"
                            read_btn!!.isEnabled = true
                            progressBar!!.visibility = View.GONE

                            val uploaddata = Upload.Data()
                            uploaddata.statusCode = "xxx"
                            uploaddata.smartDataImage = cardimage
                            uploaddata.chipId = "xxx"
                            uploaddata.laserId = "xxx"
                            uploaddata.bp1No = "xxx"
                            uploaddata.cardNumber = "xxx"
                            uploaddata.thaiTitle = "xxx"
                            uploaddata.thaiFirstName = "xxx"
                            uploaddata.thaiMiddleName = "xxx"
                            uploaddata.thaiLastName = "xxx"
                            uploaddata.engTitle = "xxx"
                            uploaddata.engFirstName = "xxx"
                            uploaddata.engMiddleName = "xxx"
                            uploaddata.engLastName = "xxx"
                            uploaddata.dateOfBirth = "xxx"
                            uploaddata.sex = "xxx"
                            uploaddata.cardPhotoIssueNo = "xxx"
                            uploaddata.cardIssuePlace = "xxx"
                            uploaddata.cardIssuerNo = "xxx"
                            uploaddata.cardIssueNo = nativeCardInfo!!.cardIssueNo
                            uploaddata.cardIssueDate = "xxx"
                            uploaddata.cardExpiryDate = "xxx"
                            uploaddata.cardType = "xxx"
                            uploaddata.versionCard = "xxx"

                            val addressdata = Upload.Data.CardAddress()
                            addressdata.homeNo = "xxx"
                            addressdata.soi = "xxx"
                            addressdata.trok = "xxx"
                            addressdata.moo = "xxx"
                            addressdata.road = "xxx"
                            addressdata.subDistrict = "xxx"
                            addressdata.district = "xxx"
                            addressdata.province = "xxx"
                            addressdata.postalCode = "xxx"
                            addressdata.country = "xxx"

                            uploaddata.address = addressdata
                            uploaddata.cardCountry = "xxx"
                            uploaddata.versionCard = nativeCardInfo!!.versionCard

                            val photodata = Upload.Data.CardPhoto()
                            photodata.statusCode = "xxx"
                            photodata.photo = "xxx"

                            uploaddata.photo = photodata

//                            val date = Date()
//                            val dipChipDateTime = date.dateToString("yyyy-MM-dd hh:mm:ss")
                            val dipChipDateTime =
                                SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault()).format(
                                    Date()
                                )
                            println("dipChipDateTime: $dipChipDateTime")
                            nativeCardInfo!!.dipChipDateTime = dipChipDateTime

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
                                .addFormDataPart("chipId", nativeCardInfo!!.chipId)
                                .addFormDataPart("cardNumber", nativeCardInfo!!.cardNumber)
                                .addFormDataPart("thaiTitle", nativeCardInfo!!.thaiTitle)
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
                                .addFormDataPart(
                                    "cardPhotoIssueNo",
                                    nativeCardInfo!!.cardPhotoIssueNo
                                )
                                .addFormDataPart("laserId", nativeCardInfo!!.laserId)
                                .addFormDataPart("cardExpiryDate", nativeCardInfo!!.cardExpiryDate)
                                .addFormDataPart("cardType", nativeCardInfo!!.cardType)
                                .addFormDataPart("homeNo", nativeCardInfo!!.address!!.homeNo)
                                .addFormDataPart("soi", nativeCardInfo!!.address!!.soi)
                                .addFormDataPart("trok", nativeCardInfo!!.address!!.trok)
                                .addFormDataPart("moo", nativeCardInfo!!.address!!.moo)
                                .addFormDataPart("road", nativeCardInfo!!.address!!.road)
                                .addFormDataPart(
                                    "subDistrict",
                                    nativeCardInfo!!.address!!.subDistrict
                                )
                                .addFormDataPart("district", nativeCardInfo!!.address!!.district)
                                .addFormDataPart("province", nativeCardInfo!!.address!!.province)
                                .addFormDataPart(
                                    "postalCode",
                                    nativeCardInfo!!.address!!.postalCode
                                )
                                .addFormDataPart("country", nativeCardInfo!!.address!!.country)
                                .addFormDataPart("cardCountry", nativeCardInfo!!.cardCountry)

                                .addFormDataPart("channel", "dipchip via MOBILE")
                                .addFormDataPart(
                                    "dipChipDateTime",
                                    nativeCardInfo!!.dipChipDateTime
                                )
                                .addFormDataPart("ipaddress", getIpv4HostAddress())

                            val exp = nativeCardInfo!!.cardExpiryDate
                            println("exp: $exp")
                            if (exp != "99/99/9999") {
                                val christ = exp.substring(6, 10).toInt() - 543
                                val newStrExpire =
                                    exp.substring(0, 6) + christ.toString().substring(2, 4)
                                val expireDateTime = LocalDate.parse(
                                    newStrExpire,
                                    ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale("th"))
                                )
                                val currentDate = LocalDate.now()
                                if (currentDate > expireDateTime) {
                                    dialog!!.cancel()
                                    MaterialDialog(this@ReaderActivity).show {
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
                                    if (AppSettings.IS_OFFLINE) {
                                        addInJSONArray(nativeCardInfo!!)
                                        val data = getDataFromSharedPreferences()
                                        data?.let { println(" data ref: ${it.size}") }
                                    } else {
                                        val requestBody: RequestBody = builder.build()
                                        uploadReader(requestBody)
                                    }
                                }
                            } else {
                                dialog!!.cancel()
                                if (AppSettings.IS_OFFLINE) {
                                    addInJSONArray(nativeCardInfo!!)
                                    val data = getDataFromSharedPreferences()
                                    data?.let { println(" data ref: ${it.size}") }
                                } else {
                                    val requestBody: RequestBody = builder.build()
                                    uploadReader(requestBody)
                                }
                            }
                        }
                    }
                    onSuccess()
                    countdata++
                }
            }
        }
    }

    private fun Date.dateToString(format: String): String {
        //simple date formatter
        val dateFormatter = SimpleDateFormat(format, Locale.getDefault())

        //return the formatted date string
        return dateFormatter.format(this)
    }

    private fun onSuccess() {

        nativeCardInfo!!.statusCode = "1"

        if (nativeCardInfo!!.address == null) {
            nativeCardInfo!!.address = CardAddress()
        }

        sharedViewModel.setNativeCardInfo(nativeCardInfo!!)


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

    private fun uploadReader(data: RequestBody) {

//        data.mId = AppSettings.USER_ID
//        data.mId = AppSettings.UID

        val dialog = retrofitDialog(this)
        dialog?.show()

        try {
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
                        val alertDialog = AlertDialog.Builder(this@ReaderActivity)
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
                                Log.d(TAG, jObjError.getString("message"))
                                if (jObjError.getString("message") == "Unauthorized") {
                                    token(AppSettings.REFRESH_TOKEN, data, dialog!!)
                                } else if ((jObjError.getString("message")) == "No address associated with hostname") {
                                    errorDialog(
                                        dialog!!,
                                        "เกิดข้อผิดพลาด เพื่อให้ใช้งานต่อได้ กรุณา login ใหม่เพื่อเปิดใช้โหมด OffLine "
                                    )
                                } else {
                                    PublicFunction().errorDialogSetTitle(
                                        dialog!!,
                                        (jObjError.getString("message"))
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, e.toString())
                                PublicFunction().errorDialogSetTitle(dialog!!, e.localizedMessage)
                                    .show()
                            }
                        } else {
                            Log.d(TAG, "onError: $e")
                            PublicFunction().errorDialogSetTitle(dialog!!, e.localizedMessage)
                                .show()
                        }
                        e.printStackTrace()
                    }
                })
        } catch (e: Exception) {
            PublicFunction().errorDialogSetTitle(dialog!!, e.localizedMessage).show()
        }
    }

    private fun token(token: String, data: RequestBody, dialog: SweetAlertDialog) {

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
                    errorTokenDialog(
                        dialog,
                        "เกิดข้อผิดพลาด Session หมดอายุ เพื่อให้ใช้งานต่อได้ กรุณา login ใหม่")
                }
            })
    }

    /** Listener ACR */

    @SuppressLint("SetTextI18n")
    private fun setListener(reader: BluetoothReader) {
        /* Update status change listener */
        if (mBluetoothReader is Acr3901us1Reader) {
            (mBluetoothReader as Acr3901us1Reader)
                .setOnBatteryStatusChangeListener { bluetoothReader, batteryStatus ->
                    Log.i(
                        TAG, "mBatteryStatusListener data: "
                                + batteryStatus
                    )
                    runOnUiThread {
                        //                                    mTxtBatteryStatus.setText(getBatteryStatusString(batteryStatus));
                    }
                }
        } else if (mBluetoothReader is Acr1255uj1Reader) {
            (mBluetoothReader as Acr1255uj1Reader)
                .setOnBatteryLevelChangeListener { bluetoothReader, batteryLevel ->
                    Log.i(
                        TAG, "mBatteryLevelListener data: "
                                + batteryLevel
                    )
                    runOnUiThread {
                        //                                    mTxtBatteryLevel.setText(getBatteryLevelString(batteryLevel));
                    }
                }
        }
        mBluetoothReader?.setOnCardStatusChangeListener(
            BluetoothReader.OnCardStatusChangeListener { bluetoothReader, sta ->
                Log.i(
                    TAG,
                    "mCardStatusListener sta: $sta $countRead"
                )
                runOnUiThread(Runnable {
                    if (sta == 1 && countdata > 1 && progressBar!!.visibility == View.VISIBLE) {
                        readCardErrorDialog()
                        cardpicture!!.setImageResource(R.drawable.icons8_contacts_108px_1)

                        nativeCardInfo = NativeCardInfo()
                        nativeCardInfo!!.address = CardAddress()
                        sharedViewModel.setNativeCardInfo(nativeCardInfo!!)

//                        progressBar!!.visibility = View.GONE
//                        read_btn!!.text = ""
//                        read_btn!!.isEnabled = false
                        countdata = 1
                        countRead = 1
                        countphoto = 1
                        strpicture = ""

                    } else if (sta == 2) {
//                        progressBar!!.visibility = View.GONE
//                        read_btn!!.text = ""
//                        read_btn!!.isEnabled = false
                        countdata = 1
                        countRead = 1
                        countphoto = 1
                        strpicture = ""
                        if (mBluetoothReader == null) {
                            //                    mTxtAuthentication.setText(R.string.card_reader_not_ready);
                            readCardErrorDialog()
                            return@Runnable
                        }
                    }
                })
            })

        /* Wait for authentication completed. */
        mBluetoothReader?.setOnAuthenticationCompleteListener(
            BluetoothReader.OnAuthenticationCompleteListener { bluetoothReader, errorCode ->
                runOnUiThread(Runnable {
                    Log.d("errorcode", errorCode.toString())
                    if (errorCode == BluetoothReader.ERROR_SUCCESS) {
                        //                                    mTxtAuthentication
                        //                                            .setText("Authentication Success!");
                        if (mBluetoothReader == null) {
                            //                                        mTxtATR.setText(R.string.card_reader_not_ready);
                            readCardErrorDialog()
                            return@Runnable
                        }
                        if (!mBluetoothReader!!.powerOnCard()) {
                            //                                        mTxtATR.setText(R.string.card_reader_not_ready);
                            readCardErrorDialog()
                        } else {
                            /* Check for detected reader. */
                            if (mBluetoothReader == null) {
                                //                                            mTxtResponseApdu.setText(R.string.card_reader_not_ready);
                                readCardErrorDialog()
                                return@Runnable
                            } else {
                                /* Retrieve APDU command from edit box. */
                                readCardInfo()
                            }
                        }
                    } else {
                        //                                    mTxtAuthentication.setText("Authentication Failed!");
                        readCardErrorDialog()
                    }
                })
            })

        /* Wait for receiving ATR string. */
        mBluetoothReader?.setOnAtrAvailableListener(BluetoothReader.OnAtrAvailableListener { bluetoothReader, atr, errorCode ->
            runOnUiThread {
                if (atr == null) {
                    //                                    mTxtATR.setText(getErrorString(errorCode));
                } else {
                    //                                    mTxtATR.setText(Utils.toHexString(atr));
                }
            }
        })

        /* Wait for power off response. */
        mBluetoothReader?.setOnCardPowerOffCompleteListener(
            BluetoothReader.OnCardPowerOffCompleteListener { bluetoothReader, result ->
                runOnUiThread {
                }
            })

        /* Wait for response APDU. */
        mBluetoothReader?.setOnResponseApduAvailableListener(
            BluetoothReader.OnResponseApduAvailableListener { bluetoothReader, apdu, errorCode ->
                runOnUiThread {

                    if (errorCode == BluetoothReader.ERROR_SUCCESS) {
                        progressBar!!.visibility = View.VISIBLE
                        read_btn!!.text = ""
                        read_btn!!.isEnabled = false
                        val hexapdu = bytesToHex(apdu)
                        if (countRead == 3 && hexapdu != "610A") {
                            readCardErrorDialog()
                        } else {
                            if (
                                hexapdu == "6C2D" || hexapdu == "610A" || hexapdu == "610D" || hexapdu == "6164" || hexapdu == "6101" || hexapdu == "6108" ||
                                hexapdu == "6114" || hexapdu == "6112" || hexapdu == "6102" || hexapdu == "6104" || hexapdu == "610E" ||
                                hexapdu == "61FF"
                            ) {
                            } else {
                                var str: String? = null
                                if (countdata == 1) {
                                    val chip_card = StringBuilder()
                                    for (i in 0..15) {
                                        chip_card.append(hexapdu[i + 26])
                                    }
                                    nativeCardInfo!!.chipId =
                                        chip_card.toString()
                                }
                                try {
                                    val bytes = hexStringToByteArray(hexapdu)
                                    str =
                                        String(bytes, Charset.forName("TIS620"))
                                    str = str.replace("\n".toRegex(), "")
                                    str = str.replace("\u0090".toRegex(), "")
                                    str = str.replace("\u0010".toRegex(), "")
                                    str = str.replace("\\s+".toRegex(), " ")
                                    str = str.replace("\\0", "")
                                        .replace("\u0000", "")
                                    if (str == "1") {
                                        str = "ชาย"
                                    }
                                    if (str == "2") {
                                        str = "หญิง"
                                    }
                                } catch (e: UnsupportedEncodingException) {
                                    Log.d(TAG, e.toString())
                                    e.printStackTrace()
                                }
                                if (countdata == 2) {
//                                    str = str!!.replace("[|?*<\":>+\\[\\]/\b' ]".toRegex(), "")
                                    val laserId = str.toString().replace("\b", "")
                                        .replace("\\s".toRegex(), "")
                                        .replace("[^A-Za-z0-9]".toRegex(), "")
                                        .replace(" ", "")
                                    nativeCardInfo!!.laserId = laserId
                                    println("laserId3: ${nativeCardInfo!!.laserId}")
                                    println("laserId4: $laserId")
                                } else if (countdata == 3) {
                                    nativeCardInfo!!.cardNumber =
                                        str.toString().trim()
//                                    TxtCID!!.text = str
                                } else if (countdata == 4) {
                                    var fn_th: Array<String>? =
                                        str?.split("#")?.toTypedArray()
                                    nativeCardInfo!!.thaiTitle =
                                        fn_th?.get(0).toString()
                                    nativeCardInfo!!.thaiFirstName =
                                        fn_th?.get(1).toString()
                                    nativeCardInfo!!.thaiMiddleName =
                                        fn_th?.get(2).toString()
                                    nativeCardInfo!!.thaiLastName =
                                        fn_th?.get(3).toString()
                                            .replaceFirst(" ", "")
//                                    TxtNameth!!.text = str
                                } else if (countdata == 5) {
                                    var fn_en: Array<String>? =
                                        str?.split("#")?.toTypedArray()
                                    nativeCardInfo!!.engTitle =
                                        fn_en?.get(0).toString()
                                    nativeCardInfo!!.engFirstName =
                                        fn_en?.get(1).toString()
                                    nativeCardInfo!!.engMiddleName =
                                        fn_en?.get(2).toString()
                                    nativeCardInfo!!.engLastName =
                                        fn_en?.get(3).toString()
                                            .replaceFirst(" ", "")
//                                    TxtNameEn!!.text = str
                                } else if (countdata == 6) {
                                    nativeCardInfo!!.sex = str.toString()
//                                    TxtGender!!.text = str
                                } else if (countdata == 7) {
                                    str = str!!.substring(0, 8)
                                    nativeCardInfo!!.dateOfBirth =
                                        str.substring(
                                            6,
                                            8
                                        ) + "/" + str.substring(
                                            4,
                                            6
                                        ) + "/" + str.substring(
                                            0,
                                            4
                                        )
//                                    TxtDOB!!.text =
                                    str.substring(
                                        6,
                                        8
                                    ) + "/" + str.substring(
                                        4,
                                        6
                                    ) + "/" + str.substring(
                                        0,
                                        4
                                    )
                                } else if (countdata == 8) {
                                    var strAddress: Array<String>? =
                                        str!!.split("#").toTypedArray()
                                    var address = CardAddress()
                                    address.homeNo = strAddress!![0]
                                    address.moo = strAddress[1]
                                    address.trok = strAddress[2]
                                    address.soi = strAddress[3]
                                    address.road = strAddress[4]
                                    address.subDistrict = strAddress[5]
                                    address.district = strAddress[6]
                                    address.province =
                                        formatString(strAddress[7])
                                    address.postalCode = findPostalCode(
                                        address.province,
                                        address.district
                                    )
                                    address.country = "ประเทศไทย"
                                    nativeCardInfo!!.address = address
                                    nativeCardInfo!!.cardCountry =
                                        address.country
//                                    TxtAddress!!.text = str
                                } else if (countdata == 9) {
                                    nativeCardInfo!!.bp1No = str.toString()
                                } else if (countdata == 10) {
                                    nativeCardInfo!!.cardIssuePlace =
                                        str.toString()
                                } else if (countdata == 11) {
                                    nativeCardInfo!!.cardIssueNo =
                                        str.toString()
                                } else if (countdata == 12) {
                                    var strIssue = str!!.substring(0, 8)
                                    var strExpire = str.substring(8, 16)
                                    strIssue =
                                        strIssue.substring(
                                            6,
                                            8
                                        ) + "/" + strIssue.substring(
                                            4,
                                            6
                                        ) + "/" + strIssue.substring(0, 4)
                                    nativeCardInfo!!.cardIssueDate = strIssue
                                    strExpire = strExpire.substring(
                                        6,
                                        8
                                    ) + "/" + strExpire.substring(
                                        4,
                                        6
                                    ) + "/" + strExpire.substring(0, 4)
                                    nativeCardInfo!!.cardExpiryDate = strExpire

                                } else if (countdata == 13) {
                                    nativeCardInfo!!.cardType = str.toString()
                                } else if (countdata == 14) {
                                    nativeCardInfo!!.versionCard =
                                        str.toString()
                                } else if (countdata == 15) {
                                    nativeCardInfo!!.cardPhotoIssueNo =
                                        str.toString()
                                } else if (countdata > 15) {
                                    val bytes = hexStringToByteArray(hexapdu)
                                    val newResult = ByteArray(255)
                                    for (i in 0..254) {
                                        newResult[i] = bytes[i % bytes.size]
                                    }
                                    strpicture += bytesToHex(newResult)
                                    countphoto++
                                    if (countphoto == 21) {
                                        byteRawHex = hexStringToByteArray(strpicture)
                                        var imgBase64String: String? = ""
                                        imgBase64String = Base64.encodeToString(
                                            byteRawHex,
                                            Base64.NO_WRAP
                                        )
                                        Log.d("imgggg", imgBase64String)
                                        val bitmapCard =
                                            BitmapFactory.decodeByteArray(
                                                byteRawHex,
                                                0,
                                                byteRawHex!!.size
                                            )

                                        cardPhoto!!.statusCode = "1"
                                        cardPhoto!!.photo = imgBase64String
                                        cardpicture!!.setImageBitmap(bitmapCard)

                                        var cardimage: MultipartBody.Part? = null

                                        val cardfile = File(this.cacheDir, "image")
                                        cardfile.createNewFile()
                                        val bos = ByteArrayOutputStream()
                                        bitmapCard.compress(
                                            CompressFormat.PNG,
                                            0 /*ignored for PNG*/,
                                            bos
                                        )
                                        val bitmapdata = bos.toByteArray()
                                        val fos = FileOutputStream(cardfile)
                                        fos.write(bitmapdata)
                                        fos.flush()
                                        fos.close()

                                        val requestcardFile: RequestBody =
                                            cardfile.asRequestBody("image/png".toMediaTypeOrNull())

                                        cardimage = MultipartBody.Part.createFormData(
                                            "image1",
                                            cardfile.name,
                                            requestcardFile
                                        )
                                        nativeCardInfo!!.strPicture = strpicture

                                        read_btn!!.text = "Read"
                                        read_btn!!.isEnabled = true
                                        progressBar!!.visibility = View.GONE

                                        val uploaddata = Upload.Data()
                                        uploaddata.statusCode = "xxx"
                                        uploaddata.smartDataImage = cardimage
                                        uploaddata.chipId = "xxx"
                                        uploaddata.laserId = "xxx"
                                        uploaddata.bp1No = "xxx"
                                        uploaddata.cardNumber = "xxx"
                                        uploaddata.thaiTitle = "xxx"
                                        uploaddata.thaiFirstName = "xxx"
                                        uploaddata.thaiMiddleName = "xxx"
                                        uploaddata.thaiLastName = "xxx"
                                        uploaddata.engTitle = "xxx"
                                        uploaddata.engFirstName = "xxx"
                                        uploaddata.engMiddleName = "xxx"
                                        uploaddata.engLastName = "xxx"
                                        uploaddata.dateOfBirth = "xxx"
                                        uploaddata.sex = "xxx"
                                        uploaddata.cardPhotoIssueNo = "xxx"
                                        uploaddata.cardIssuePlace = "xxx"
                                        uploaddata.cardIssuerNo = "xxx"
                                        uploaddata.cardIssueNo = nativeCardInfo!!.cardIssueNo
                                        uploaddata.cardIssueDate = "xxx"
                                        uploaddata.cardExpiryDate = "xxx"
                                        uploaddata.cardType = "xxx"
                                        uploaddata.versionCard = "xxx"

                                        val addressdata = Upload.Data.CardAddress()
                                        addressdata.homeNo = "xxx"
                                        addressdata.soi = "xxx"
                                        addressdata.trok = "xxx"
                                        addressdata.moo = "xxx"
                                        addressdata.road = "xxx"
                                        addressdata.subDistrict = "xxx"
                                        addressdata.district = "xxx"
                                        addressdata.province = "xxx"
                                        addressdata.postalCode = "xxx"
                                        addressdata.country = "xxx"

                                        uploaddata.address = addressdata
                                        uploaddata.cardCountry = "xxx"
                                        uploaddata.versionCard = nativeCardInfo!!.versionCard

                                        val photodata = Upload.Data.CardPhoto()
                                        photodata.statusCode = "xxx"
                                        photodata.photo = "xxx"

                                        uploaddata.photo = photodata

//                                        val date = Date()
//                                        val dipChipDateTime = date.dateToString("yyyy-MM- dd hh:mm:ss")
                                        val dipChipDateTime = SimpleDateFormat(
                                            "yyyy-MM-dd hh:mm:ss",
                                            Locale.getDefault()
                                        ).format(Date())
                                        println("dipChipDateTime: $dipChipDateTime")
                                        nativeCardInfo!!.dipChipDateTime = dipChipDateTime
                                        println("dipChipDateTime: ${nativeCardInfo!!.laserId}")
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
                                            .addFormDataPart("chipId", nativeCardInfo!!.chipId)
                                            .addFormDataPart(
                                                "cardNumber",
                                                nativeCardInfo!!.cardNumber
                                            )
                                            .addFormDataPart(
                                                "thaiTitle",
                                                nativeCardInfo!!.thaiTitle
                                            )
                                            .addFormDataPart(
                                                "thaiFirstName",
                                                nativeCardInfo!!.thaiFirstName
                                            )
                                            .addFormDataPart(
                                                "thaiMiddleName",
                                                nativeCardInfo!!.thaiMiddleName
                                            )
                                            .addFormDataPart(
                                                "thaiLastName",
                                                nativeCardInfo!!.thaiLastName
                                            )
                                            .addFormDataPart("engTitle", nativeCardInfo!!.engTitle)
                                            .addFormDataPart(
                                                "engFirstName",
                                                nativeCardInfo!!.engFirstName
                                            )
                                            .addFormDataPart(
                                                "engMiddleName",
                                                nativeCardInfo!!.engMiddleName
                                            )
                                            .addFormDataPart(
                                                "engLastName",
                                                nativeCardInfo!!.engLastName
                                            )
                                            .addFormDataPart(
                                                "dateOfBirth",
                                                nativeCardInfo!!.dateOfBirth
                                            )
                                            .addFormDataPart("sex", nativeCardInfo!!.sex)
                                            .addFormDataPart(
                                                "cardIssueNo",
                                                nativeCardInfo!!.cardIssueNo
                                            )
                                            .addFormDataPart(
                                                "cardIssuePlace",
                                                nativeCardInfo!!.cardIssuePlace
                                            )
                                            .addFormDataPart(
                                                "cardIssueDate",
                                                nativeCardInfo!!.cardIssueDate
                                            )
                                            .addFormDataPart(
                                                "cardPhotoIssueNo",
                                                nativeCardInfo!!.cardPhotoIssueNo
                                            )
                                            .addFormDataPart("laserId", nativeCardInfo!!.laserId)
                                            .addFormDataPart(
                                                "cardExpiryDate",
                                                nativeCardInfo!!.cardExpiryDate
                                            )
                                            .addFormDataPart("cardType", nativeCardInfo!!.cardType)
                                            .addFormDataPart(
                                                "homeNo",
                                                nativeCardInfo!!.address!!.homeNo
                                            )
                                            .addFormDataPart("soi", nativeCardInfo!!.address!!.soi)
                                            .addFormDataPart(
                                                "trok",
                                                nativeCardInfo!!.address!!.trok
                                            )
                                            .addFormDataPart("moo", nativeCardInfo!!.address!!.moo)
                                            .addFormDataPart(
                                                "road",
                                                nativeCardInfo!!.address!!.road
                                            )
                                            .addFormDataPart(
                                                "subDistrict",
                                                nativeCardInfo!!.address!!.subDistrict
                                            )
                                            .addFormDataPart(
                                                "district",
                                                nativeCardInfo!!.address!!.district
                                            )
                                            .addFormDataPart(
                                                "province",
                                                nativeCardInfo!!.address!!.province
                                            )
                                            .addFormDataPart(
                                                "postalCode",
                                                nativeCardInfo!!.address!!.postalCode
                                            )
                                            .addFormDataPart(
                                                "country",
                                                nativeCardInfo!!.address!!.country
                                            )
                                            .addFormDataPart(
                                                "cardCountry",
                                                nativeCardInfo!!.cardCountry
                                            )
                                            .addFormDataPart("channel", "dipchip via MOBILE")
                                            .addFormDataPart(
                                                "dipChipDateTime",
                                                nativeCardInfo!!.dipChipDateTime
                                            )
                                            .addFormDataPart("ipaddress", getIpv4HostAddress())

                                        val exp = nativeCardInfo!!.cardExpiryDate
                                        val christ = exp.substring(6, 10).toInt() - 543
                                        val newStrExpire =
                                            exp.substring(0, 6) + christ.toString().substring(2, 4)
                                        println("newStrExpire: $newStrExpire")
                                        val expireDateTime = LocalDate.parse(
                                            newStrExpire, ofLocalizedDate(
                                                FormatStyle.SHORT
                                            ).withLocale(Locale("th"))
                                        )
                                        val currentDate = LocalDate.now()
                                        if (currentDate > expireDateTime) {
                                            dialog!!.cancel()
                                            MaterialDialog(this@ReaderActivity).show {
                                                title(text = "คำเตือน")
                                                message(text = "บัตรหมดอายุ")
                                                negativeButton(text = "ยกเลิก")
                                                positiveButton(text = "ดำเนินการต่อ", click = {
                                                    if (AppSettings.IS_OFFLINE) {
                                                        addInJSONArray(nativeCardInfo!!)
                                                        val data = getDataFromSharedPreferences()
                                                        data?.let { println(" data ref: ${it.size}") }
                                                    } else {
                                                        val requestBody: RequestBody =
                                                            builder.build()
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
                                        countdata = 1
                                    }
                                }
                                onSuccess()
                                countdata++
                            }
                        }
                        countRead++
                    } else {
//                        Log.d("responseData",getErrorString(errorCode))
                        dialog!!.cancel()
                    }
                }
            })


        /* Wait for escape command response. */
        mBluetoothReader?.setOnEscapeResponseAvailableListener(
            BluetoothReader.OnEscapeResponseAvailableListener { bluetoothReader, response, errorCode ->
                runOnUiThread {
                    //                                mTxtEscapeResponse.setText(getResponseString(response, errorCode));
                }
            })

        /* Wait for device info available. */
        mBluetoothReader?.setOnDeviceInfoAvailableListener(
            BluetoothReader.OnDeviceInfoAvailableListener { bluetoothReader, infoId, o, status ->
                runOnUiThread(Runnable {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        Toast.makeText(
                            this@ReaderActivity,
                            "Failed to read device info!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Runnable
                    }
                    when (infoId) {
                        BluetoothReader.DEVICE_INFO_SYSTEM_ID -> {
                        }

                        BluetoothReader.DEVICE_INFO_MODEL_NUMBER_STRING -> {
                        }

                        BluetoothReader.DEVICE_INFO_SERIAL_NUMBER_STRING -> {
                        }

                        BluetoothReader.DEVICE_INFO_FIRMWARE_REVISION_STRING -> {
                        }

                        BluetoothReader.DEVICE_INFO_HARDWARE_REVISION_STRING -> {
                        }

                        BluetoothReader.DEVICE_INFO_MANUFACTURER_NAME_STRING -> {
                        }

                        else -> {
                        }
                    }
                })
            })

        /* Wait for battery level available. */if (mBluetoothReader is Acr1255uj1Reader) {
            (mBluetoothReader as Acr1255uj1Reader)
                .setOnBatteryLevelAvailableListener { bluetoothReader, batteryLevel, status ->
                    Log.i(
                        TAG, "mBatteryLevelListener data: "
                                + batteryLevel
                    )
                    runOnUiThread {
                        //                                    mTxtBatteryLevel2.setText(getBatteryLevelString(batteryLevel));
                    }
                }
        }

        /* Handle on battery status available. */if (mBluetoothReader is Acr3901us1Reader) {
            (mBluetoothReader as Acr3901us1Reader)
                .setOnBatteryStatusAvailableListener { bluetoothReader, batteryStatus, status ->
                    runOnUiThread {
                        //                                    mTxtBatteryStatus2.setText(getBatteryStatusString(batteryStatus));
                    }
                }
        }

        /* Handle on slot status available. */
        mBluetoothReader?.setOnCardStatusAvailableListener(
            BluetoothReader.OnCardStatusAvailableListener { bluetoothReader, cardStatus, errorCode ->
                runOnUiThread {
                    if (errorCode != BluetoothReader.ERROR_SUCCESS) {
                        //                                    mTxtSlotStatus.setText(getErrorString(errorCode));
                        Log.d(TAG, errorCode.toString())
                        Log.d(TAG, getErrorString(errorCode).toString())
                        Toast.makeText(
                            this@ReaderActivity,
                            getErrorString(errorCode),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        //                                    mTxtSlotStatus.setText(getCardStatusString(cardStatus));
                        Log.d(TAG, errorCode.toString())
                        val status = getCardStatusString(cardStatus)
                        Log.d(TAG, status.toString())
                        if (status == "The card status is unknown.") {
                            readCardErrorDialog()
                        }

                    }
                }
            })
        mBluetoothReader?.setOnEnableNotificationCompleteListener(
            BluetoothReader.OnEnableNotificationCompleteListener { bluetoothReader, result ->
                runOnUiThread {
                    if (result != BluetoothGatt.GATT_SUCCESS) {
                        /* Fail */
                        Toast.makeText(
                            this@ReaderActivity,
                            "The device is unable to set notification!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {

                        sharedPreferences?.edit()
                            ?.putString(EXTRAS_DEVICE_NAME, mDeviceName)
                            ?.apply()
                        sharedPreferences?.edit()
                            ?.putString(EXTRAS_DEVICE_ADDRESS, mDeviceAddress)
                            ?.apply()

                        Toast.makeText(
                            this@ReaderActivity,
                            "The device is ready to use!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
    }

    private fun getDataFromSharedPreferences(): List<NativeCardInfo?>? {
        val gson = Gson()
        var productFromShared: List<NativeCardInfo?>? = ArrayList()
        val sharedPref = applicationContext.getSharedPreferences(
            "SharedPrefs${AppSettings.ID_USER_NAME}",
            MODE_PRIVATE
        )
        val jsonPreferences = sharedPref.getString("NativeCardInfo", "")
        val type = object : TypeToken<List<NativeCardInfo?>?>() {}.type
        productFromShared = gson.fromJson<List<NativeCardInfo?>>(jsonPreferences, type)
        return productFromShared
    }

    private fun addInJSONArray(productToAdd: NativeCardInfo) {
        val dialog = retrofitDialog(this)
        dialog?.show()
        val gson = Gson()
        val sharedPref = applicationContext.getSharedPreferences(
            "SharedPrefs${AppSettings.ID_USER_NAME}",
            MODE_PRIVATE
        )
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

    /** //////////////////////  MBR20  ///////////////////////////// */

    override fun onPICCActivate(status: Boolean, cardSN: String) {
        // TODO Auto-generated method stub
    }

    override fun onPICCDeactivate(status: Boolean) {
        // TODO Auto-generated method stub
    }

    override fun onPICCRate(status: Boolean, ats: String) {
        // TODO Auto-generated method stub
    }

    override fun onPICCAccess(status: Boolean, rAPUD: String) {
        // TODO Auto-generated method stub
    }

    override fun onMifareAuth(status: Boolean) {
        // TODO Auto-generated method stub
    }

    override fun onMifareReadBlock(status: Boolean, data: String) {
        // TODO Auto-generated method stub
    }

    override fun onMifareWriteBlock(status: Boolean) {
        // TODO Auto-generated method stub
    }

    override fun onMifareIncrement(status: Boolean) {
        // TODO Auto-generated method stub
    }

    override fun onMifareDecrement(status: Boolean) {
        // TODO Auto-generated method stub
    }

    override fun onSetUseVersion(status: Boolean) {
        // TODO Auto-generated method stub
    }

    override fun onSetBluetoothDeviceName(status: Boolean) {
        // TODO Auto-generated method stub
    }

    override fun onGetReaderSN(status: Boolean, sn: String) {
        // TODO Auto-generated method stub
    }

    override fun onSetReaderSN(status: Boolean) {
        // TODO Auto-generated method stub
    }

    override fun onDetectBattery(status: Boolean, energy: String) {
        // TODO Auto-generated method stub
    }

    override fun onSetSleepTimer(status: Boolean) {
        // TODO Auto-generated method stub
    }

    override fun onGetCardInfo(
        status: Boolean,
        info: String,
        pan: String,
        cardholderName: String,
        expDate: String,
    ) {
        // TODO Auto-generated method stub
    }

    override fun onSetICCPort(status: Boolean) {
        // TODO 自動產生的方法 Stub
        Log.d(TAG, "onSetICCPort: $status")
        if (status) {
            Log.d(TAG, "onSetICCPort: Success")
//            PutMessage(">>	" + "Success");
        } else {
            Log.d(TAG, "onSetICCPort: Fail")
        }
    }

    override fun onSelectMemoryCardType(status: Boolean) {
        // TODO 自動產生的方法 Stub
    }

    override fun onMemoryCardPowerOn(status: Boolean, atr: String) {
        // TODO 自動產生的方法 Stub
    }

    override fun onMemoryCardGetType(status: Boolean, typeCode: String, type: String) {
        // TODO 自動產生的方法 Stub
    }

    override fun onMemoryCardReadData(status: Boolean, rAPDU: String) {
        // TODO 自動產生的方法 Stub
    }

    override fun onMemoryCardWriteData(status: Boolean) {
        // TODO 自動產生的方法 Stub
    }

    override fun onMemoryCardPowerOff(status: Boolean) {
        // TODO 自動產生的方法 Stub
    }

    override fun onMemoryCardReadErrorCounter(status: Boolean, errorCounter: Int) {
        // TODO 自動產生的方法 Stub
    }

    override fun onMemoryCardVerifyPSC(status: Boolean) {
        // TODO 自動產生的方法 Stub
    }

    override fun onMemoryCardGetPSC(status: Boolean, psc: String) {
        // TODO 自動產生的方法 Stub
    }

    override fun onMemoryCardModifyPSC(status: Boolean) {
        // TODO 自動產生的方法 Stub
    }

    override fun onMemoryCardReadDataWithProtectBit(status: Boolean, data: String) {
        // TODO 自動產生的方法 Stub
    }

    override fun onMemoryCardWriteDataWithProtectBit(status: Boolean) {
        // TODO 自動產生的方法 Stub
    }

    override fun onMemoryCardReadProtectionData(status: Boolean, data: String) {
        // TODO 自動產生的方法 Stub
    }

    override fun onMemoryCardWriteProtectionData(status: Boolean) {
        // TODO 自動產生的方法 Stub
    }

    override fun onGiveUpAction(status: Boolean) {
        // TODO 自動產生的方法 Stub
        if (status) {
//            PutMessage(">>	" + "Cancel Success");
        }
    }

    override fun onBUZZERTest(status: Boolean) {
        // TODO 自動產生的方法 Stub
    }

    override fun onClick(view: View) {
        if (view == read_btn) {
            val deviceName = mDeviceName!!.substring(0, 3)
            if (deviceName == "ACR") {
                /* Retrieve master key from edit box. */
                dialog = PublicFunction().retrofitDialog(this)
                if (!dialog!!.isShowing) {
                    runOnUiThread {
                        dialog!!.show()
                    }
                }
                val masterKey: ByteArray?
                masterKey =
                    toByteArray(DEFAULT_3901_MASTER_KEY)
                if (masterKey != null && masterKey.size > 0) {
                    /* Clear response field for the result of authentication. */
                    //                    mTxtAuthentication.setText(R.string.noData);

                    /* Start authentication. */
                    if (!mBluetoothReader!!.authenticate(masterKey)) {
                        //                        mTxtAuthentication.setText(R.string.card_reader_not_ready);
                    } else {
                        //                        mTxtAuthentication.setText("Authenticating...");
                    }
                } else {
                    //                    mTxtAuthentication.setText("Character format error!");
                }
            } else {
                dialog = PublicFunction().retrofitDialog(this)
                if (!dialog!!.isShowing) {
                    runOnUiThread {
                        dialog!!.show()
                    }
                }

                cardpicture!!.setImageResource(R.drawable.icons8_contacts_108px_1)

                nativeCardInfo = NativeCardInfo()
                nativeCardInfo!!.address = CardAddress()
                sharedViewModel.setNativeCardInfo(nativeCardInfo!!)
                sendICCStatus()
            }
        }
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

    fun errorTokenDialog(dialog: SweetAlertDialog, message: String?): SweetAlertDialog {
        dialog.setTitleText(message)
            .setConfirmText("ตกลง")
            .setConfirmButtonBackgroundColor(
                ContextCompat.getColor(
                    dialog.context,
                    R.color.Red_Crimson
                )
            )
            .setConfirmClickListener { sweetAlertDialog -> // เมื่อผู้ใช้คลิกปุ่ม "Yes, delete it!"
                val intent: Intent =
                    Intent(this@ReaderActivity, LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            .changeAlertType(SweetAlertDialog.ERROR_TYPE)
        return dialog
    }

    fun readCardErrorDialog() {
        disconnectReader()
        MaterialDialog(this@ReaderActivity).show {
            title(R.string.some_error)
            val message =
                "กรุณาถอดบัตรออก หลังจากนั้นกดตกลง แล้วลองใหม่อีกครั้ง"
            message(text = message)
            positiveButton(R.string.ok) {

                connectReader()
            }
        }
    }

    /* Get the Bonding status string. */
    private fun getBondingStatusString(bondingStatus: Int): String? {
        if (bondingStatus == BluetoothDevice.BOND_BONDED) {
            return "BOND BONDED"
        } else if (bondingStatus == BluetoothDevice.BOND_NONE) {
            return "BOND NONE"
        } else if (bondingStatus == BluetoothDevice.BOND_BONDING) {
            return "BOND BONDING"
        }
        return "BOND UNKNOWN."
    }

    private fun getErrorString(errorCode: Int): String? {
        if (errorCode == BluetoothReader.ERROR_SUCCESS) {
            return ""
        } else if (errorCode == BluetoothReader.ERROR_INVALID_CHECKSUM) {
            return "The checksum is invalid."
        } else if (errorCode == BluetoothReader.ERROR_INVALID_DATA_LENGTH) {
            return "The data length is invalid."
        } else if (errorCode == BluetoothReader.ERROR_INVALID_COMMAND) {
            return "The command is invalid."
        } else if (errorCode == BluetoothReader.ERROR_UNKNOWN_COMMAND_ID) {
            return "The command ID is unknown."
        } else if (errorCode == BluetoothReader.ERROR_CARD_OPERATION) {
            return "The card operation failed."
        } else if (errorCode == BluetoothReader.ERROR_AUTHENTICATION_REQUIRED) {
            return "Authentication is required."
        } else if (errorCode == BluetoothReader.ERROR_LOW_BATTERY) {
            return "The battery is low."
        } else if (errorCode == BluetoothReader.ERROR_CHARACTERISTIC_NOT_FOUND) {
            return "Error characteristic is not found."
        } else if (errorCode == BluetoothReader.ERROR_WRITE_DATA) {
            return "Write command to reader is failed."
        } else if (errorCode == BluetoothReader.ERROR_TIMEOUT) {
            return "Timeout."
        } else if (errorCode == BluetoothReader.ERROR_AUTHENTICATION_FAILED) {
            return "Authentication is failed."
        } else if (errorCode == BluetoothReader.ERROR_UNDEFINED) {
            return "Undefined error."
        } else if (errorCode == BluetoothReader.ERROR_INVALID_DATA) {
            return "Received data error."
        } else if (errorCode == BluetoothReader.ERROR_COMMAND_FAILED) {
            return "The command failed."
        }
        return "Unknown error."
    }

    private fun readCardInfo() {
        cardpicture!!.setImageResource(R.drawable.icons8_contacts_108px_1)

        nativeCardInfo = NativeCardInfo()
        nativeCardInfo!!.address = CardAddress()
        sharedViewModel.setNativeCardInfo(nativeCardInfo!!)
        countRead = 1
        var apduCommand: ByteArray?
        //CHIP ID
        var apdu = "80ca9f7f00"
        apduCommand = toByteArray(apdu)
        mBluetoothReader!!.transmitApdu(
            apduCommand
        )
        apdu = "80ca9f7f2d"
        apduCommand = toByteArray(apdu)
        mBluetoothReader!!.transmitApdu(
            apduCommand
        )

        //LASER ID
        apdu = "00a4040008a000000084060002"
        apduCommand = toByteArray(apdu)
        mBluetoothReader!!.transmitApdu(
            apduCommand
        )
        apdu = "8000000017"
        apduCommand = toByteArray(apdu)
        mBluetoothReader!!.transmitApdu(
            apduCommand
        )
        apduCommand =
            toByteArray(DEFAULT_3901_APDU_COMMAND)
        mBluetoothReader!!.transmitApdu(apduCommand)
        if (apduCommand != null && apduCommand.size > 0) {

            if (!mBluetoothReader!!.transmitApdu(
                    apduCommand
                )
            ) {
                //                                                    mTxtResponseApdu.setText(R.string.card_reader_not_ready);
            } else {
                if (mBluetoothReader!!.cardStatus) {
                    //CID
                    var apdu = "80B0000402000D"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    apdu = "00C000000D"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )

                    //FULL NANE TH
                    apdu = "80B00011020064"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    apdu = "00C0000064"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )

                    //FULL NANE EN
                    apdu = "80B00075020064"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    apdu = "00C0000064"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )

                    //GENDER
                    apdu = "80B000E1020001"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    apdu = "00C0000001"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )

                    //DOB
                    apdu = "80B000D9020008"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    apdu = "00C0000008"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )

                    //Address
                    apdu = "80B01579020064"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    apdu = "00C0000064"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    //RequestNum
                    apdu = "80B000E2020014"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    apdu = "00C0000014"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    //Issue place
                    apdu = "80B000F6020064"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    apdu = "00C0000064"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    //Issue code
                    apdu = "80B0015A02000D"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    apdu = "00C000000D"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    //Issue expire
                    apdu = "80B00167020012"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    apdu = "00C0000012"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    //Card type
                    apdu = "80B00177020002"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    apdu = "00C0000002"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    //version
                    apdu = "80B00000020004"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    apdu = "00C0000004"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    //version
                    apdu = "80b0161902000e"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    apdu = "00c000000e"
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )

                    //Picture
                    apdu = DEFAULT_3901_APDU_COMMAND
                    apduCommand = toByteArray(apdu)
                    mBluetoothReader!!.transmitApdu(
                        apduCommand
                    )
                    for (i in codephoto.indices) {
                        apdu = codephoto[i]
                        apduCommand =
                            toByteArray(apdu)
                        mBluetoothReader!!.transmitApdu(
                            apduCommand
                        )
                        apdu = "00C00000FF"
                        apduCommand =
                            toByteArray(apdu)
                        mBluetoothReader!!.transmitApdu(
                            apduCommand
                        )
                    }
                } else {
                    Log.d(
                        "responseData",
                        "card no input"
                    )
                }
            }
        } else {
            //                                                mTxtResponseApdu.setText("Character format error!");
        }
    }


    private fun getCardStatusString(cardStatus: Int): String? {
        if (cardStatus == BluetoothReader.CARD_STATUS_ABSENT) {
            read_btn!!.text = "Read"
            read_btn!!.isEnabled = true
            progressBar!!.visibility = View.GONE
            return "Absent."
        } else if (cardStatus == BluetoothReader.CARD_STATUS_PRESENT) {
            return "Present."
        } else if (cardStatus == BluetoothReader.CARD_STATUS_POWERED) {
            return "Powered."
        } else if (cardStatus == BluetoothReader.CARD_STATUS_POWER_SAVING_MODE) {
            return "Power saving mode."
        }
        return "The card status is unknown."
    }

    companion object {
        private const val REQUEST_ACCESS_COARSE_LOCATION = 2
        const val TAG = "ReaderLOG"
        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
        const val EXTRAS_TYPE_READER = "TYPE_READERS"
        private const val CHIP_ID_APDU_COMMAND = "80ca9f7f00"
        private const val DEFAULT_3901_MASTER_KEY =
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF"
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
        private const val CHIP_ID_APDU_LE = "80ca9f7f2d"
        private const val LASER_ID_APDU_COMMAND = "00a4040008a000000084060002"
        private const val LASER_ID_APDU_LE = "8000000017"
        private const val DEFAULT_3901_APDU_COMMAND = "00A4040008A000000054480001"
        private const val IDCARD_APDU_COMMAND = "80B0000402000D"
        private const val IDCARD_APDU_LE = "00C000000D"
        private const val NAME_TH_APDU_COMMAND = "80B00011020064"
        private const val NAME_TH_APDU_LE = "00C0000064"
        private const val NAME_EN_APDU_COMMAND = "80B00075020064"
        private const val NAME_EN_APDU_LE = "00C0000064"
        private const val GENDER_APDU_COMMAND = "80B000E1020001"
        private const val GENDER_APDU_LE = "00C0000001"
        private const val DOB_APDU_COMMAND = "80B000D9020008"
        private const val DOB_APDU_LE = "00C0000008"
        private const val Address_APDU_COMMAND = "80B01579020064"
        private const val Address_APDU_LE = "00C0000064"
        private const val RequestNum_APDU_COMMAND = "80B000E2020014"
        private const val RequestNum_APDU_LE = "00C0000014"
        private const val Issue_place_APDU_COMMAND = "80B000F6020064"
        private const val Issue_place_APDU_LE = "00C0000064"
        private const val Issue_code_APDU_COMMAND = "80B0015A02000D"
        private const val Issue_code_APDU_LE = "00C000000D"
        private const val ISSUE_EXPIRE_APDU_COMMAND = "80B00167020012"
        private const val ISSUE_EXPIRE_APDU_LE = "00C0000012"
        private const val Card_Type_APDU_COMMAND = "80B00177020002"
        private const val Card_Type_APDU_LE = "00C0000002"
        private const val Version_APDU_COMMAND = "80B00000020004"
        private const val Version_APDU_LE = "00C0000004"
        private const val Image_code_APDU_COMMAND = "80b0161902000e"
        private const val Image_code_APDU_LE = "00c000000e"
        private const val Code_APDU_LE = "00C00000FF"
        private val codephoto = arrayOf(
            "80B0017B0200FF",
            "80B0027A0200FF",
            "80B003790200FF",
            "80B004780200FF",
            "80B005770200FF",
            "80B006760200FF",
            "80B007750200FF",
            "80B008740200FF",
            "80B009730200FF",
            "80B00A720200FF",
            "80B00B710200FF",
            "80B00C700200FF",
            "80B00D6F0200FF",
            "80B00E6E0200FF",
            "80B00F6D0200FF",
            "80B0106C0200FF",
            "80B0116B0200FF",
            "80B0126A0200FF",
            "80B013690200FF",
            "80B014680200FF"
        )
        const val STATE_DISCONNECTED = 0
        const val STATE_DISCONNECTING = 3
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
        const val STATUS_NOT_INSERT = "Card not insert"
        const val STATUS_INSERTED = "Card inserted"
        fun hexStringToByteArray(hex: String): ByteArray {
            val l = hex.length
            val data = ByteArray(l / 2)
            var i = 0
            while (i < l) {
                data[i / 2] = ((Character.digit(hex[i], 16) shl 4)
                        + Character.digit(hex[i + 1], 16)).toByte()
                i += 2
            }
            return data
        }

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

        private fun toByteArray(hexString: String): ByteArray? {
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
    }

    private fun checkForInternet(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
}