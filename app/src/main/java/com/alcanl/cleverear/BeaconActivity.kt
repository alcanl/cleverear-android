package com.alcanl.cleverear

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityBeaconBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Arrays

@AndroidEntryPoint
class BeaconActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityBeaconBinding

    private lateinit var mBluetoothManager: BluetoothManager

    private lateinit var mBluetoothAdapter: BluetoothAdapter

    private lateinit var mBluetoothLeScanner: BluetoothLeScanner

    private val test : ScanCallback by lazy {
        object : ScanCallback() {

            @SuppressLint("MissingPermission", "NewApi")
            override fun onScanResult(callbackType: Int, result: ScanResult) {

                if (result.scanRecord != null) {
                    result.scanRecord!!.bytes.forEach { i -> Log.d("Advertise Data", i.toString()) }
                    detectEddystoneTLM(result.scanRecord?.bytes)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        discoverEddyStoneBeacons()
    }

    private fun initialize()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_beacon)
        mBluetoothManager = getSystemService(BluetoothManager::class.java)
        mBluetoothAdapter = mBluetoothManager.adapter
        mBluetoothLeScanner = mBluetoothAdapter.bluetoothLeScanner
    }

    @SuppressLint("MissingPermission")
    private fun discoverEddyStoneBeacons()
    {
        mBluetoothLeScanner.startScan(test)
        Handler(Looper.myLooper()!!).postDelayed({ mBluetoothLeScanner.stopScan(test) }, 20000L)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun detectEddystoneTLM(scanData: ByteArray?) {
        if (scanData != null && scanData.isNotEmpty()) {
            scanData.forEach { i -> Log.d("Scan Data", i.toHexString()) }
            if (scanData[0] == 0x16.toByte() && scanData[1] == 0xFE.toByte() && scanData[2] == 0x20.toByte()) {
                val telemetryData = Arrays.copyOfRange(scanData, 3, scanData.size)
                parseTelemetryData(telemetryData)
            }
        }

    }

    private fun parseTelemetryData(telemetryData: ByteArray) {

        if (telemetryData.size >= 14) {

            val batteryLevel = (telemetryData[0].toInt() and 0xFF)
            val temperature = ((telemetryData[1].toInt() shl 8) or (telemetryData[2].toInt() and 0xFF)).toShort()
            val timeCounter = (telemetryData[3].toInt() shl 24) or ((telemetryData[4].toInt() and 0xFF) shl 16) or
                        ((telemetryData[5].toInt() and 0xFF) shl 8) or (telemetryData[6].toInt() and 0xFF)

            val version = telemetryData[7]

            Log.d("EddystoneTLM", "Battery Level: $batteryLevel%")
            Log.d("EddystoneTLM", "Temperature: $temperature°C")
            Log.d("EddystoneTLM", "Time Counter: $timeCounter seconds")
            Log.d("EddystoneTLM", "Version: $version")
        } else {
            Log.d("EddystoneTLM", "Invalid TLM data length")
        }
    }

}