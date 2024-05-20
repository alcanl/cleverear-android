package com.alcanl.cleverear

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivitySplashBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.HA_LEFT_ADDRESS
import com.alcanl.cleverear.util.HA_RIGHT_ADDRESS
import com.alcanl.cleverear.util.SHARED_PREF_DEVICE
import com.alcanl.cleverear.util.dialog.CustomAlertDialogConnectedBefore
import com.alcanl.cleverear.util.isConnected
import com.alcanl.cleverear.util.listeners.SplashActivityEventListener
import com.ark.EventHandler
import dagger.hilt.android.AndroidEntryPoint
import java.util.EnumMap
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

    private lateinit var mAlertDialog: CustomAlertDialogConnectedBefore
    private lateinit var mListener: SplashActivityEventListener
    private lateinit var mAddressArray : Array<String>
    private var isAshaConnected = false
    private fun initCountDownTimer()
    {
        object: CountDownTimer(4000, 1000) {
            private fun handleConnectedBefore()
            {
                val adapter = getSystemService(BluetoothManager::class.java).adapter
                if (mAddressArray.all { isConnected(adapter.getRemoteDevice(it)) })
                    isAshaConnected = true

            }
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish()
            {
                handleConnectedBefore()

                if (configuration.isConnected) {
                    Intent(this@SplashActivity, ControlActivity::class.java).apply { startActivity(this); finish() }
                    return
                }
                else if (!configuration.isServiceOnBoard && configuration.isConnectedBefore)
                {
                    if (isAshaConnected) {
                        startActivity(Intent(this@SplashActivity, LoadingActivity::class.java))
                        finish()
                    }
                    else
                        mAlertDialog.show()
                }
                else {
                    Intent(
                        this@SplashActivity,
                        MainActivity::class.java
                    ).apply { startActivity(this) }
                    finish()
                }

            }
        }.start()
    }
    private fun checkPrefs()
    {
        val prefs = getSharedPreferences(SHARED_PREF_DEVICE, MODE_PRIVATE)
        val addressLeft = prefs.getString(HA_LEFT_ADDRESS, "")
        val addressRight = prefs.getString(HA_RIGHT_ADDRESS, "")

        if (addressLeft?.isNotEmpty()!! && addressRight?.isNotEmpty()!!)
            configuration.apply {
                isDoubleDeviceConnection = true
                isConnectedBefore = true
                mAddressArray = arrayOf(addressLeft, addressRight)
                doubleDeviceMap = EnumMap(EarSide::class.java)
            }

        else if (addressLeft.isNotEmpty())
            configuration.apply {
                earSide = EarSide.Left
                isConnectedBefore = true
                mAddressArray = arrayOf(addressLeft)
            }

        else if (addressRight?.isNotEmpty()!!)
            configuration.apply {
                earSide = EarSide.Right
                isConnectedBefore = true
                mAddressArray = arrayOf(addressRight)
            }

    }

    private fun initialize()
    {
        checkAndRequestNotificationPermission()
        DataBindingUtil.setContentView<ActivitySplashBinding>(this, R.layout.activity_splash)
        checkPrefs()
        mListener = SplashActivityEventListener(eventHandler, threadPool, configuration, this)
        mAlertDialog = CustomAlertDialogConnectedBefore(this)
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        initCountDownTimer()
        cleanEvents()
    }

    private fun checkAndRequestNotificationPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE) != PackageManager.PERMISSION_GRANTED)
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
            Thread.sleep(2000)
            mListener.stopListener()
        } catch (_: InterruptedException) {}
    }
    fun alertDialogPositiveButtonClicked()
    {
        mAlertDialog.dismiss()
        startActivity(Intent(this, AutoConnectActivity::class.java))
        finish()
    }
    fun alertDialogNegativeButtonClicked()
    {
        configuration.isConnectedBefore = false
        mAlertDialog.dismiss()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}