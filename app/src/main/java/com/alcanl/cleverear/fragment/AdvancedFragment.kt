package com.alcanl.cleverear.fragment


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.alcanl.cleverear.MainActivity
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentAdvancedBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.fitting.event.listener.IEventListener
import com.alcanl.cleverear.util.BUNDLE_INTENT_FROM_SPLASH
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.extension.startSetStreamChannelAnimation
import com.alcanl.cleverear.util.extension.startAuxMicSlidersFirstAnimation
import com.alcanl.cleverear.util.extension.startAuxMicSlidersSecondAnimation
import com.alcanl.cleverear.util.listeners.AdvancedFragmentEventListener
import com.alcanl.cleverear.util.listeners.event.BatteryEvent
import com.alcanl.cleverear.util.listeners.event.ConnectionEvent
import com.alcanl.cleverear.util.listeners.event.MemoryEvent
import com.alcanl.cleverear.util.listeners.event.VolumeEvent
import com.alcanl.cleverear.util.service.AutoConnectService
import com.alcanl.cleverear.viewmodel.AdvancedFragmentListenersViewModel
import com.ark.ArkException
import com.ark.ParameterSpace
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.EventObject
import javax.inject.Inject

@AndroidEntryPoint
class AdvancedFragment : Fragment(), IEventListener {

    @Inject
    lateinit var configuration : ConfigurationModule

    @Inject
    lateinit var mutex: Mutex

    @Inject
    lateinit var listener: AdvancedFragmentEventListener

    private lateinit var mBinding : FragmentAdvancedBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_advanced, container, false)
        mBinding.lifecycleOwner = this
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        initialize()
    }

    override fun onStart()
    {
        super.onStart()

        listener.apply {
            if (!isListenerOnBoard())
                startEventListener()
        }

        EventBus.getDefault().apply {
            if (!isRegistered(this@AdvancedFragment))
                register(this@AdvancedFragment)
        }
    }

    override fun onResume()
    {
        super.onResume()

        if (!configuration.isConnected) {
            requireActivity().finish()
            return
        }

        listener.apply {
            if (!isListenerOnBoard())
                startEventListener()
        }
    }
    override fun onStop()
    {
        super.onStop()
        listener.apply {
            if (isListenerOnBoard())
                stopListener()
        }
    }
    override fun onDestroy() {
        super.onDestroy()

        listener.apply {
            if (isListenerOnBoard())
                stopListener()
        }

        EventBus.getDefault().apply {
            if (isRegistered(this@AdvancedFragment))
                unregister(this@AdvancedFragment)
        }
    }
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onEventReceive(event: EventObject)
    {
        if (isVisible)
            when (event) {
                is VolumeEvent -> onVolumeEventReceived(event)
                is MemoryEvent -> onMemoryEventReceived(event)
                is BatteryEvent -> onBatteryEventReceived(event)
                is ConnectionEvent -> onConnectionEventReceived(event)
            }
    }

    private fun onVolumeEventReceived(event: VolumeEvent)
    {
        lifecycleScope.launch(Dispatchers.Default) {

            configuration.apply {
                if (!isDoubleDeviceConnection)
                    hearingAidMap[earSide]?.volume = event.volumeData
                else
                    if (event.eventSender == hearingAidMap[EarSide.Left]?.address)
                        hearingAidMap[EarSide.Left]?.volume = event.volumeData
                    else
                        hearingAidMap[EarSide.Right]?.volume = event.volumeData
            }

            launch(Dispatchers.Main) {
                Toast.makeText(requireContext(), String.format(
                    ContextCompat.getString(requireContext(),
                        R.string.listener_volume_level_text
                    ) + " %d", event.volumeData), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onBatteryEventReceived(event: BatteryEvent)
    {
        lifecycleScope.launch(Dispatchers.Default) {

            configuration.apply {
                if (!isDoubleDeviceConnection)
                    hearingAidMap[earSide]?.batteryLevel = event.batteryData
                else
                    if (event.eventSender == hearingAidMap[EarSide.Left]?.address)
                        hearingAidMap[EarSide.Left]?.batteryLevel = event.batteryData
                    else
                        hearingAidMap[EarSide.Right]?.batteryLevel = event.batteryData
            }
        }
    }

    private fun onMemoryEventReceived(event: MemoryEvent)
    {
        lifecycleScope.launch(Dispatchers.Default) {

            configuration.apply {
                if (!isDoubleDeviceConnection)
                    hearingAidMap[earSide]?.currentMemory = ParameterSpace.value(event.memoryData)
                else
                    if (event.eventSender == hearingAidMap[EarSide.Left]?.address!!)
                        hearingAidMap[EarSide.Left]?.currentMemory = ParameterSpace.value(event.memoryData)
                    else
                        hearingAidMap[EarSide.Right]?.currentMemory = ParameterSpace.value(event.memoryData)
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(requireContext(), String.format(
                ContextCompat.getString(requireContext(), R.string.text_active_memory)  + (event.memoryData + 1).toString()), Toast.LENGTH_SHORT).show()
        }
    }

    private fun onConnectionEventReceived(event: ConnectionEvent)
    {
        if (event.connectionData == 1) {
            lifecycleScope.launch(Dispatchers.Main) {
                Toast.makeText(requireContext(), R.string.toast_connection_lost_text, Toast.LENGTH_SHORT).show()

                configuration.apply {
                    if (isServiceOnBoard) {
                        isSystemRestartOnService = false
                        requireActivity().stopService(Intent(requireActivity(), AutoConnectService::class.java))
                    }
                }

                try {
                    configuration.apply {
                        if (isAshaConnected || requireActivity().intent.getBooleanExtra(
                                BUNDLE_INTENT_FROM_SPLASH, false))
                            requireActivity().apply {
                                startActivity(Intent(this, MainActivity::class.java))
                            }
                        isConnected = false
                        hearingAidMap.filter { ha -> ha.value.address == event.eventSender }
                            .values.first().isConnected = false
                    }
                } catch (ex: NoSuchElementException) {
                    Log.e(ConnectionFragment::class.simpleName, ex.message!!)
                }
                requireActivity().finish()
            }
        }
    }

    private fun initialize()
    {
        mBinding.viewModel = AdvancedFragmentListenersViewModel(this)
    }

    private fun setStreamAddressButtonClicked(channel: Int)
    {
        if (channel !in 1..255) {
            Toast.makeText(requireContext(), R.string.toast_channel_error_text, Toast.LENGTH_LONG).show()
            mBinding.editTextChannel.setText(getString(R.string.empty_string))
            return
        }
        val addressShift: Int = channel shl 16
        val dataOr = 0x00CDE6 //52710
        val result = dataOr or addressShift

        with(configuration) {
            if (isConnected)
                if (isDoubleDeviceConnection)
                    hearingAidMap.forEach { setStreamAddressButtonClickedCallback(result,channel, it.value) }
                else
                    setStreamAddressButtonClickedCallback(result, channel, hearingAidMap[earSide]!!)
        }
    }

    private fun setStreamAddressButtonClickedCallback(result: Int, channel: Int, hearingAid: HearingAid)
    {
        lifecycleScope.launch(Dispatchers.IO) {

            try {
                with(hearingAid) {
                    streamAddressRaw = result
                    streamAddress = channel
                    wirelessControl?.streamAddress = result
                }
            } catch (_: ArkException) {}
        }
    }

    private fun modeButtonClickedCallback(view: View, hearingAid: HearingAid)
    {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                hearingAid.apply {
                    streamChannelList = when (view) {
                        mBinding.buttonModeOne -> 0
                        mBinding.buttonModeTwo -> 1
                        else -> 2
                    }
                    wirelessControl?.streamChannelList = when (view) {
                        mBinding.buttonModeOne -> 0
                        mBinding.buttonModeTwo -> 1
                        else -> 2
                    }
                }
            } catch (_: ArkException) {}
        }
    }

    private fun micSliderValueChanged(value: Float, earSide: EarSide)
    {
        lifecycleScope.launch(Dispatchers.IO) { micSliderValueChangedCallback(value, earSide) }
    }

    private suspend fun micSliderValueChangedCallback(value: Float, earSide: EarSide)
    {
        mutex.withLock {
            try {
                configuration.apply {
                    if (isConnected) {
                        hearingAidMap[earSide]?.wirelessControl?.micAttenuation = value.toInt()
                        hearingAidMap[earSide]?.micAttenuation = value.toInt()
                    }
                }
            } catch (ex: ArkException) {
                Log.e(AdvancedFragment::class.simpleName, ex.message!!)
            }
        }
    }

    private fun auxSliderValueChanged(value: Float, earSide: EarSide)
    {
        lifecycleScope.launch(Dispatchers.IO) { auxSliderValueChangedCallback(value, earSide) }
    }

    private suspend fun auxSliderValueChangedCallback(value: Float, earSide: EarSide)
    {
        mutex.withLock {
            try {
                with(configuration) {
                    if (isConnected) {
                        hearingAidMap[earSide]?.wirelessControl?.auxAttenuation = value.toInt()
                        hearingAidMap[earSide]?.auxAttenuation = value.toInt()
                    }
                }
            } catch (ex: ArkException) {
                Log.e(AdvancedFragment::class.simpleName, ex.message!!)
            }
        }
    }

    private fun setSelectedModeButtonView(button: Button)
    {
        lifecycleScope.launch(Dispatchers.Main) {
            button.backgroundTintList = AppCompatResources.getColorStateList(
                requireContext(),
                com.google.android.material.R.color.design_default_color_secondary_variant
            )
            button.foregroundTintList = AppCompatResources.getColorStateList(
                requireContext(), R.color.white
            )

            mBinding.constraintLayoutMode.children.filter{
                it is Button && it != button }.forEach { setRestModeButtonsDefaultView(it as Button) }
        }
    }

    private fun setRestModeButtonsDefaultView(button: Button)
    {
        button.backgroundTintList = AppCompatResources.getColorStateList(
            requireContext(), R.color.default_button_color)
        button.foregroundTintList = AppCompatResources.getColorStateList(
            requireContext(), R.color.black)
    }

    fun modeButtonClicked(view: View)
    {
        configuration.apply {
            if (isConnected) {
                setSelectedModeButtonView(view as Button)

                if (!isDoubleDeviceConnection)
                    modeButtonClickedCallback(view, hearingAidMap[earSide]!!)
                else
                    hearingAidMap.forEach { modeButtonClickedCallback(view, it.component2()) }
            }
        }
    }

    fun micDualSliderValueChanged(value: Float)
    {
        configuration.apply {
            if (isDoubleDeviceConnection) {
                micLeftSliderValueChanged(value)
                micRightSliderValueChanged(value)
                return
            }

            if (earSide == EarSide.Left)
                micLeftSliderValueChanged(value)
            else
                micRightSliderValueChanged(value)

        }
    }

    fun auxDualSliderValueChanged(value: Float)
    {
        configuration.apply {
            if (isDoubleDeviceConnection) {
                auxLeftSliderValueChanged(value)
                auxRightSliderValueChanged(value)
                return
            }

            if (earSide == EarSide.Left)
                auxLeftSliderValueChanged(value)
            else
                auxRightSliderValueChanged(value)

        }
    }

    fun micLeftSliderValueChanged(value: Float)
    {
        micSliderValueChanged(value, EarSide.Left)
    }

    fun micRightSliderValueChanged(value: Float)
    {
        micSliderValueChanged(value, EarSide.Right)
    }

    fun auxLeftSliderValueChanged(value: Float)
    {
        auxSliderValueChanged(value, EarSide.Left)
    }

    fun auxRightSliderValueChanged(value: Float)
    {
        auxSliderValueChanged(value, EarSide.Right)
    }

    fun setStreamAddressButtonClicked(imageButton: ImageButton)
    {
        lifecycleScope.launch(Dispatchers.Main) {

            try {
                val channel = mBinding.editTextChannel.text.toString().toInt()
                mBinding.editTextChannel.setText(R.string.empty_string)
                imageButton.startSetStreamChannelAnimation(requireContext(), { setStreamAddressButtonClicked(channel) },
                    {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.settings_updated))
                        .setIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.check_tick))
                        .setMessage(getString(R.string.toast_channel_and_mode_set_text,
                            configuration.hearingAidMap[configuration.earSide]?.streamChannelList!!, channel))
                        .setPositiveButton(getString(R.string.alert_dialog_connection_problem_ok_button)) { d,_ -> d.dismiss()}
                        .setCancelable(true)
                        .show()
                    })
            } catch (_: NumberFormatException) {
                Toast.makeText(requireContext(), R.string.toast_channel_error_text, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun switchOnCheckedChanged()
    {
        configuration.apply {

                    if (!isDualDeviceConfig)
                        lifecycleScope.launch(Dispatchers.Main) {
                            mBinding.apply {
                                sliderAuxLeft.startAuxMicSlidersFirstAnimation(
                                    this,
                                    requireContext(),
                                    R.anim.translate_slider_adv_left_to_right_left
                                )
                                sliderMicLeft.startAuxMicSlidersFirstAnimation(
                                    this,
                                    requireContext(),
                                    R.anim.translate_slider_adv_left_to_right_left
                                )
                                sliderAuxRight.startAuxMicSlidersFirstAnimation(
                                    this,
                                    requireContext(),
                                    R.anim.translate_slider_adv_right_to_left_right
                                )
                                sliderMicRight.startAuxMicSlidersFirstAnimation(
                                    this,
                                    requireContext(),
                                    R.anim.translate_slider_adv_right_to_left_right
                                )

                            }
                        }
                     else
                        lifecycleScope.launch(Dispatchers.Main) {
                            mBinding.apply {
                                sliderAuxLeft.startAuxMicSlidersSecondAnimation(
                                    this,
                                    requireContext(),
                                    R.anim.translate_slider_adv_right_to_left_left
                                )
                                sliderMicLeft.startAuxMicSlidersSecondAnimation(
                                    this,
                                    requireContext(),
                                    R.anim.translate_slider_adv_right_to_left_left
                                )
                                sliderAuxRight.startAuxMicSlidersSecondAnimation(
                                    this,
                                    requireContext(),
                                    R.anim.translate_slider_adv_left_to_right_right
                                )
                                sliderMicRight.startAuxMicSlidersSecondAnimation(
                                    this,
                                    requireContext(),
                                    R.anim.translate_slider_adv_left_to_right_right
                                )

                            }
                        }
        }
    }
}


