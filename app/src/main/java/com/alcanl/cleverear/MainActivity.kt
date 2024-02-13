package com.alcanl.cleverear

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.helpers.service.AutoConnectJobService
import com.alcanl.cleverear.viewmodel.MainActivityListenersViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var configuration: ConfigurationModule
    private lateinit var mBinding: ActivityMainBinding
    private var mBluetoothPermission = false
    private val gpsActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        result: ActivityResult ->
        if (result.resultCode == RESULT_OK)
            gpsScan()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()


        if (!mBluetoothPermission) {
            scanBluetooth()
            checkBluetoothPermission()
        }

    }
    override fun onResume() {
        super.onResume()
        checkBluetoothPermission()
        mBinding.buttonSingleDevice.startAnimation(
            android.view.animation.AnimationUtils.loadAnimation
                (this, R.anim.translate_button_left_to_right)
        )
        mBinding.buttonDoubleDevice.startAnimation(
            android.view.animation.AnimationUtils.loadAnimation
                (this, R.anim.translate_button_right_to_left)
        )
    }
    fun buttonSingleDeviceClick() {

        if (mBluetoothPermission) {
            Intent(this, EarSideSelectionActivity::class.java).apply { startActivity(this) }
            configuration.isDoubleDeviceConnection = false
        }
        else
            scanBluetooth()
    }

    fun buttonDoubleDeviceClick()
    {
        if (mBluetoothPermission) {
            configuration.isDoubleDeviceConnection = true
            configuration.earSide = EarSide.Left
            Intent(this, ConnectionActivity::class.java).apply { startActivity(this) }
        }
        else
            scanBluetooth()
    }

    private fun initBinding() {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mBinding.viewModel = MainActivityListenersViewModel(this)
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissionForQ() {
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
    private fun checkPermissionForN() {
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
    private fun scanBluetooth()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            checkPermissionForS()
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scanGPS()
            checkPermissionForQ()
        }
        else {
            scanGPS()
            checkPermissionForN()
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
    private fun checkPermissionForS()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
        )
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                1
            )
    }
    private fun gpsScan()
    {
        Toast.makeText(this, R.string.toast_gps_on, Toast.LENGTH_LONG).show()
    }
    private fun checkBluetoothPermission()
    {
        mBluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
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

    private fun startScheduler()
    {
        val scheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler

        scheduler.schedule(
            JobInfo.Builder(1, ComponentName(this, AutoConnectJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(60000)
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(false)
            .build())

    }
}