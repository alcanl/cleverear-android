package com.alcanl.cleverear

import android.Manifest
import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityBeaconBinding
import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser
import com.neovisionaries.bluetooth.ble.advertising.EddystoneTLM
import com.neovisionaries.bluetooth.ble.advertising.EddystoneUID
import com.neovisionaries.bluetooth.ble.advertising.EddystoneURL
import com.neovisionaries.bluetooth.ble.advertising.IBeacon
import dagger.hilt.android.AndroidEntryPoint
import java.util.Arrays


@AndroidEntryPoint
class BeaconActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityBeaconBinding

    private lateinit var mBluetoothManager: BluetoothManager

    private lateinit var mBluetoothAdapter: BluetoothAdapter

    private lateinit var mBluetoothLeScanner: BluetoothLeScanner

    private val mLocationFinePermission by lazy {
        ContextCompat.checkSelfPermission(
            this,
            ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val mLocationBackgroundPermission by lazy {

        if (Build.VERSION.SDK_INT >= 29)
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        else true
    }

    private val test : ScanCallback by lazy {
        object : ScanCallback() {

            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                Log.d(BeaconActivity::class.simpleName, result.device.address)

                if (result.scanRecord != null) {
                    //Log.d(BeaconActivity::class.simpleName, result.rssi.toString())
                    //result.scanRecord!!.bytes.forEach { i -> Log.d("Advertise Data", i.toString()) }
                    //parseScanDataWithNewLibTest(result.scanRecord!!.bytes)
                    //detectEddystoneTLM(result.scanRecord?.bytes)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        checkLocationPermission()
        discoverEddyStoneBeacons()
    }

    private fun checkLocationPermission()
    {
        if (!mLocationFinePermission)
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), 60)

        if (!mLocationBackgroundPermission && Build.VERSION.SDK_INT >= 29)
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_BACKGROUND_LOCATION), 61)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 60)
            if (grantResults.isNotEmpty())
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && mLocationFinePermission)
                    if (!mLocationBackgroundPermission && Build.VERSION.SDK_INT >= 29)
                        ActivityCompat.requestPermissions(this, arrayOf(ACCESS_BACKGROUND_LOCATION), 62)
                else
                    Toast.makeText(this, getString(R.string.toast_no_permission_splash_activity), Toast.LENGTH_LONG).show()

        if (requestCode == 61)
            if (grantResults.isNotEmpty())
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && mLocationBackgroundPermission)
                  if (!mLocationFinePermission)
                      ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), 63)

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .setReportDelay(0L)
            .build()
        mBluetoothLeScanner.startScan(emptyList(), settings, test)
        //Handler(Looper.myLooper()!!).postDelayed({ mBluetoothLeScanner.stopScan(test) }, 20000L)
    }

    private fun detectEddystoneTLM(scanData: ByteArray?) {
        if (scanData != null && scanData.isNotEmpty()) {
            //scanData.forEach { i -> Log.d("Scan Data", i.toHexString()) }
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

private fun parseScanDataWithNewLibTest(scanRecord: ByteArray)
{
    val structures = ADPayloadParser.getInstance().parse(scanRecord)

    for (structure in structures) {
        if (structure is EddystoneUID) {
            // Eddystone UID
            val es = structure
            Log.d(BeaconActivity::class.simpleName, "EddystoneUID")
            // (1) Calibrated Tx power at 0 m.
            val power = es.txPower
            Log.d(BeaconActivity::class.simpleName, power.toString())

            // (2) 10-byte Namespace ID
            val namespaceId = es.namespaceId
            Log.d(BeaconActivity::class.simpleName, namespaceId.toString())
            val namespaceIdAsString = es.namespaceIdAsString
            Log.d(BeaconActivity::class.simpleName, namespaceIdAsString)

            // (3) 6-byte Instance ID
            val instanceId = es.instanceId
            Log.d(BeaconActivity::class.simpleName, instanceId.toString())
            val instanceIdAsString = es.instanceIdAsString
            Log.d(BeaconActivity::class.simpleName, instanceIdAsString)

            // (4) 16-byte Beacon ID
            val beaconId = es.beaconId
            Log.d(BeaconActivity::class.simpleName, beaconId.toString())
            val beaconIdAsString = es.beaconIdAsString
            Log.d(BeaconActivity::class.simpleName, beaconIdAsString)
        } else if (structure is EddystoneURL) {
            // Eddystone URL
            Log.d(BeaconActivity::class.simpleName, "EddystoneUID")
            val es = structure

            // (1) Calibrated Tx power at 0 m.
            val power = es.txPower
            Log.d(BeaconActivity::class.simpleName, power.toString())
            // (2) URL
            val url = es.url
            Log.d(BeaconActivity::class.simpleName, url.toString())
        } else if (structure is EddystoneTLM) {
            // Eddystone TLM
            Log.d(BeaconActivity::class.simpleName, "EddystoneUID")
            val es = structure
            // (1) TLM Version
            val version = es.tlmVersion
            Log.d(BeaconActivity::class.simpleName, version.toString())

            // (2) Battery Voltage
            val voltage = es.batteryVoltage
            Log.d(BeaconActivity::class.simpleName, voltage.toString())

            // (3) Beacon Temperature
            val temperature = es.beaconTemperature
            Log.d(BeaconActivity::class.simpleName, temperature.toString())

            // (4) Advertisement count since power-on or reboot.
            val count = es.advertisementCount
            Log.d(BeaconActivity::class.simpleName, count.toString())

            // (5) Elapsed time in milliseconds since power-on or reboot.
            val elapsed = es.elapsedTime
            Log.d(BeaconActivity::class.simpleName, elapsed.toString())
        } else if (structure is IBeacon) {
            // iBeacon
            val iBeacon = structure

            // (1) Proximity UUID
            val uuid = iBeacon.uuid
            Log.d("UUID", uuid.toString())
            // (2) Major number
            val major = iBeacon.major
            Log.d("Major", major.toString())

            // (3) Minor number
            val minor = iBeacon.minor
            Log.d("Minor", minor.toString())

            // (4) Tx Power
            val power = iBeacon.power
            Log.d("Power", power.toString())
        }
    }
}