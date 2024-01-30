package com.alcanl.cleverear

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityMainBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.viewmodel.MainActivityListenersViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var configuration: ConfigurationModule
    private lateinit var mBinding: ActivityMainBinding
    private val bluetoothActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK)
            bluetoothScan()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()
        scanBT()
    }
    private fun bluetoothScan()
    {
        Toast.makeText(this, R.string.toast_bt_on, Toast.LENGTH_LONG).show()
    }
    override fun onResume() {
        super.onResume()
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
        Intent(this, EarSideSelectionActivity::class.java).apply { startActivity(this) }
    }

    fun buttonDoubleDeviceClick() {
        configuration.isDoubleDeviceConnection = true
        configuration.earSide = EarSide.Left
        Intent(this, ConnectionActivity::class.java).apply { startActivity(this) }
    }

    private fun initBinding() {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mBinding.viewModel = MainActivityListenersViewModel(this)
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissionForQ() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN
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
                    Manifest.permission.BLUETOOTH_ADMIN
                ),
                1
            )
    }
    private fun scanBT()
    {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.toast_bluetooth_not_found_text, Toast.LENGTH_LONG).show()
        }
        else {
            if (!bluetoothAdapter.isEnabled) {
                val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothActivityResultLauncher.launch(enableBluetoothIntent)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                checkBluetoothConnectAndScanPermission()
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                checkPermissionForQ()
            else
                checkPermissionForN()
        }
    }
    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkBluetoothConnectAndScanPermission()
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
}