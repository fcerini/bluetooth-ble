package com.example.myapplication

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.random.Random


class MainActivity : AppCompatActivity() {
    private lateinit var btAdapter: BluetoothAdapter
    private lateinit var btLeAdvertiser: BluetoothLeAdvertiser

    private val requestBT = 1234
    var testData = "000000000000009"

    private var btLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
    private var mScanning = false
    private val handler = Handler()
    private val SCAN_PERIOD: Long = 30000
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (result.scanRecord == null){
                return
            }

            val record = result.scanRecord!!
            val data = record.serviceData
            if (data.size == 0){
                return
            }

            tView.setText ( tView.text.toString() +
                    result.rssi.toString() + "db, "+
                    String(data.values.first()) +"\n")
        }
    }

    private val advertiseCallback : AdvertiseCallback = object: AdvertiseCallback(){
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            tView.setText ( "Error: " + errorCode.toString() )
        }
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            tView.setText ( tView.text.toString() +  "Advertising successfully started \n")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testData = "im:0000000000000" + Random(System.nanoTime()).nextInt(10, 90).toString()
        btstartAdvertising.setText("Advertise " + testData)

        init()

    }

    fun startAdvertising ( v:View){
        if (!init()){
            return
        }

        val settings = buildAdvertiseSettings()
        val data = buildAdvertiseData()

        tView.setText ( "startAdvertising... \n")

        btLeAdvertiser.startAdvertising(
            settings, data,
            advertiseCallback
        )

    }

    fun stopAdvertising ( v:View) {
        if (!init()){
            return
        }

        tView.setText ( "stopAdvertising! \n")
        btLeAdvertiser.stopAdvertising(advertiseCallback)

    }
        fun startScan( v:View){
            try {
                if (!init()){
                    return
                }
                if (!mScanning) { // Stops scanning after a pre-defined scan period.
                    handler.postDelayed({
                        mScanning = false
                        btLeScanner.stopScan(leScanCallback)
                        tView.setText ( tView.text.toString() +  "stopScan \n")
                    }, SCAN_PERIOD)
                    mScanning = true
                    btLeScanner.startScan(leScanCallback)
                    tView.setText ( "startScan " + SCAN_PERIOD.toString() + "sec. \n")
                } else {
                    mScanning = false
                    btLeScanner.stopScan(leScanCallback)
                    tView.setText ( "stopScan!")
                }

            } catch (e: Throwable) {
                tView.setText (e.message)
            }

    }

    fun init() :Boolean{
        try {
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                tView.setText ("Bluetooth Low Energy not supported! sorry...")
                return false
            }

            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            btAdapter = bluetoothManager.adapter

            if (btAdapter == null || !btAdapter.isEnabled) {
                tView.setText ( "bluetooth problem... \n")
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, requestBT)
                return false
            }

            if (ContextCompat.checkSelfPermission(this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION) !==
                PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                } else {
                    ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                }

                return false
            }

            btLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
            btLeAdvertiser = btAdapter.bluetoothLeAdvertiser

            return true
        } catch (e: Throwable) {
            tView.setText (e.message)
            return true
        }
    }


    private fun buildAdvertiseData(): AdvertiseData {
        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         * This includes everything put into AdvertiseData including UUIDs, device info, &
         * arbitrary service or manufacturer data.
         * Attempting to send packets over this limit will result in a failure with error code
         * AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         * onStartFailure() method of an AdvertiseCallback implementation.
         */

        val service_UUID = ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb")

        val dataBuilder = AdvertiseData.Builder()
        dataBuilder.addServiceUuid(service_UUID)
        dataBuilder.setIncludeDeviceName(false)
        dataBuilder.addServiceData(service_UUID, testData.toByteArray())

        return dataBuilder.build()
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private fun buildAdvertiseSettings(): AdvertiseSettings? {
        val settingsBuilder = AdvertiseSettings.Builder()
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        settingsBuilder.setTimeout(0)
        return settingsBuilder.build()
    }


    override fun onDestroy() {
        super.onDestroy()
    }

}