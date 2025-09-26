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

/**
 * The constant device addresses must be updated for each device test
 * The eddystone supported hearing aid device mac addresses must be known before the tests
 */
const val EDDYSTONE_LEFT_DEVICE_ADDRESS = "TEST1"
const val EDDYSTONE_RIGHT_DEVICE_ADDRESS = "TEST2"

/**
 * <p>A test class that represents useful real time test scenarios for eddystone tlm supported hearing aid devices</p>
 *
 * @see RunWith
 * @see AndroidJUnit4
 */
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

    /**
     * <p>Determines the eddystone tlm supporter devices and parses their advertise data,
     * if it is a hearing aid device then sets the relevant properties
     *
     * @param scanRecord a byte array that contains scan result for a bluetooth low energy device
     * @param address mac address data of the recent scanned bluetooth low energy device
     */
    private fun determineEddystoneBeacons(scanRecord: ByteArray, address: String)
    {
        ADPayloadParser.getInstance().parse(scanRecord).forEach {

            if (it is EddystoneTLM)
                with(configuration) {

                        when (address) {
                            EDDYSTONE_LEFT_DEVICE_ADDRESS -> eddyStoneMap[EarSide.Left]?.setEddystoneProperties(it, address, "TEST", appContext)
                            EDDYSTONE_RIGHT_DEVICE_ADDRESS -> eddyStoneMap[EarSide.Right]?.setEddystoneProperties(it, address, "TEST", appContext)
                            else -> {}
                        }
                }
        }
    }

    /**
     * <p>Initializes the relevant properties for the tests</p>
     * <p>This method just runs before all tests and prepares the test runs</p>
     *
     * @see Before
     */
    @Before
    fun setUp()
    {
        configuration = ConfigurationModule.createConfiguration()
        configuration.eddyStoneMap[EarSide.Left] = EddystoneHearingAid()
        configuration.eddyStoneMap[EarSide.Right] = EddystoneHearingAid()
        mBluetoothLeScanner = appContext.getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner
        mBluetoothLeScanner.startScan(mScanCallback)
    }

    /**
     * <p>Runs the relevant jobs after the all tests</p>
     * <p>This method ends the test session</p>
     *
     * @see After
     */
    @After
    fun tearDown()
    {
        mBluetoothLeScanner.stopScan(mScanCallback)
    }

    /**
     * <p>Checks the eddystone supported left side hearing aid device mac address</p>
     * <p>The test passes if the device mac address is expected value, otherwise assertion exception occurs</p>
     *
     * @see Test
     */
    @Test
    fun leftDeviceDetected_ThenCheckAddress()
    {
        assertEquals(EDDYSTONE_LEFT_DEVICE_ADDRESS, configuration.eddyStoneMap[EarSide.Left]?.macAddress)
    }

    /**
     * <p>Checks the eddystone supported right side hearing aid device mac address</p>
     * <p>The test passes if the device mac address is expected value, otherwise assertion exception occurs</p>
     *
     * @see Test
     */
    @Test
    fun rightDeviceDetected_ThenCheckAddress()
    {
        assertEquals(EDDYSTONE_RIGHT_DEVICE_ADDRESS, configuration.eddyStoneMap[EarSide.Right]?.macAddress)
    }

    /**
     * <p>Checks the eddystone supported left side hearing aid device temperature value</p>
     * <p>The test passes if the device temperature value is not empty, otherwise assertion exception occurs</p>
     *
     * @see Test
     */
    @Test
    fun leftDevicePropertiesRead_ThenCheckTemperature()
    {
        assertNotSame(0.0, configuration.eddyStoneMap[EarSide.Left]?.temperature)
    }

    /**
     * <p>Checks the eddystone supported right side hearing aid device temperature value</p>
     * <p>The test passes if the device temperature value is not empty, otherwise assertion exception occurs</p>
     *
     * @see Test
     */
    @Test
    fun rightDevicePropertiesRead_ThenCheckTemperature()
    {
        assertNotSame(0.0, configuration.eddyStoneMap[EarSide.Right]?.temperature)
    }

    /**
     * <p>Checks the eddystone supported left side hearing aid device distance value</p>
     * <p>The test passes if the device distance value is not empty, otherwise assertion exception occurs</p>
     *
     * @see Test
     */
    @Test
    fun leftDevicePropertiesRead_ThenCheckDistance()
    {
        assertNotSame(0, configuration.eddyStoneMap[EarSide.Left]?.distance)
    }

    /**
     * <p>Checks the eddystone supported right side hearing aid device temperature value</p>
     * <p>The test passes if the device distance value is not empty, otherwise assertion exception occurs</p>
     *
     * @see Test
     */
    @Test
    fun rightDevicePropertiesRead_ThenCheckDistance()
    {
        assertNotSame(0, configuration.eddyStoneMap[EarSide.Right]?.distance)
    }

    /**
     * <p>Checks the eddystone supported left side hearing aid device battery voltage value</p>
     * <p>The test passes if the device battery voltage value is not empty, otherwise assertion exception occurs</p>
     *
     * @see Test
     */
    @Test
    fun leftDevicePropertiesRead_ThenCheckBatteryVoltage()
    {
        assertNotSame(0, configuration.eddyStoneMap[EarSide.Left]?.batteryVoltage)
    }

    /**
     * <p>Checks the eddystone supported right side hearing aid device battery voltage value</p>
     * <p>The test passes if the device battery voltage value is not empty, otherwise assertion exception occurs</p>
     *
     * @see Test
     */
    @Test
    fun rightDevicePropertiesRead_ThenCheckBatteryVoltage()
    {
        assertNotSame(0, configuration.eddyStoneMap[EarSide.Right]?.batteryVoltage)
    }

    /**
     * <p>Checks the eddystone supported left side hearing aid device tlm version data</p>
     * <p>The test passes if the device tlm version data is not empty, otherwise assertion exception occurs</p>
     *
     * @see Test
     */
    @Test
    fun leftDevicePropertiesRead_ThenCheckTLMVersion()
    {
        assertNotSame(0, configuration.eddyStoneMap[EarSide.Left]?.tlmVersion)
    }

    /**
     * <p>Checks the eddystone supported right side hearing aid device tlm version data</p>
     * <p>The test passes if the device tlm version data is not empty, otherwise assertion exception occurs</p>
     *
     * @see Test
     */
    @Test
    fun rightDevicePropertiesRead_ThenCheckTLMVersion()
    {
        assertNotSame(0, configuration.eddyStoneMap[EarSide.Right]?.tlmVersion)
    }

    /**
     * <p>Checks the eddystone supported left side hearing aid device step count value</p>
     * <p>The test passes if the device step count value is not empty, otherwise assertion exception occurs</p>
     *
     * @see Test
     */
    @Test
    fun leftDevicePropertiesRead_ThenCheckStepCount()
    {
        assertNotSame(0, configuration.eddyStoneMap[EarSide.Left]?.stepCount)
    }

    /**
     * <p>Checks the eddystone supported right side hearing aid device step count value</p>
     * <p>The test passes if the device step count value is not empty, otherwise assertion exception occurs</p>
     *
     * @see Test
     */
    @Test
    fun rightDevicePropertiesRead_ThenCheckStepCount()
    {
        assertNotSame(0, configuration.eddyStoneMap[EarSide.Right]?.stepCount)
    }
}
