/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright (C) 2014-2018 Advanced Card Systems Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wac.wacthaiidcloud

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import java.util.*

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
class DeviceScanActivity : com.wac.wacthaiidcloud.utils.ListActivity() {
    private var mLeDeviceListAdapter: LeDeviceListAdapter? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mScanning = false
    private var mHandler: Handler? = null
    private var TAG: String? = "DeviceScanLOG"
    private var sharedPreferences: SharedPreferences? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(getString(R.string.PrefsSetting), 0)
        supportActionBar!!.setTitle(R.string.title_devices)
        mHandler = Handler()
        /*
         * Use this check to determine whether BLE is supported on the device.
         * Then you can selectively disable BLE-related features.
         */
        if (!packageManager.hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE
            )
        ) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT)
                .show()
        }

        /*
         * Initializes a Bluetooth adapter. For API level 18 and above, get a
         * reference to BluetoothAdapter through BluetoothManager.
         */
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        /* Checks if Bluetooth is supported on the device. */
        if (mBluetoothAdapter == null) {
            Toast.makeText(
                this, R.string.error_bluetooth_not_supported,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).isVisible = false
            menu.findItem(R.id.menu_scan).isVisible = true
            menu.findItem(R.id.menu_refresh).actionView = null
        } else {
            menu.findItem(R.id.menu_stop).isVisible = true
            menu.findItem(R.id.menu_scan).isVisible = false
            menu.findItem(R.id.menu_refresh).setActionView(
                R.layout.actionbar_indeterminate_progress
            )
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scan -> {
                mLeDeviceListAdapter!!.clear()
                scanLeDevice(true)
            }
            R.id.menu_stop -> scanLeDevice(false)
            R.id.menu_usb -> {
                scanLeDevice(false)
                val intent = Intent(this, UsbReaderActivity::class.java)
//                intent.putExtra(ReaderActivity.EXTRAS_TYPE_READER, true)
                startActivity(intent)
            }
            R.id.menu_about -> {
                val manager = this@DeviceScanActivity.packageManager
                val info = manager.getPackageInfo(
                    this@DeviceScanActivity.packageName,
                    PackageManager.GET_ACTIVITIES
                )

                MaterialDialog(this@DeviceScanActivity).show {
                    title(text = "About ${getString(R.string.app_name)}")
                    val message = "Version : " + info.versionName
                    message(text = message)
                    positiveButton(R.string.ok)
                }
            }
            R.id.menu_logout -> {
                val intent: Intent =
                    Intent(this@DeviceScanActivity, LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        return true
    }


    private fun requestPermissionBluetooth() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.BLUETOOTH,
                        ),
                        REQUEST_ACCESS_COARSE_LOCATION
                    )
                } else {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.BLUETOOTH,
                        ),
                        REQUEST_ACCESS_COARSE_LOCATION
                    )
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    ),
                    BLUETOOTH_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!mBluetoothAdapter!!.isEnabled) {
            requestPermissionBluetooth()
            val enableBtIntent = Intent(
                BluetoothAdapter.ACTION_REQUEST_ENABLE
            )
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            } else {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        } else {
            /* Request access coarse location permission. */
            requestPermissionBluetooth()
        }

        /* Initializes list view adapter. */
        mLeDeviceListAdapter = LeDeviceListAdapter()
        listAdapter = mLeDeviceListAdapter
        scanLeDevice(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        /* User chose not to enable Bluetooth. */
        if (requestCode == REQUEST_ENABLE_BT
            && resultCode == RESULT_CANCELED
        ) {
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        super.onPause()
        scanLeDevice(false)
        mLeDeviceListAdapter?.clear()
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val device = this.mLeDeviceListAdapter!!.getDevice(position) ?: return
        scanLeDevice(false)
        val intent = Intent(this, ReaderActivity::class.java)
        intent.putExtra(ReaderActivity.EXTRAS_DEVICE_NAME, device.name)
        intent.putExtra(ReaderActivity.EXTRAS_DEVICE_ADDRESS, device.address)
        startActivity(intent)
    }

    @Synchronized
    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            /* Stops scanning after a pre-defined scan period. */
            mHandler!!.postDelayed({
                if (mScanning) {
                    mScanning = false
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_CODE)
                        } else {
                            ActivityCompat. requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_ACCESS_COARSE_LOCATION)
                        }

                    }
                    mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
                }
                invalidateOptionsMenu()
            }, SCAN_PERIOD)
            mScanning = true
            mBluetoothAdapter!!.startLeScan(mLeScanCallback)
            invalidateOptionsMenu()
        } else if (mScanning) {
            mScanning = false
            mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
            invalidateOptionsMenu()
        }
    }

    /* Adapter for holding devices found through scanning. */
    private inner class LeDeviceListAdapter : BaseAdapter() {
        private val mLeDevices: ArrayList<BluetoothDevice> = ArrayList()
        private val mInflator: LayoutInflater = this@DeviceScanActivity.layoutInflater
        fun addDevice(device: BluetoothDevice) {
            Log.d(TAG, "addDevice: ${device.name}")
            if (!mLeDevices.contains(device) && device.name != null && device.name.length > 0) {
                mLeDevices.add(device)
            }
            if (!mLeDevices.contains(device) && device.name != null) {
                mLeDevices.add(device)
            }
        }

        fun getDevice(position: Int): BluetoothDevice {
            return mLeDevices[position]
        }

        fun clear() {
            mLeDevices.clear()
        }

        override fun getCount(): Int {
            return mLeDevices.size
        }

        override fun getItem(i: Int): Any {
            return mLeDevices[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup?): View? {
            var view = view
            val viewHolder: ViewHolder
            /* General ListView optimization code. */if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null)
                viewHolder = ViewHolder()
                viewHolder.deviceAddress = view
                    .findViewById<View>(R.id.device_address) as TextView
                viewHolder.deviceName = view
                    .findViewById<View>(R.id.device_name) as TextView
                view.tag = viewHolder
            } else {
                viewHolder = view.tag as ViewHolder
            }
            val device = mLeDevices[i]
            val deviceName = device.name
            if (deviceName != null && deviceName.length > 0) {
                viewHolder.deviceName!!.text = deviceName
                Log.d(TAG, "getView: $deviceName")
            } else viewHolder.deviceName!!.setText(R.string.unknown_device)
            viewHolder.deviceAddress!!.text = device.address
            return view
        }

    }

    /* Device scan callback. */
    private val mLeScanCallback = LeScanCallback { device, rssi, scanRecord ->
        runOnUiThread {
            mLeDeviceListAdapter!!.addDevice(device)
            mLeDeviceListAdapter!!.notifyDataSetChanged()
        }
    }

    internal class ViewHolder {
        var deviceName: TextView? = null
        var deviceAddress: TextView? = null
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_ACCESS_COARSE_LOCATION = 2
        private const val REQUEST_ACCESS_BT = 3
        private const val REQUEST_BLUETOOTH_CODE = 10
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 9999
        /* Stops scanning after 10 seconds. */
        private const val SCAN_PERIOD: Long = 10000
    }
}