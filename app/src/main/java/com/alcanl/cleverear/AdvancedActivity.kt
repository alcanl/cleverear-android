package com.alcanl.cleverear

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityAdvancedBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.helpers.EarSide
import com.ark.ArkException
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import javax.inject.Inject
@AndroidEntryPoint
class AdvancedActivity : AppCompatActivity(){
    @Inject
    lateinit var configuration : ConfigurationModule
    @Inject
    lateinit var threadPool: ExecutorService
    private lateinit var mBinding : ActivityAdvancedBinding
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
    }

    override fun onResume()
    {
        super.onResume()
        if (!configuration.isConnected)
            finish()

        mBinding.bottomNavigationView.selectedItemId = R.id.navbar_advanced
    }
    private fun initialize()
    {
        initBinding()
        initializeNavigationView()

    }
    private fun initBinding()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_advanced)
    }
    private fun initializeNavigationView()
    {
        mBinding.bottomNavigationView.setOnItemSelectedListener {

            when (it.itemId) {
                R.id.navbar_control -> {
                    initializeNavigationView(ControlActivity::class.java); finish(); true}
                R.id.navbar_connection -> {
                    initializeNavigationView(ConnectionActivity::class.java); finish(); true}
                else -> true
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
                    this@AdvancedActivity,
                    R.anim.slide_in,
                    R.anim.slide_out
                ).toBundle()
                startActivity(this, animationBundle)
            }
        }
    }
    private fun micSliderValueChanged(value: Float, earSide: EarSide)
    {
        try {
            if (configuration.isConnected) {
                if (!configuration.isDoubleDeviceConnection) {
                    configuration.hearingAid.wirelessControl?.micAttenuation = value.toInt()

                } else
                    configuration.doubleDeviceMap[earSide]?.wirelessControl?.micAttenuation = value.toInt()
            }
        } catch (_: ArkException) {}
    }
    private fun auxSliderValueChanged(value: Float, earSide: EarSide)
    {
        try {
            if (configuration.isConnected) {
                if (!configuration.isDoubleDeviceConnection) {
                    configuration.hearingAid.wirelessControl?.auxAttenuation = value.toInt()

                } else
                    configuration.doubleDeviceMap[earSide]?.wirelessControl?.auxAttenuation = value.toInt()
            }
        } catch (_: ArkException) {}
    }
    fun micDualSliderValueChanged(value: Float)
    {
        threadPool.execute { micLeftSliderValueChanged(value) }
        threadPool.execute { micRightSliderValueChanged(value) }
    }
    fun auxDualSliderValueChanged(value: Float)
    {
        threadPool.execute { auxLeftSliderValueChanged(value) }
        threadPool.execute { auxRightSliderValueChanged(value) }
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
    fun modeButtonClicked(view: View)
    {

    }
}