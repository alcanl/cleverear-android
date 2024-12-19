package com.alcanl.cleverear

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityMainBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.util.ENGLISH_MANUAL_URL
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.GERMAN
import com.alcanl.cleverear.util.GERMAN_MANUAL_URL
import com.alcanl.cleverear.util.TURKISH
import com.alcanl.cleverear.util.TURKISH_MANUAL_URL
import com.alcanl.cleverear.util.getSystemLanguage
import com.alcanl.cleverear.util.service.broadcast.BluetoothStateBroadcastReceiver
import com.alcanl.cleverear.viewmodel.MainActivityListenersViewModel
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var enableBluetoothBroadCast : BluetoothStateBroadcastReceiver
    private lateinit var mBinding: ActivityMainBinding
    private var mIsBluetoothPermissionsGranted = false
    private var mIsWebViewActive = false
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }

    private val gpsActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        result: ActivityResult ->
        if (result.resultCode == RESULT_OK)
            gpsTurnedOn()
    }
    private val bluetoothActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK)
            bluetoothTurnedOn()
    }
    private val updateAppResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
        if (result.resultCode != RESULT_OK) {
            Toast.makeText(this, R.string.toast_update_need_text, Toast.LENGTH_LONG).show()
            Handler(Looper.myLooper()!!).postDelayed({ finish() },2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize()
        checkBluetoothPermission()

        if (!mIsBluetoothPermissionsGranted) {
            scanBluetoothPermissions()
            checkBluetoothPermission()
        }

        checkBluetoothEnabled()
        checkForUpdates()
    }
    override fun onResume() {
        super.onResume()
        checkBluetoothPermission()

        if (mIsWebViewActive)
            mBinding.frameLayoutWebView.visibility = View.GONE

        mBinding.buttonSingleDevice.startAnimation(
            android.view.animation.AnimationUtils.loadAnimation
                (this, R.anim.translate_button_left_to_right)
        )
        mBinding.buttonDoubleDevice.startAnimation(
            android.view.animation.AnimationUtils.loadAnimation
                (this, R.anim.translate_button_right_to_left)
        )

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateAppResultLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()) } }

    }
    override fun onDestroy()
    {
        super.onDestroy()
        unregisterReceiver(enableBluetoothBroadCast)
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mBinding.webView.canGoBack()) {
            mBinding.webView.goBack()
            mBinding.frameLayoutWebView.visibility = View.GONE
            return true
        }

        return super.onKeyDown(keyCode, event)
    }
    private fun bluetoothTurnedOn()
    {
        Toast.makeText(this, R.string.toast_bt_on, Toast.LENGTH_LONG).show()
        configuration.isBluetoothEnabled = true
    }
    private fun initialize()
    {
        initBinding()
        registerReceiver(enableBluetoothBroadCast, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }
    private fun initBinding() {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mBinding.viewModel = MainActivityListenersViewModel(this)
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkAndRequestPermissionForQ() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) !=
            PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH
                ), 1
            )
        }
    }
    private fun checkAndRequestPermissionForN() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) !=
            PackageManager.PERMISSION_GRANTED
        )
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH
                ),
                1
            )
    }
    private fun scanBluetoothPermissions()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            checkAndRequestPermissionForS()
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scanGPS()
            checkAndRequestPermissionForQ()
        }
        else {
            scanGPS()
            checkAndRequestPermissionForN()
        }
    }
    private fun scanGPS()
    {
        val locationManager : LocationManager = getSystemService(LocationManager::class.java)
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { gpsActivityResultLauncher.launch(this) }
        }
    }
    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkAndRequestPermissionForS()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
        )
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ),
                1
            )
    }
    private fun gpsTurnedOn()
    {
        Toast.makeText(this, R.string.toast_gps_on, Toast.LENGTH_LONG).show()
    }
    private fun checkBluetoothPermission()
    {
        mIsBluetoothPermissionsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        else
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun toastNoPermission()
    {
        Toast.makeText(this, R.string.toast_no_permission, Toast.LENGTH_LONG).show()
    }
    private fun controlButtonAnimation() : AnimationListener
    {
        return object : AnimationListener {
            override fun onAnimationStart(animation: Animation?)
            {
                if (mIsBluetoothPermissionsGranted) {
                    configuration.isDoubleDeviceConnection = false
                    Intent(this@MainActivity, AutoConnectActivity::class.java).apply { startActivity(this) }
                }
                else {
                    toastNoPermission()
                    scanBluetoothPermissions()
                }
            }
            override fun onAnimationEnd(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
        }
    }
    private fun infoButtonAnimation() : AnimationListener
    {
        return object : AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                val language = getSystemLanguage()

                with(mBinding) {

                    when (language.lowercase()) {
                        TURKISH -> webView.loadUrl(TURKISH_MANUAL_URL)
                        GERMAN -> webView.loadUrl(GERMAN_MANUAL_URL)
                        else -> webView.loadUrl(ENGLISH_MANUAL_URL)
                    }

                    frameLayoutWebView.visibility = View.VISIBLE
                    mIsWebViewActive = true
                }
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        }
    }
    private fun checkBluetoothEnabled()
    {
        configuration.isBluetoothEnabled = getSystemService(BluetoothManager::class.java).adapter.isEnabled
    }
    private fun searchBluetooth()
    {
        val bluetoothAdapter = getSystemService(BluetoothManager::class.java).adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.toast_bluetooth_not_found_text, Toast.LENGTH_LONG).show()
            finish()
        }
        else if (!bluetoothAdapter.isEnabled) {
            checkBluetoothPermission()
            if (mIsBluetoothPermissionsGranted) {
                val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothActivityResultLauncher.launch(enableBluetoothIntent)
            }
            else
                Toast.makeText(this, R.string.toast_no_permission, Toast.LENGTH_LONG).show()
        }
            else
                configuration.isBluetoothEnabled = true
    }
    private fun checkForUpdates()
    {

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateAppResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
            }
        }
    }
    fun buttonSingleDeviceClick() {

        if (!configuration.isBluetoothEnabled) {
            searchBluetooth()
            return
        }

        if (mIsBluetoothPermissionsGranted) {
            Intent(this, EarSideSelectionActivity::class.java).apply { startActivity(this) }
            configuration.isDoubleDeviceConnection = false
        }
        else {
            toastNoPermission()
            scanBluetoothPermissions()
        }
    }
    fun buttonDoubleDeviceClick()
    {
        if (!configuration.isBluetoothEnabled) {
            searchBluetooth()
            return
        }

        if (mIsBluetoothPermissionsGranted) {
            configuration.apply {
                isDoubleDeviceConnection = true
                earSide = EarSide.Left
            }
            Intent(this, MainContainerActivity::class.java).apply { startActivity(this) }
        }
        else {
            toastNoPermission()
            scanBluetoothPermissions()
        }
    }
    fun buttonControlClicked()
    {
        if (!configuration.isBluetoothEnabled) {
            searchBluetooth()
            return
        }

        mBinding.imageButtonControl.apply {
            startAnimation(
                AnimationUtils.loadAnimation(this@MainActivity,
                    R.anim.alpha_blink_anim).also { it.setAnimationListener(controlButtonAnimation()) })
        }
    }
    fun buttonInfoClicked()
    {
        mBinding.imageButtonInfo.apply {
            startAnimation(AnimationUtils.loadAnimation(this@MainActivity, R.anim.alpha_blink_anim)
                .also { it.setAnimationListener(infoButtonAnimation()) })
        }
    }
}