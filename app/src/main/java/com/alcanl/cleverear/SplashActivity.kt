package com.alcanl.cleverear

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process.killProcess
import android.os.Process.myPid
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.alcanl.cleverear.databinding.ActivitySplashBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.fitting.event.listener.IEventListener
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.dialog.CustomAlertDialogConnectedBefore
import com.alcanl.cleverear.util.getLeftHearingAidAddress
import com.alcanl.cleverear.util.getRightHearingAidAddress
import com.alcanl.cleverear.util.isConnected
import com.alcanl.cleverear.util.isInternetAvailable
import com.alcanl.cleverear.util.listeners.SplashActivityEventListener
import com.alcanl.cleverear.util.listeners.event.BatteryEvent
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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
    lateinit var listener: SplashActivityEventListener

    @Inject
    lateinit var handler : Handler

    private var mIsAshaConnected = false

    private lateinit var mAddressArray : Array<String>

    private var mIsUpdatedVersion = true

    private val mAppUpdateManager by lazy { AppUpdateManagerFactory.create(applicationContext) }

    private val mUpdateAppResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
        if (result.resultCode != RESULT_OK) {
            Toast.makeText(this, R.string.toast_update_need_text, Toast.LENGTH_LONG).show()
            Handler(Looper.myLooper()!!).postDelayed({ finish() },2000)
        }
        else
            mIsUpdatedVersion = true
    }

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

        lifecycleScope.launch(Dispatchers.IO) {
            async {
                mIsUpdatedVersion = if (!isInternetAvailable(this@SplashActivity)) true
                else try {
                    withTimeout(5000) {
                        mAppUpdateManager.requestAppUpdateInfo()
                            .updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE
                    }
                } catch (ex: Exception) {
                    Log.e(SplashActivity::class.simpleName, ex.message!!)
                    true
                }
            }.await()

            launch(Dispatchers.Main) {
                if (!mIsUpdatedVersion)
                    doUpdate()
                else if (!mForeGroundServicePermission || !mPostNotificationPermission)
                    checkAndRequestNotificationPermission()
                else {
                    cleanEvents()
                    handler.postDelayed(::finalizeSplash, 4000)
                }
            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        mAppUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                mAppUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    mUpdateAppResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()) } }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 22)
            if (grantResults.isNotEmpty())
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && mForeGroundServicePermission) {
                    cleanEvents()
                    handler.postDelayed(::finalizeSplash, 4000)
                }
                else
                    onPermissionResultNegative()

        if (requestCode == 23)
            if (grantResults.isNotEmpty())
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && mPostNotificationPermission) {
                    cleanEvents()
                    handler.postDelayed(::finalizeSplash, 4000)
                }
                else
                    onPermissionResultNegative()

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

    private fun onPermissionResultNegative()
    {
        Toast.makeText(this, getString(R.string.toast_no_permission_splash_activity), Toast.LENGTH_LONG).show()
        handler.postDelayed({ killProcess(myPid()) }, 2000)
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

    private fun handleConnectedBefore()
    {
        val adapter = getSystemService(BluetoothManager::class.java).adapter
        if (mAddressArray.all { isConnected(adapter.getRemoteDevice(it)) })
            mIsAshaConnected = true
    }

    private fun finalizeSplash()
    {
        handleConnectedBefore()

        if (configuration.isConnected) {
            Intent(this@SplashActivity, MainContainerActivity::class.java)
                .apply { startActivity(this); finish() }
            return
        }
        else if (!configuration.isServiceOnBoard && configuration.isConnectedBefore)
        {
            if (mIsAshaConnected) {
                startActivity(Intent(this@SplashActivity, LoadingActivity::class.java))
                finish()
            }
            else
                mAlertDialog.show()
        }
        else {
            Intent(this@SplashActivity, MainActivity::class.java)
                .apply { startActivity(this) }; finish()
        }
    }
    private fun checkPrefs()
    {
        val addressLeft = getLeftHearingAidAddress(this)
        val addressRight = getRightHearingAidAddress(this)

        if (addressLeft.isNotEmpty() && addressRight.isNotEmpty())
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

        else if (addressRight.isNotEmpty())
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
        checkPrefs()
    }

    private fun cleanEvents()
    {
        lifecycleScope.launch(Dispatchers.Default) { cleanEventsCallback() }
    }

    private fun doUpdate()
    {
        mAppUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                mAppUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    mUpdateAppResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
            }
        }
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