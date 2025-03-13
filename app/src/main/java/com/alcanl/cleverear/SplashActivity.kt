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
import com.alcanl.cleverear.util.BUNDLE_INTENT_FROM_SPLASH
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.determineHearingAidObjects
import com.alcanl.cleverear.util.dialog.showDeviceConnectedBeforeDialog
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

private const val FOREGROUND_PERMISSION_REQUEST_CODE = 23
private const val POST_NOTIFICATION_PERMISSION_REQUEST_CODE = 22
private const val DELAY_TWO_SECONDS = 2000L
private const val DELAY_FOUR_SECONDS = 4000L
private const val DELAY_FIVE_SECONDS = 5000L

@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity(), IEventListener {

    @Inject
    lateinit var configuration: ConfigurationModule

    @Inject
    lateinit var listener: SplashActivityEventListener

    @Inject
    lateinit var handler : Handler

    private lateinit var mAddressArray : Array<String>

    private var mIsUpdatedVersion = true

    private val mAppUpdateManager by lazy { AppUpdateManagerFactory.create(applicationContext) }

    private val mUpdateAppResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
        if (result.resultCode != RESULT_OK) {
            Toast.makeText(this, R.string.toast_update_need_text, Toast.LENGTH_LONG).show()
            Handler(Looper.myLooper()!!).postDelayed({ finish() }, DELAY_TWO_SECONDS)
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

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        EventBus.getDefault().register(this)
        initialize()

        lifecycleScope.launch(Dispatchers.IO) {
            async {
                mIsUpdatedVersion = if (!isInternetAvailable(this@SplashActivity)) true
                else try {
                    withTimeout(DELAY_FIVE_SECONDS) {
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
                    handler.postDelayed(::finalizeSplash, DELAY_FOUR_SECONDS)
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
        if (requestCode == POST_NOTIFICATION_PERMISSION_REQUEST_CODE)
            if (grantResults.isNotEmpty())
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && mForeGroundServicePermission) {
                    cleanEvents()
                    handler.postDelayed(::finalizeSplash, DELAY_FOUR_SECONDS)
                }
                else
                    onPermissionResultNegative()

        if (requestCode == FOREGROUND_PERMISSION_REQUEST_CODE)
            if (grantResults.isNotEmpty())
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && mPostNotificationPermission) {
                    cleanEvents()
                    handler.postDelayed(::finalizeSplash, DELAY_FOUR_SECONDS)
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
        handler.postDelayed({ killProcess(myPid()) }, DELAY_TWO_SECONDS)
    }

    @SuppressLint("InlinedApi")
    private fun checkAndRequestNotificationPermission()
    {
        if (!mPostNotificationPermission)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                POST_NOTIFICATION_PERMISSION_REQUEST_CODE
            )

        if (!mForeGroundServicePermission)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE),
                FOREGROUND_PERMISSION_REQUEST_CODE)
    }

    private fun handleConnectedBefore()
    {
        val adapter = getSystemService(BluetoothManager::class.java).adapter
        configuration.isAshaConnected = if (mAddressArray.isEmpty())
                                            false
                                        else mAddressArray.all { isConnected(adapter.getRemoteDevice(it)) }
    }

    private fun finalizeSplash()
    {
        handleConnectedBefore()

        configuration.apply {
            if (isConnected) {
                val bundle = Bundle()
                bundle.putBoolean(BUNDLE_INTENT_FROM_SPLASH, true)
                Intent(this@SplashActivity, MainContainerActivity::class.java)
                    .apply { putExtras(bundle); setResult(RESULT_OK); startActivity(this); finish() }
                return
            }
            else if (!isServiceOnBoard && isConnectedBefore)
            {
                if (isAshaConnected) {
                    determineHearingAidObjects(this, this@SplashActivity)
                    startActivity(Intent(this@SplashActivity, LoadingActivity::class.java))
                    finish()
                }
                else
                    showDeviceConnectedBeforeDialog(this@SplashActivity,
                        ::alertDialogPositiveButtonClicked,
                        ::alertDialogNegativeButtonClicked)
            }
            else {
                Intent(this@SplashActivity, MainActivity::class.java)
                    .apply { startActivity(this) }; finish()
            }
        }
    }

    private fun checkPrefs()
    {
        val addressLeft = getLeftHearingAidAddress(this)
        val addressRight = getRightHearingAidAddress(this)

        when {
            addressLeft.isNotEmpty() && addressRight.isNotEmpty() ->
                configuration.apply {
                    isDoubleDeviceConnection = true
                    isConnectedBefore = true
                    mAddressArray = arrayOf(addressLeft, addressRight)
                }

            addressLeft.isNotEmpty() ->
                configuration.apply {
                    earSide = EarSide.Left
                    isConnectedBefore = true
                    mAddressArray = arrayOf(addressLeft)
                }

            addressRight.isNotEmpty() ->
                configuration.apply {
                    earSide = EarSide.Right
                    isConnectedBefore = true
                    mAddressArray = arrayOf(addressRight)
                }

            else -> mAddressArray = emptyArray()
        }
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
            delay(DELAY_TWO_SECONDS)
            listener.stopListener()

        } catch (_: InterruptedException) {}
    }

    private fun alertDialogPositiveButtonClicked()
    {
        val bundle = Bundle()
        bundle.putBoolean(BUNDLE_INTENT_FROM_SPLASH, true)
        Intent(this, AutoConnectActivity::class.java).apply {
            putExtras(bundle)
            setResult(RESULT_OK, this)
            startActivity(this)
        }
        finish()
    }

    private fun alertDialogNegativeButtonClicked()
    {
        configuration.isConnectedBefore = false
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}