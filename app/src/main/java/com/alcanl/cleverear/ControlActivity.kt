package com.alcanl.cleverear

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.widget.Button
import android.widget.TableRow
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityControlBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.listeners.ControlActivityEventListener
import com.alcanl.cleverear.viewmodel.ControlActivityListenersViewModel
import com.ark.ArkException
import com.ark.EventHandler
import com.ark.ParameterSpace
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import javax.inject.Inject
@AndroidEntryPoint
class ControlActivity : AppCompatActivity() {
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var threadPool: ExecutorService
    @Inject
    lateinit var configuration : ConfigurationModule
    lateinit var mBinding: ActivityControlBinding
    private lateinit var listener : ControlActivityEventListener
    private var isListenerOnBoard = false
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()

        if (configuration.isConnected) {
            listener.startEventListener()
            isListenerOnBoard = true
        }
    }

    override fun onResume()
    {
        super.onResume()
        if (!configuration.isConnected)
            finish()

        mBinding.bottomNavigationView.selectedItemId = R.id.navbar_control
        initSliders()
    }
    override fun onDestroy()
    {
        super.onDestroy()

        if (isListenerOnBoard)
            listener.stopListener()
    }
    private fun initBatteryViews()
    {
        if (configuration.isDoubleDeviceConnection) {
            mBinding.batteryViewLeftDevice.batteryLevel = configuration.doubleDeviceMap[EarSide.Left]?.batteryLevel!!
            mBinding.batteryViewRightDevice.batteryLevel = configuration.doubleDeviceMap[EarSide.Right]?.batteryLevel!!
            mBinding.batteryViewLeftDevice.visibility = View.VISIBLE
            mBinding.batteryViewRightDevice.visibility = View.VISIBLE

        }
        else {
            if (configuration.hearingAid.side == EarSide.Left) {
                mBinding.batteryViewLeftDevice.batteryLevel = configuration.hearingAid.batteryLevel
                mBinding.batteryViewLeftDevice.visibility = View.VISIBLE
            } else {
                mBinding.batteryViewRightDevice.batteryLevel = configuration.hearingAid.batteryLevel
                mBinding.batteryViewRightDevice.visibility = View.VISIBLE
            }
        }
    }
    private fun initBinding()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_control)
        mBinding.viewModel = ControlActivityListenersViewModel(this)
    }
    private fun initialize()
    {
        initBinding()
        initializeNavigationView()
        initMemoryButtons()
        initBatteryViews()
        listener = ControlActivityEventListener(this, eventHandler, threadPool, configuration)
    }
    private fun initMemoryButtons()
    {
        initMemoryButtons(if (!configuration.isDoubleDeviceConnection) configuration.hearingAid
            else configuration.doubleDeviceMap[EarSide.Left]!!)
    }
    private fun initMemoryButtons(hearingAid: HearingAid)
    {
        hearingAid.numberOfMemories.also {

            if (it > 4) {
                initMemoryButtons(4, mBinding.tableLayout.getChildAt(0) as TableRow)
                initMemoryButtons(it - 4, mBinding.tableLayout.getChildAt(1) as TableRow)
            } else
                initMemoryButtons(it, mBinding.tableLayout.getChildAt(0) as TableRow)
        }
    }
    private fun setButtonsDefaultColor(hearingAid: HearingAid, except: Int)
    {
        hearingAid.numberOfMemories.also {

            if (it > 4) {
                setButtonsDefaultColor(4, mBinding.tableLayout.getChildAt(0) as TableRow, except)
                setButtonsDefaultColor(it - 4, mBinding.tableLayout.getChildAt(1) as TableRow, except - 4)
            } else
                setButtonsDefaultColor(it, mBinding.tableLayout.getChildAt(0) as TableRow, except)
        }
    }
    private fun setButtonsDefaultColor(bound: Int, tableRow: TableRow, except: Int)
    {
        (0..<bound).filter { it != except  }.forEach { tableRow.getChildAt(it).apply {
            backgroundTintList = AppCompatResources.getColorStateList(
            this@ControlActivity, R.color.default_button_color)
            foregroundTintList = AppCompatResources.getColorStateList(
                this@ControlActivity, R.color.black)
            }
        }
    }
    private fun initMemoryButtons(bound: Int, tableRow: TableRow)
    {
        runOnUiThread {
            (0..<bound).forEach { tableRow.getChildAt(it).apply {
                    isEnabled = true
                    backgroundTintList = AppCompatResources.getColorStateList(
                        this@ControlActivity, R.color.default_button_color)
                        }
                    }
                }
    }
    private fun initializeNavigationView()
    {
        mBinding.bottomNavigationView.setOnItemSelectedListener {

            when (it.itemId) {
                R.id.navbar_control -> true
                R.id.navbar_connection -> {
                    initializeNavigationView(ConnectionActivity::class.java); finish(); true}
                else ->{
                    initializeNavigationView(AdvancedActivity::class.java); finish(); true }
            }
        }
    }
    private inline fun <reified T : AppCompatActivity> initializeNavigationView(activity: Class<T>)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivity(Intent(this, T::class.java))
            overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
        } else {
            Intent(this, T::class.java).apply {
                val animationBundle = ActivityOptions.makeCustomAnimation(
                    this@ControlActivity,
                    R.anim.slide_in,
                    R.anim.slide_out
                ).toBundle()
                startActivity(this, animationBundle)
            }
        }
    }
    private fun initSliders()
    {
        if (configuration.isConnected) {
            if (!configuration.isDoubleDeviceConnection)
                if (configuration.hearingAid.side == EarSide.Left)
                    mBinding.sliderLeft.value = configuration.hearingAid.volume.toFloat()
                else
                    mBinding.sliderRight.value = configuration.hearingAid.volume.toFloat()
            else {
                mBinding.sliderLeft.value =
                    configuration.doubleDeviceMap[EarSide.Left]?.volume!!.toFloat()
                mBinding.sliderRight.value =
                    configuration.doubleDeviceMap[EarSide.Right]?.volume!!.toFloat()

            }

            if (configuration.isDualDeviceConfig) {
                mBinding.sliderLeft.visibility = View.INVISIBLE
                mBinding.sliderRight.visibility = View.INVISIBLE
                mBinding.sliderCommon.visibility = View.VISIBLE
            }
        }
    }
    fun setSelectedMemoryButtonView(button: Button, num : Int)
    {
        runOnUiThread {
            button.backgroundTintList = AppCompatResources.getColorStateList(
                this,
                com.google.android.material.R.color.design_default_color_secondary_variant
            )
            button.foregroundTintList = AppCompatResources.getColorStateList(
                this@ControlActivity, R.color.white)

            setButtonsDefaultColor(if (!configuration.isDoubleDeviceConnection) configuration.hearingAid
            else configuration.doubleDeviceMap[EarSide.Left]!!, num)
        }
    }
    private fun memoryButtonsClickedCallback(hearingAid: HearingAid, parameterSpace: ParameterSpace)
    {
        hearingAid.wirelessControl?.currentMemory = parameterSpace
    }
    private fun setSlidersFirstAnimationEnd(slider: Slider) : AnimationListener
    {
        return object : AnimationListener {
            override fun onAnimationStart(animation: Animation?)
            {
                slider.isEnabled = false
            }

            override fun onAnimationEnd(animation: Animation?)
            {

                slider.visibility = View.INVISIBLE
                mBinding.textViewLeftSeekBar.visibility = View.INVISIBLE
                mBinding.textViewRightSeekBar.visibility = View.INVISIBLE

                if (mBinding.sliderCommon.visibility != View.VISIBLE) {
                    mBinding.sliderCommon.startAnimation(
                        android.view.animation.AnimationUtils.loadAnimation(
                            this@ControlActivity, R.anim.alpha_blink_anim
                        ).also {it.setAnimationListener(object : AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {
                                mBinding.sliderCommon.visibility = View.VISIBLE
                                mBinding.sliderCommon.isEnabled = true
                            }

                            override fun onAnimationEnd(animation: Animation?) {

                            }

                            override fun onAnimationRepeat(animation: Animation?) {

                            }
                        })

                        })
                }
            }
            override fun onAnimationRepeat(animation: Animation?)
            {

            }
        }
    }
    private fun setSlidersSecondAnimationEnd(slider: Slider) : AnimationListener
    {
        return object : AnimationListener {
            override fun onAnimationStart(animation: Animation?)
            {
                mBinding.sliderCommon.visibility = View.GONE
                mBinding.sliderCommon.isEnabled = false
            }

            override fun onAnimationEnd(animation: Animation?)
            {
                slider.visibility = View.VISIBLE
                slider.isEnabled = true
                mBinding.textViewRightSeekBar.visibility = View.VISIBLE
                mBinding.textViewLeftSeekBar.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(animation: Animation?) {

            }
        }
    }
    fun buttonOneClicked()
    {
        if (configuration.isDoubleDeviceConnection)
            configuration.doubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory0) }
        else
            memoryButtonsClickedCallback(configuration.hearingAid, ParameterSpace.kNvmMemory0)

        setSelectedMemoryButtonView(mBinding.buttonOne, 0)
    }
    fun buttonTwoClicked()
    {
        if (configuration.isDoubleDeviceConnection)
            configuration.doubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory1) }
        else
            memoryButtonsClickedCallback(configuration.hearingAid, ParameterSpace.kNvmMemory1)

        setSelectedMemoryButtonView(mBinding.buttonTwo, 1)
    }
    fun buttonThreeClicked()
    {
        if (configuration.isDoubleDeviceConnection)
            configuration.doubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory2) }
        else
            memoryButtonsClickedCallback(configuration.hearingAid, ParameterSpace.kNvmMemory2)

        setSelectedMemoryButtonView(mBinding.buttonThree, 2)
    }
    fun buttonFourClicked()
    {
        if (configuration.isDoubleDeviceConnection)
            configuration.doubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory3) }
        else
            memoryButtonsClickedCallback(configuration.hearingAid, ParameterSpace.kNvmMemory3)

        setSelectedMemoryButtonView(mBinding.buttonFour, 3)
    }
    fun buttonFiveClicked()
    {
        if (configuration.isDoubleDeviceConnection)
            configuration.doubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory4) }
        else
            memoryButtonsClickedCallback(configuration.hearingAid, ParameterSpace.kNvmMemory4)

        setSelectedMemoryButtonView(mBinding.buttonFive, 4)
    }
    fun buttonSixClicked()
    {
        if (configuration.isDoubleDeviceConnection)
            configuration.doubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory5) }
        else
            memoryButtonsClickedCallback(configuration.hearingAid, ParameterSpace.kNvmMemory5)

        setSelectedMemoryButtonView(mBinding.buttonSix, 5)
    }
    fun buttonSevenClicked()
    {
        if (configuration.isDoubleDeviceConnection)
            configuration.doubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory6) }
        else
            memoryButtonsClickedCallback(configuration.hearingAid, ParameterSpace.kNvmMemory6)

        setSelectedMemoryButtonView(mBinding.buttonSeven, 6)
    }
    fun buttonEightClicked()
    {
        if (configuration.isDoubleDeviceConnection)
            configuration.doubleDeviceMap.forEach { memoryButtonsClickedCallback(it.value, ParameterSpace.kNvmMemory7) }
        else
            memoryButtonsClickedCallback(configuration.hearingAid, ParameterSpace.kNvmMemory7)

        setSelectedMemoryButtonView(mBinding.buttonEight, 7)
    }
    fun sliderLeftEarOnValueChanged(value: Float)
    {
        try {
            if (configuration.isConnected) {
                if (!configuration.isDoubleDeviceConnection) {
                    configuration.hearingAid.wirelessControl?.volume = value.toInt()

                } else
                    configuration.doubleDeviceMap[EarSide.Left]?.wirelessControl?.volume = value.toInt()
            }
        }
        catch (_: ArkException) {}
    }
    fun sliderRightEarOnValueChanged(value: Float)
    {
        try {
            if (configuration.isConnected) {
                if (!configuration.isDoubleDeviceConnection) {
                    configuration.hearingAid.wirelessControl?.volume = value.toInt()

                } else
                    configuration.doubleDeviceMap[EarSide.Right]?.wirelessControl?.volume = value.toInt()
            }
        } catch (_: ArkException) {}
    }
    fun switchOnCheckedChanged()
    {
        if (!configuration.isDualDeviceConfig) {
            runOnUiThread {
                mBinding.sliderLeft.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        R.anim.translate_left_to_right_left_slider
                    ).also { it.setAnimationListener(setSlidersFirstAnimationEnd(mBinding.sliderLeft)) }
                )
                mBinding.sliderRight.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        R.anim.translate_right_to_left_right_slider
                    ).also { it.setAnimationListener(setSlidersFirstAnimationEnd(mBinding.sliderRight)) }
                )

            }
            configuration.isDualDeviceConfig = true
            }
        else {
            runOnUiThread {
                mBinding.sliderLeft.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        R.anim.translate_right_to_left_left_slider
                    ).also { it.setAnimationListener(setSlidersSecondAnimationEnd(mBinding.sliderLeft)) }
                )
                mBinding.sliderRight.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        R.anim.translate_left_to_right_right_slider
                    ).also { it.setAnimationListener(setSlidersSecondAnimationEnd(mBinding.sliderRight)) }
                )

            }
            configuration.isDualDeviceConfig = false
        }
    }
    fun sliderDualOnValueChanged(value: Float)
    {
        threadPool.execute { sliderLeftEarOnValueChanged(value) }
        threadPool.execute { sliderRightEarOnValueChanged(value) }
    }
}