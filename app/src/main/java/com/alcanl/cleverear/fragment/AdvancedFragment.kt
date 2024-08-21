package com.alcanl.cleverear.fragment


import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentAdvancedBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.fitting.event.listener.IEventListener
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.listeners.AdvancedActivityEventListener
import com.alcanl.cleverear.util.listeners.event.BatteryEvent
import com.alcanl.cleverear.util.listeners.event.ConnectionEvent
import com.alcanl.cleverear.util.listeners.event.MemoryEvent
import com.alcanl.cleverear.util.listeners.event.VolumeEvent
import com.alcanl.cleverear.viewmodel.AdvancedFragmentListenersViewModel
import com.ark.ArkException
import com.ark.EventHandler
import com.ark.ParameterSpace
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.EventObject
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@AndroidEntryPoint
class AdvancedFragment : Fragment(), IEventListener {
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var configuration : ConfigurationModule
    @Inject
    lateinit var threadPool: ExecutorService
    @Inject
    lateinit var listener: AdvancedActivityEventListener

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
    override fun onStart()
    {
        super.onStart()
        initialize()
        listener.startEventListener()
        EventBus.getDefault().register(this)
    }
    override fun onResume()
    {
        super.onResume()

        if (!configuration.isConnected) {
            activity?.finish()
            return
        }

        if (!listener.isListenerOnBoard())
            listener.startEventListener()
    }
    override fun onStop()
    {
        super.onStop()
        if (listener.isListenerOnBoard())
            listener.stopListener()

    }
    override fun onDestroy() {
        super.onDestroy()

        if (listener.isListenerOnBoard())
            listener.stopListener()

        EventBus.getDefault().unregister(this)

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
        threadPool.execute {
            with(configuration) {
                if (!isDoubleDeviceConnection)
                    hearingAidMap[earSide]?.volume = event.volumeData
                else
                    if (event.eventSender == hearingAidMap[EarSide.Left]?.address)
                        hearingAidMap[EarSide.Left]?.volume = event.volumeData
                    else
                        hearingAidMap[EarSide.Right]?.volume = event.volumeData
            }

            activity?.runOnUiThread {
                Toast.makeText(requireContext(), String.format(
                    ContextCompat.getString(requireContext(),
                        R.string.listener_volume_level_text
                    ) + " %d", event.volumeData), Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun onBatteryEventReceived(event: BatteryEvent)
    {
        threadPool.execute {
            with(configuration) {
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
        threadPool.execute {
            with(configuration) {
                if (!isDoubleDeviceConnection)
                    hearingAidMap[earSide]?.currentMemory = ParameterSpace.value(event.memoryData)
                else
                    if (event.eventSender == hearingAidMap[EarSide.Left]?.address!!)
                        hearingAidMap[EarSide.Left]?.currentMemory = ParameterSpace.value(event.memoryData)
                    else
                        hearingAidMap[EarSide.Right]?.currentMemory = ParameterSpace.value(event.memoryData)
            }
        }
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), String.format(
                ContextCompat.getString(requireContext(), R.string.text_active_memory)
                        + (event.memoryData + 1).toString()), Toast.LENGTH_SHORT).show()
        }
    }
    private fun onConnectionEventReceived(event: ConnectionEvent)
    {
        if (event.connectionData == 1) {
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), R.string.toast_connection_lost_text, Toast.LENGTH_SHORT)
                    .show()
                activity?.finish()
                configuration.isConnected = false
            }
        }
    }
    private fun initialize()
    {
        initBinding()
        initSwitch()
        initSliders()
        initTextViews()
    }
    private fun initSliders()
    {
        with(configuration) {
            if (!isDoubleDeviceConnection) {
                with(mBinding) {
                    sliderAuxLeft.visibility = View.INVISIBLE
                    sliderAuxRight.visibility= View.INVISIBLE
                    sliderMicLeft.visibility = View.INVISIBLE
                    sliderMicRight.visibility = View.INVISIBLE
                    initCommonSliderForSingleDevice(sliderMicCommon)
                    initCommonSliderForSingleDevice(sliderAuxCommon)
                }
            }
            else
                with(mBinding) {
                    if (!isDualDeviceConfig) {
                        sliderAuxCommon.visibility = View.INVISIBLE
                        sliderMicCommon.visibility = View.INVISIBLE
                    }
                    else {
                        sliderMicLeft.visibility = View.INVISIBLE
                        sliderAuxLeft.visibility = View.INVISIBLE
                        sliderMicRight.visibility = View.INVISIBLE
                        sliderAuxRight.visibility = View.INVISIBLE
                    }
                }
        }
    }
    private fun initCommonSliderForSingleDevice(slider: Slider)
    {
        slider.apply {
            visibility = View.VISIBLE
            isEnabled = true
            trackActiveTintList = initCommonSliderTheme()
            thumbTintList = initCommonSliderTheme()
        }
    }
    private fun initCommonSliderTheme() : ColorStateList
    {
        return if (configuration.earSide == EarSide.Left)
            AppCompatResources.getColorStateList(
                requireContext(),
                R.color.left_device_slider
            )
        else
            AppCompatResources.getColorStateList(
                requireContext(),
                R.color.right_device_slider
            )
    }
    fun switchOnCheckedChanged()
    {
        with(configuration) {
            activity?.runOnUiThread {
                isDualDeviceConfig = if (!isDualDeviceConfig) {
                    with(mBinding) {
                        sliderFirstAnimationCallback(sliderAuxLeft, R.anim.translate_slider_adv_left_to_right_left)
                        sliderFirstAnimationCallback(sliderMicLeft, R.anim.translate_slider_adv_left_to_right_left)
                        sliderFirstAnimationCallback(sliderAuxRight, R.anim.translate_slider_adv_right_to_left_right)
                        sliderFirstAnimationCallback(sliderMicRight, R.anim.translate_slider_adv_right_to_left_right)
                    }
                    true
                } else {
                    with(mBinding) {
                        sliderSecondAnimationCallback(sliderAuxLeft, R.anim.translate_slider_adv_right_to_left_left)
                        sliderSecondAnimationCallback(sliderMicLeft, R.anim.translate_slider_adv_right_to_left_left)
                        sliderSecondAnimationCallback(sliderAuxRight, R.anim.translate_slider_adv_left_to_right_right)
                        sliderSecondAnimationCallback(sliderMicRight, R.anim.translate_slider_adv_left_to_right_right)
                        false
                    }
                }
            }
        }
    }
    private fun sliderFirstAnimationCallback(slider: Slider, anim: Int)
    {
        with(slider) {
            startAnimation(
                android.view.animation.AnimationUtils.loadAnimation(
                    requireContext(),
                    anim
                ).also { it.setAnimationListener(setSlidersFirstAnimation(slider)) })
        }
    }
    private fun sliderSecondAnimationCallback(slider: Slider, anim: Int)
    {
        with(slider) {
            startAnimation(
                android.view.animation.AnimationUtils.loadAnimation(
                    requireContext(),
                    anim
                ).also { it.setAnimationListener(setSlidersSecondAnimation(slider)) })
        }
    }
    private fun setSlidersFirstAnimation(slider: Slider) : AnimationListener
    {
        return object : AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                slider.visibility = View.INVISIBLE
            }

            override fun onAnimationEnd(animation: Animation?) {

                startCommonSlidersAnimation(mBinding.sliderAuxCommon)
                startCommonSlidersAnimation(mBinding.sliderMicCommon)

                with(mBinding) {
                    textViewAuxL.visibility = View.INVISIBLE
                    textViewAuxR.visibility= View.INVISIBLE
                    textViewMicL.visibility = View.INVISIBLE
                    textViewMicR.visibility = View.INVISIBLE
                }

            }
            override fun onAnimationRepeat(animation: Animation?) {

            }
        }
    }
    private fun setSlidersSecondAnimation(slider: Slider) : AnimationListener
    {
        return object : AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                with(mBinding) {
                    sliderAuxCommon.apply {
                        visibility = View.INVISIBLE; isEnabled = false
                    }
                    sliderMicCommon.apply {
                        visibility = View.INVISIBLE; isEnabled = false
                    }

                    textViewAuxL.visibility = View.VISIBLE
                    textViewAuxR.visibility= View.VISIBLE
                    textViewMicL.visibility = View.VISIBLE
                    textViewMicR.visibility = View.VISIBLE
                }
            }

            override fun onAnimationEnd(animation: Animation?) {
                slider.apply {
                    visibility = View.VISIBLE
                    isEnabled = true
                }
            }

            override fun onAnimationRepeat(animation: Animation?) {

            }
        }
    }
    private fun startCommonSlidersAnimation(slider: Slider)
    {
        slider.startAnimation(
            android.view.animation.AnimationUtils.loadAnimation(
                requireContext(), R.anim.alpha_blink_anim
            ).also {it.setAnimationListener(

                object: AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        slider.visibility = View.VISIBLE
                        slider.isEnabled = true
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                    }
                }
            ) })

    }
    private fun initBinding()
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
                    hearingAidMap.forEach {
                        setStreamAddressButtonClickedCallback(result,channel, it.value) }
                else
                    setStreamAddressButtonClickedCallback(result, channel, hearingAidMap[earSide]!!)
        }
    }
    private fun initSwitch()
    {
        if (configuration.isDoubleDeviceConnection)
            mBinding.switchDual.visibility = View.VISIBLE
    }
    private fun setStreamAddressButtonClickedCallback(result: Int, channel: Int, hearingAid: HearingAid)
    {
        threadPool.execute {
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
        threadPool.execute {
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

    private fun initTextViews()
    {
        with(configuration) {
            if (isDoubleDeviceConnection) {
                if (isDualDeviceConfig)
                    mBinding.apply {
                        textViewAuxL.visibility = View.INVISIBLE
                        textViewAuxR.visibility = View.INVISIBLE
                        textViewMicL.visibility = View.INVISIBLE
                        textViewMicR.visibility = View.INVISIBLE
                    }
                else
                    return
            }

            else
                if (earSide == EarSide.Left)
                    mBinding.apply {
                        textViewMicR.visibility = View.INVISIBLE
                        textViewAuxR.visibility = View.INVISIBLE
                    }

                else
                    mBinding.apply {
                        textViewAuxL.visibility = View.INVISIBLE
                        textViewMicL.visibility = View.INVISIBLE
                    }

        }
    }
    private fun micSliderValueChanged(value: Float, earSide: EarSide)
    {
        threadPool.execute { micSliderValueChangedCallback(value, earSide) }
    }
    @Synchronized
    private fun micSliderValueChangedCallback(value: Float, earSide: EarSide)
    {
        try {
            with(configuration) {
                if (isConnected) {
                    hearingAidMap[earSide]?.wirelessControl?.micAttenuation = value.toInt()
                    hearingAidMap[earSide]?.micAttenuation = value.toInt()
                }
            }
        } catch (ex: ArkException) {
            Log.e("mic", ex.message!!)
        }
    }
    private fun auxSliderValueChanged(value: Float, earSide: EarSide)
    {
        threadPool.execute { auxSliderValueChangedCallback(value, earSide) }
    }
    @Synchronized
    private fun auxSliderValueChangedCallback(value: Float, earSide: EarSide)
    {
        try {
            with(configuration) {
                if (isConnected) {
                    hearingAidMap[earSide]?.wirelessControl?.auxAttenuation = value.toInt()
                    hearingAidMap[earSide]?.auxAttenuation = value.toInt()
                }
            }
        } catch (ex: ArkException) {
            Log.e("aux", ex.message!!)
        }
    }
    private fun setSelectedModeButtonView(button: Button)
    {
        activity?.runOnUiThread {
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
        with(configuration) {
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
        with(configuration) {
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
        with(configuration) {
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
    fun setStreamAddressButtonClicked(view: View)
    {
        activity?.runOnUiThread { (view as ImageButton).startAnimation( android.view.animation.AnimationUtils.loadAnimation(
            requireContext(), R.anim.alpha_blink_anim
        ).also {it.setAnimationListener(

            object: AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    val channel: Int

                    try {
                        channel = mBinding.editTextChannel.text.toString().toInt()
                        setStreamAddressButtonClicked(channel)
                    } catch (_: NumberFormatException) {
                        Toast.makeText(requireContext(), R.string.toast_channel_error_text, Toast.LENGTH_LONG).show()
                    }
                }
                override fun onAnimationEnd(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
            }
        ) })
        }
    }
}


