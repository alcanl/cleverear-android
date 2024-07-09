package com.alcanl.cleverear

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.Animation
import android.widget.ImageButton
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityAutoConnectBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.HA_LEFT_NAME
import com.alcanl.cleverear.util.HA_RIGHT_NAME
import com.alcanl.cleverear.util.SHARED_PREF_DEVICE
import com.alcanl.cleverear.viewmodel.AutoConnectActivityListenersViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AutoConnectActivity : AppCompatActivity() {
    @Inject
    lateinit var configuration : ConfigurationModule
    private var isLeftSelected = false
    private var isRightSelected = false
    private lateinit var mBinding : ActivityAutoConnectBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize()
    }
    private fun initialize()
    {
        initBinding()
    }
    private fun initBinding()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_auto_connect)
        mBinding.viewModel = AutoConnectActivityListenersViewModel(this)
    }
    fun getLeftEarModelNumber() : String
    {
        return getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE).getString(HA_LEFT_NAME, "")!!
    }
    fun getRightEarModelNumber() : String
    {
        return getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE).getString(HA_RIGHT_NAME, "")!!
    }
    fun leftEarButtonClicked()
    {
        mBinding.imageButtonLeft.apply {
            startAnimation(android.view.animation.AnimationUtils.loadAnimation(this@AutoConnectActivity,
                R.anim.alpha_blink_anim).also { it.setAnimationListener(imageButtonAnimation(this, EarSide.Left)) })
        }
    }
    fun rightEarButtonClicked()
    {
        mBinding.imageButtonRight.apply {
            startAnimation(android.view.animation.AnimationUtils.loadAnimation(this@AutoConnectActivity,
                R.anim.alpha_blink_anim).also { it.setAnimationListener(imageButtonAnimation(this, EarSide.Right)) })
        }
    }
    fun controlButtonClicked()
    {

        mBinding.imageButtonControl.apply {
            startAnimation(android.view.animation.AnimationUtils.loadAnimation(this@AutoConnectActivity,
                R.anim.alpha_blink_anim).also { it.setAnimationListener(controlButtonAnimation()) })
        }
    }
    fun remoteFittingButtonClicked()
    {
        mBinding.imageButtonRemoteConf.apply {
            startAnimation(android.view.animation.AnimationUtils.loadAnimation(this@AutoConnectActivity,
                R.anim.alpha_blink_anim).also { it.setAnimationListener(remoteFittingButtonAnimation()) })
        }
    }
    private fun controlButtonClickedCommon()
    {
        configuration.isConnectedBefore = true
        startActivity(Intent(this@AutoConnectActivity, LoadingActivity::class.java))
        finish()
    }
    private fun remoteFittingButtonClickedCommon()
    {
        configuration.isRemoteFittingConnection = true
        controlButtonClickedCommon()
    }
    private fun isValidDeviceData(isDual: Boolean, text: String) : Boolean
    {
        return if (isDual)
            mBinding.textViewLeft.text != getString(R.string.textview_no_previous_device_info)
                    && mBinding.textViewRight.text != getString(R.string.textview_no_previous_device_info)
        else
            text != getString(R.string.textview_no_previous_device_info)

    }
    private fun toastInvalidDeviceData()
    {
        Toast.makeText(this, R.string.toast_no_previous_device_for_selection, Toast.LENGTH_LONG).show()
    }
    private fun imageButtonAnimation(button: ImageButton, side: EarSide) : Animation.AnimationListener
    {
        return object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?)
            {
                if (side == EarSide.Left) {
                    isLeftSelected = !isLeftSelected
                    button.setImageResource(if (isLeftSelected) R.drawable.leftear_blue else R.drawable.lefteargray)
                }
                else {
                    isRightSelected = !isRightSelected
                    button.setImageResource(if (isRightSelected) R.drawable.rightear_red else R.drawable.righteargray)
                }
            }

            override fun onAnimationEnd(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
        }
    }
    private fun controlButtonAnimation() : Animation.AnimationListener
    {
        return object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?)
            {
                with(configuration) {

                    if (isLeftSelected && isRightSelected)
                        if (isValidDeviceData(true, "")) {
                            isDoubleDeviceConnection = true
                            controlButtonClickedCommon()
                        }
                        else
                            toastInvalidDeviceData()
                    else if (isLeftSelected)
                        if (isValidDeviceData(false, mBinding.textViewLeft.text.toString())){
                            isDoubleDeviceConnection = false
                            earSide = EarSide.Left
                            controlButtonClickedCommon()
                        }
                        else
                            toastInvalidDeviceData()
                    else if (isRightSelected)
                        if (isValidDeviceData(false, mBinding.textViewRight.text.toString())){
                            isDoubleDeviceConnection = false
                            earSide = EarSide.Right
                            controlButtonClickedCommon()
                        }
                        else
                            toastInvalidDeviceData()

                    else
                        Toast.makeText(this@AutoConnectActivity, R.string.toast_no_device_selected, Toast.LENGTH_LONG).show()
                }
            }
            override fun onAnimationEnd(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
        }
    }
    private fun remoteFittingButtonAnimation() : Animation.AnimationListener
    {
        return object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?)
            {
                with(configuration) {

                    if (isLeftSelected && isRightSelected)
                        if (isValidDeviceData(true, "")) {
                            isDoubleDeviceConnection = true
                            remoteFittingButtonClickedCommon()
                        }
                        else
                            toastInvalidDeviceData()
                    else if (isLeftSelected)
                        if (isValidDeviceData(false, mBinding.textViewLeft.text.toString())){
                            isDoubleDeviceConnection = false
                            earSide = EarSide.Left
                            remoteFittingButtonClickedCommon()
                        }
                        else
                            toastInvalidDeviceData()
                    else if (isRightSelected)
                        if (isValidDeviceData(false, mBinding.textViewRight.text.toString())){
                            isDoubleDeviceConnection = false
                            earSide = EarSide.Right
                            remoteFittingButtonClickedCommon()
                        }
                        else
                            toastInvalidDeviceData()

                    else
                        Toast.makeText(this@AutoConnectActivity, R.string.toast_no_device_selected, Toast.LENGTH_LONG).show()
                }
            }
            override fun onAnimationEnd(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
        }
    }
}