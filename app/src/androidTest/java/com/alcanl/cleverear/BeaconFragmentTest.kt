package com.alcanl.cleverear

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.EddystoneHearingAid
import com.alcanl.cleverear.util.EarSide
import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser
import com.neovisionaries.bluetooth.ble.advertising.EddystoneTLM
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotSame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

const val EDDYSTONE_LEFT_DEVICE_ADDRESS = "TEST1"
const val EDDYSTONE_RIGHT_DEVICE_ADDRESS = "TEST2"

@RunWith(AndroidJUnit4::class)
class BeaconFragmentTest {

    private lateinit var configuration : ConfigurationModule

    private lateinit var mBluetoothLeScanner : BluetoothLeScanner

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    private val mScanCallback: ScanCallback by lazy {
        object : ScanCallback() {

            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult) {

                if (result.scanRecord != null)
                    CoroutineScope(Dispatchers.IO).launch {
                        determineEddystoneBeacons(result.scanRecord!!.bytes, result.device.address)
                    }
            }
        }
    }

    private fun determineEddystoneBeacons(scanRecord: ByteArray, address: String)
    {
        ADPayloadParser.getInstance().parse(scanRecord).forEach {

            if (it is EddystoneTLM)
                with(configuration) {

                        when (address) {
                            EDDYSTONE_LEFT_DEVICE_ADDRESS -> eddyStoneMap[EarSide.Left]?.setEddystoneProperties(it, address)
                            EDDYSTONE_RIGHT_DEVICE_ADDRESS -> eddyStoneMap[EarSide.Right]?.setEddystoneProperties(it, address)
                            else -> {}
                        }
                }
        }
    }

    @Before
    fun setUp()
    {
        configuration = ConfigurationModule.createConfiguration()
        configuration.eddyStoneMap[EarSide.Left] = EddystoneHearingAid()
        configuration.eddyStoneMap[EarSide.Right] = EddystoneHearingAid()
        mBluetoothLeScanner = appContext.getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner
        mBluetoothLeScanner.startScan(mScanCallback)
    }

    @After
    fun tearDown()
    {
        mBluetoothLeScanner.stopScan(mScanCallback)
    }

    @Test
    fun leftDeviceDetected_ThenCheckAddress()
    {
        assertEquals(EDDYSTONE_LEFT_DEVICE_ADDRESS, configuration.eddyStoneMap[EarSide.Left]?.macAddress)
    }

    @Test
    fun rightDeviceDetected_ThenCheckAddress()
    {
        assertEquals(EDDYSTONE_RIGHT_DEVICE_ADDRESS, configuration.eddyStoneMap[EarSide.Right]?.macAddress)
    }

    @Test
    fun leftDevicePropertiesRead_ThenCheckTemperature()
    {
        assertNotSame(0.0, configuration.eddyStoneMap[EarSide.Left]?.temperature)
    }

    @Test
    fun rightDevicePropertiesRead_ThenCheckTemperature()
    {
        assertNotSame(0.0, configuration.eddyStoneMap[EarSide.Right]?.temperature)
    }
}
