package com.alcanl.cleverear.fragment

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentBeaconBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.EddystoneHearingAid
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.viewmodel.BeaconFragmentListenersViewModel
import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser
import com.neovisionaries.bluetooth.ble.advertising.EddystoneTLM
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BeaconFragment : Fragment() {

    @Inject
    lateinit var configuration : ConfigurationModule

    private lateinit var mBinding: FragmentBeaconBinding

    private lateinit var mBluetoothLeScanner: BluetoothLeScanner

    private val mScanCallback: ScanCallback by lazy {

        object : ScanCallback() {

            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult) {

                if (result.scanRecord != null)
                    determineEddystoneBeacons(result.scanRecord!!.bytes, result.device.address)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        initialize()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_beacon, container, false)
        mBinding.lifecycleOwner = this
        return mBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy()
    {
        super.onDestroy()
        mBluetoothLeScanner.stopScan(mScanCallback)

        mBinding.shimmerLayout.apply {
            if (isShimmerVisible) {
                stopShimmer()
                hideShimmer()
            }
        }
    }

    override fun onPause()
    {
        super.onPause()
        mBinding.shimmerLayout.apply {
            if (isShimmerVisible) {
                stopShimmer()
                hideShimmer()
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        mBinding.shimmerLayout.apply {
            if (!isShimmerVisible)
                startShimmer()
        }

        if (configuration.hearingAidMap.none { it.value.isEddystoneTLM })
            handleNotSupportedHearingAid()
        else
            handleEddystoneTLM()
    }

    private fun initialize()
    {
        mBinding.viewModel = BeaconFragmentListenersViewModel(this)
        mBluetoothLeScanner = requireContext().getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner
    }

    @SuppressLint("MissingPermission")
    private fun discoverEddyStoneBeacons()
    {
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .setReportDelay(2)
            .build()

        mBluetoothLeScanner.startScan(emptyList(), settings, mScanCallback)
        configuration.isEddyStoneScanInProgress = true
    }

    private fun determineEddystoneBeacons(scanRecord: ByteArray, address: String)
    {
        ADPayloadParser.getInstance().parse(scanRecord).forEach {

            if (it is EddystoneTLM)
                with(configuration) {
                    if (hearingAidMap.none { ha -> ha.value.address == address })
                        return

                    if (isDoubleDeviceConnection) {
                        if (address == hearingAidMap[EarSide.Left]?.address)
                            eddyStoneMap[EarSide.Left] = EddystoneHearingAid()
                                .also { eddyStoneLeft -> eddyStoneLeft.setEddystoneProperties(it, address) }
                        else if (address == hearingAidMap[EarSide.Right]?.address)
                            eddyStoneMap[EarSide.Right] = EddystoneHearingAid()
                                .also { eddyStoneRight -> eddyStoneRight.setEddystoneProperties(it, address) }
                    }
                    else {
                        eddyStoneMap[earSide] = EddystoneHearingAid()
                            .also { eddystone -> eddystone.setEddystoneProperties(it, address) }
                    }
                }
        }
    }

    private fun handleNotSupportedHearingAid()
    {
        with(mBinding) {
            frameLayoutNotSupported.visibility = View.VISIBLE
            linearLayoutMain.visibility = View.INVISIBLE
            shimmerLayout.apply {
                stopShimmer()
                hideShimmer()
            }
        }
    }

    private fun handleEddystoneTLM()
    {
        discoverEddyStoneBeacons()
    }

    fun enableEddystoneTracking(earSide: EarSide)
    {
        configuration.eddyStoneMap[earSide]?.apply {
            isTrackingActive = !isTrackingActive
        }
    }
}