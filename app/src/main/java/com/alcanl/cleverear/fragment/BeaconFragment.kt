package com.alcanl.cleverear.fragment

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.MainContainerActivity
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentBeaconBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.EddystoneHearingAid
import com.alcanl.cleverear.fitting.event.listener.IEventListener
import com.alcanl.cleverear.util.EMERGENCY_NUMBER
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.SHARED_PREF_DEVICE
import com.alcanl.cleverear.util.WHAT_START_SHIMMER
import com.alcanl.cleverear.util.WHAT_STOP_SHIMMER
import com.alcanl.cleverear.util.extension.startAlphaAnimation
import com.alcanl.cleverear.util.listeners.BeaconFragmentEventListener
import com.alcanl.cleverear.util.listeners.event.ConnectionEvent
import com.alcanl.cleverear.viewmodel.BeaconFragmentListenersViewModel
import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser
import com.neovisionaries.bluetooth.ble.advertising.EddystoneTLM
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference
import java.util.EventObject
import javax.inject.Inject

@AndroidEntryPoint
class BeaconFragment : Fragment(), IEventListener {

    @Inject
    lateinit var configuration : ConfigurationModule

    @Inject
    lateinit var listener : BeaconFragmentEventListener

    lateinit var binding: FragmentBeaconBinding

    private lateinit var mHandler: BeaconHandler

    private var mJob: Job? = null

    private val mBluetoothLeScanner by lazy {
        requireContext().getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner
    }

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

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onEventReceive(event: EventObject)
    {
        if (event is ConnectionEvent && event.connectionData == 1)
            activity?.let {
                it.runOnUiThread {
                    Toast.makeText(it, R.string.toast_connection_lost_text, Toast.LENGTH_SHORT)
                        .show()
                    it.finish()
                    configuration.apply {
                        isConnected = false
                        hearingAidMap.filter { ha -> ha.value.address == event.eventSender }
                            .values.first().isConnected = false
                    }
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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_beacon, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy()
    {
        super.onDestroy()
        EventBus.getDefault().unregister(this)

        binding.shimmerLayout.apply {
            if (isShimmerVisible) {
                stopShimmer()
                hideShimmer()
            }
        }

        if (listener.isListenerOnBoard())
            listener.stopListener()

        if (mJob != null && !mJob!!.isCancelled)
            mJob?.cancel()

        if (configuration.eddyStoneMap.none { it.value.isTrackingActive })
            mBluetoothLeScanner.stopScan(mScanCallback)
    }

    override fun onStart()
    {
        super.onStart()
        EventBus.getDefault().register(this)
        listener.startEventListener()
    }

    override fun onPause()
    {
        super.onPause()
        binding.shimmerLayout.apply {
            if (isShimmerVisible) {
                stopShimmer()
                hideShimmer()
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        binding.shimmerLayout.apply {
            stopShimmer()
            hideShimmer()
        }

        if (configuration.hearingAidMap.none { it.value.isEddystoneTLM })
            handleNotSupportedHearingAid()
        else
            handleEddystoneTLM()
    }

    private fun initialize()
    {
        binding.viewModel = BeaconFragmentListenersViewModel(this)
        mHandler = BeaconHandler(this)
    }

    @SuppressLint("MissingPermission")
    private fun discoverEddyStoneBeacons()
    {
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .setReportDelay(0)
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

                    if (isDoubleDeviceConnection)
                        when (address) {
                            hearingAidMap[EarSide.Left]?.address -> eddyStoneMap[EarSide.Left]?.setEddystoneProperties(it, address)
                            hearingAidMap[EarSide.Right]?.address -> eddyStoneMap[EarSide.Right]?.setEddystoneProperties(it, address)
                            else -> {}
                        }
                    else
                        eddyStoneMap[earSide]?.setEddystoneProperties(it, address)
                }
        }
    }

    private fun handleNotSupportedHearingAid()
    {
        with(binding) {
            frameLayoutNotSupported.visibility = View.VISIBLE
            linearLayoutMain.visibility = View.INVISIBLE
        }
    }

    private fun handleEddystoneTLM()
    {
        configuration.eddyStoneMap.forEach { (earSide, eddystoneHearingAid) ->

            if (eddystoneHearingAid.isTrackingActive)
                if (earSide == EarSide.Left)
                    binding.buttonTrackLeftDevice.text = getString(R.string.button_disable_tracking_text)
                else
                    binding.buttonTrackRightDevice.text = getString(R.string.button_disable_tracking_text)
        }

        discoverEddyStoneBeacons()

        mJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    mHandler.apply {
                        sendEmptyMessage(WHAT_START_SHIMMER)
                        delay(2000)
                        sendEmptyMessage(WHAT_STOP_SHIMMER)
                    }

                    delay(4000)

                } catch (ex: Exception) {
                    Log.e(BeaconFragment::class.simpleName, ex.message!!)
                }
            }
        }
    }

    class BeaconHandler(fragment: BeaconFragment) : Handler(Looper.myLooper()!!) {

        private val mWeakReference = WeakReference(fragment)

        @SuppressLint("SetTextI18n")
        private fun fillTextViewsForDeviceCallback(earSide: EarSide, hearingAid: EddystoneHearingAid)
        {
            mWeakReference.get()?.binding?.let {
                if (earSide == EarSide.Left) {
                    it.textViewLeftDeviceTLMVersion.startAlphaAnimation(mWeakReference.get()?.requireContext()!!, hearingAid.tlmVersion.toString())
                    it.textViewLeftDeviceTLMAdvertisementCount.startAlphaAnimation(mWeakReference.get()?.requireContext()!!, hearingAid.advertisementCount.toString())
                    it.textViewLeftDeviceTLMTemperature.startAlphaAnimation(mWeakReference.get()?.requireContext()!!, hearingAid.temperature.toString())
                    it.textViewLeftDeviceTLMStepCount.startAlphaAnimation(mWeakReference.get()?.requireContext()!!, hearingAid.stepCount.toString())
                    it.textViewLeftDeviceTLMBatteryVoltage.startAlphaAnimation(mWeakReference.get()?.requireContext()!!, hearingAid.batteryVoltage.toString())
                    it.textViewLeftDeviceTLMElapsedTime.startAlphaAnimation(mWeakReference.get()?.requireContext()!!, hearingAid.elapsedTime.toString())
                }
                else {
                    it.textViewRightDeviceTLMVersion.startAlphaAnimation(mWeakReference.get()?.requireContext()!!, hearingAid.tlmVersion.toString())
                    it.textViewRightDeviceTLMAdvertisementCount.startAlphaAnimation(mWeakReference.get()?.requireContext()!!, hearingAid.advertisementCount.toString())
                    it.textViewRightDeviceTLMTemperature.startAlphaAnimation(mWeakReference.get()?.requireContext()!!, hearingAid.temperature.toString())
                    it.textViewRightDeviceTLMStepCount.startAlphaAnimation(mWeakReference.get()?.requireContext()!!, hearingAid.stepCount.toString())
                    it.textViewRightDeviceTLMBatteryVoltage.startAlphaAnimation(mWeakReference.get()?.requireContext()!!, hearingAid.batteryVoltage.toString())
                    it.textViewRightDeviceTLMElapsedTime.startAlphaAnimation(mWeakReference.get()?.requireContext()!!, hearingAid.elapsedTime.toString())
                }
            }
        }

        private fun startShimmer()
        {
            mWeakReference.get()?.binding?.shimmerLayout?.showShimmer(true)
        }

        private fun stopShimmer()
        {
            mWeakReference.get()?.binding?.let {
                it.shimmerLayout.hideShimmer()
                it.shimmerLayout.stopShimmer()
            }
            mWeakReference.get()?.configuration?.eddyStoneMap?.forEach(this::fillTextViewsForDeviceCallback)
        }

        override fun handleMessage(msg: Message)
        {
            when (msg.what) {
                31 -> startShimmer()
                32 -> stopShimmer()
            }
        }
    }

    fun onEddystoneTrackingButtonClicked(earSide: EarSide)
    {
        if (!(requireActivity() as MainContainerActivity).mSendSmsPermission) {
            (requireActivity() as MainContainerActivity).requestSmsPermission(earSide,
                if (earSide == EarSide.Left) binding.buttonTrackLeftDevice else binding.buttonTrackRightDevice)
            return
        }

        if (requireActivity().getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE).getString(EMERGENCY_NUMBER, "")?.isBlank()!!)
            (requireActivity() as MainContainerActivity).showSMSInformationDialog(earSide,
                if (earSide == EarSide.Left) binding.buttonTrackLeftDevice else binding.buttonTrackRightDevice)
        else
            configuration.eddyStoneMap[earSide]?.apply {
                isTrackingActive = !isTrackingActive

                if (earSide == EarSide.Left)
                    binding.buttonTrackLeftDevice.text = getString(if (isTrackingActive) R.string.button_disable_tracking_text else R.string.button_enable_tracking_text)
                else
                    binding.buttonTrackRightDevice.text = getString(if (isTrackingActive) R.string.button_disable_tracking_text else R.string.button_enable_tracking_text)
            }
    }
}