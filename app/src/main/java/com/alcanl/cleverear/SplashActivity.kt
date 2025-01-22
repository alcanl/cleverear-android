package com.alcanl.cleverear

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.alcanl.cleverear.databinding.ActivitySplashBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.fitting.event.listener.IEventListener
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.HA_LEFT_ADDRESS
import com.alcanl.cleverear.util.HA_RIGHT_ADDRESS
import com.alcanl.cleverear.util.SHARED_PREF_DEVICE
import com.alcanl.cleverear.util.dialog.CustomAlertDialogConnectedBefore
import com.alcanl.cleverear.util.isConnected
import com.alcanl.cleverear.util.listeners.SplashActivityEventListener
import com.alcanl.cleverear.util.listeners.event.BatteryEvent
import com.ark.EventHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.EventObject
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity(), IEventListener {

    @Inject
    lateinit var configuration: ConfigurationModule

    @Inject
    lateinit var eventHandler: EventHandler

    @Inject
    lateinit var listener: SplashActivityEventListener

    private var isAshaConnected = false

    private lateinit var mAddressArray : Array<String>

    private val mPostNotificationPermission by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        else true
        }

    private val mForeGroundServicePermission by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE
            ) == PackageManager.PERMISSION_GRANTED
        else true
    }

    private val mAlertDialog: CustomAlertDialogConnectedBefore by lazy {
        CustomAlertDialogConnectedBefore(this)
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        initialize()

        if (mForeGroundServicePermission && mPostNotificationPermission) {
            initCountDownTimer()
            cleanEvents()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 22)
            if (grantResults.isNotEmpty())
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && mForeGroundServicePermission)
                    initCountDownTimer()
                else
                    Toast.makeText(this, getString(R.string.toast_no_permission_splash_activity), Toast.LENGTH_LONG).show()

        if (requestCode == 23)
            if (grantResults.isNotEmpty())
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && mPostNotificationPermission)
                    initCountDownTimer()
                else
                    Toast.makeText(this, getString(R.string.toast_no_permission_splash_activity), Toast.LENGTH_LONG).show()

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onEventReceive(event: EventObject)
    {
        if (event is BatteryEvent)
            configuration.apply {
                if (isDoubleDeviceConnection)
                    if (event.eventSender == hearingAidMap[EarSide.Left]?.address)
                        hearingAidMap[EarSide.Left]?.batteryLevel = event.batteryData
                    else
                        hearingAidMap[EarSide.Right]?.batteryLevel = event.batteryData
                else
                    hearingAidMap[earSide]?.batteryLevel = event.batteryData
            }
    }

    @SuppressLint("InlinedApi")
    private fun checkAndRequestNotificationPermission()
    {
        if (!mPostNotificationPermission)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                22
            )
        if (!mForeGroundServicePermission)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE),
                23)

    }

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
                    Intent(this@SplashActivity, MainContainerActivity::class.java).apply { startActivity(this); finish() }
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
        else
            mAddressArray = emptyArray()
    }

    private fun initialize()
    {
        DataBindingUtil.setContentView<ActivitySplashBinding>(this, R.layout.activity_splash)
        checkAndRequestNotificationPermission()
        checkPrefs()
    }

    private fun cleanEvents()
    {
        lifecycleScope.launch(Dispatchers.Default) { cleanEventsCallback() }
    }

    private suspend fun cleanEventsCallback()
    {
        try {
            listener.startEventListener()
            delay(2000)
            listener.stopListener()

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