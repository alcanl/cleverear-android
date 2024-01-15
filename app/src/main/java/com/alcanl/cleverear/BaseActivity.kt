package com.alcanl.cleverear

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityBaseBinding
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.helpers.EarSide
import com.ark.EventHandler
import com.ark.ProductManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@AndroidEntryPoint
open class BaseActivity : AppCompatActivity() {
    private lateinit var mBinding : ActivityBaseBinding
    @Inject
    lateinit var productManager: ProductManager
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var threadPool: ExecutorService
    lateinit var mHearingAid: HearingAid
    protected var mDoubleDeviceMap = HashMap<EarSide,HearingAid>()
    protected lateinit var mEarSide : EarSide
    private var mIsFirstLaunch = true

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        if (mIsFirstLaunch) {
            initialize()
            mIsFirstLaunch = false
        }
    }
    override fun onStart()
    {
        super.onStart()
        initCountDownTimer()
    }
    private fun initCountDownTimer()
    {
        object: CountDownTimer(6000, 1000) {
            override fun onTick(millisUntilFinished: Long)
            {
                if (millisUntilFinished == 3000L)
                    mBinding.textViewConnecting.text = getString(R.string.textview_device_detection_connection_in_process_text)

                mBinding.textViewConnecting.text = String.format("${mBinding.textViewConnecting.text}" + ".")
            }
            override fun onFinish()
            {
                mBinding.textViewConnecting.text = getString(R.string.textview_device_detection_device_connected_text)
                Handler(Looper.myLooper()!!).postDelayed( {finish()}, 1000)
            }
        }.start()
    }
    private fun initialize()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_base)
    }

}