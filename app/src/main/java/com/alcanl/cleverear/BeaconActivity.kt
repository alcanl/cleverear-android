package com.alcanl.cleverear

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityBeaconBinding
import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser
import com.neovisionaries.bluetooth.ble.advertising.EddystoneTLM
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class BeaconActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityBeaconBinding

    private lateinit var mBluetoothLeScanner: BluetoothLeScanner

    private val mLocationFinePermission by lazy {
        ContextCompat.checkSelfPermission(
            this,
            ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val mLocationCoarsePermission by lazy {
        if (Build.VERSION.SDK_INT >= 29)
            ContextCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        else true
    }

    private val mScanCallback: ScanCallback by lazy {
        object : ScanCallback() {

            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult) {

                if (result.scanRecord != null)
                    determineEddystoneBeacons(result.scanRecord!!.bytes, result.device.address)

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        if (!mLocationCoarsePermission || !mLocationFinePermission)
            requestLocationPermission()
        else
            discoverEddyStoneBeacons()
    }

    private fun requestLocationPermission()
    {
        if (Build.VERSION.SDK_INT >= 29)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                60
            )
        else
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), 61)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        if (requestCode == 60 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            && grantResults[1] == PackageManager.PERMISSION_GRANTED)
            discoverEddyStoneBeacons()

        else if (requestCode == 61 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            discoverEddyStoneBeacons()

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun initialize()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_beacon)
        mBluetoothLeScanner = getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner
    }

    @SuppressLint("MissingPermission")
    private fun discoverEddyStoneBeacons()
    {
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .setReportDelay(3000L)
            .build()

        mBluetoothLeScanner.startScan(emptyList(), settings, mScanCallback)
        Handler(Looper.myLooper()!!).postDelayed({ mBluetoothLeScanner.stopScan(mScanCallback) }, 20000L)
    }

    private fun determineEddystoneBeacons(scanRecord: ByteArray, address: String)
    {
        val structures = ADPayloadParser.getInstance().parse(scanRecord)

        structures.forEach {

            if (it is EddystoneTLM) {
                Log.d(BeaconActivity::class.simpleName, "EddystoneUIDTLM")
                Log.d(BeaconActivity::class.simpleName, address)

                val version = it.tlmVersion
                Log.d(BeaconActivity::class.simpleName, version.toString())

                val voltage = it.batteryVoltage
                Log.d(BeaconActivity::class.simpleName, voltage.toString())

                val temperature = it.beaconTemperature
                Log.d(BeaconActivity::class.simpleName, temperature.toString())

                val count = it.advertisementCount
                Log.d(BeaconActivity::class.simpleName, count.toString())

                val elapsed = it.elapsedTime
                Log.d(BeaconActivity::class.simpleName, elapsed.toString())
            }
        }
    }
}
