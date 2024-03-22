package com.alcanl.cleverear

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivitySplashBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.util.listeners.SplashActivityEventListener
import com.ark.EventHandler
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import javax.inject.Inject
@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    @Inject
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var eventHandler: EventHandler
    @Inject
    lateinit var threadPool: ExecutorService
    private lateinit var mListener: SplashActivityEventListener

    private fun initCountDownTimer()
    {
        object: CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish()
            {
                if (configuration.isConnected) {
                    Intent(this@SplashActivity, ControlActivity::class.java).apply { startActivity(this); finish() }
                    return
                }

                Intent(this@SplashActivity, MainActivity::class.java).apply { startActivity(this) }
                finish()
            }
        }.start()
    }
    private fun initialize()
    {
        requestNotificationPermission()
        DataBindingUtil.setContentView<ActivitySplashBinding>(this, R.layout.activity_splash)
        mListener = SplashActivityEventListener(eventHandler, threadPool)
    }
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        initCountDownTimer()
        cleanEvents()
    }
    private fun requestNotificationPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            if (checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE), 0)

    }
    private fun cleanEvents()
    {
        threadPool.execute { cleanEventsCallback() }
    }
    @Synchronized
    private fun cleanEventsCallback()
    {
        try {
            mListener.startEventListener()
            Thread.sleep(1500)
            mListener.stopListener()
        } catch (_: InterruptedException) {}
    }
}