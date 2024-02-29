package com.alcanl.cleverear

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE
import android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivitySplashBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.helpers.listeners.SplashActivityEventListener
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
    private lateinit var mBinding : ActivitySplashBinding
    private lateinit var mListener: SplashActivityEventListener
    private val bluetoothActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK)
            bluetoothScan()
    }
    private lateinit var mEnableBluetoothBroadCast : BroadcastReceiver
    private fun bluetoothScan()
    {
        Toast.makeText(this, R.string.toast_bt_on, Toast.LENGTH_LONG).show()
    }
    private fun initCountDownTimer()
    {
        object: CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long)
            {

            }
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
        initBinding()
        mEnableBluetoothBroadCast = handleBroadcastReceiver()
        registerReceiver(mEnableBluetoothBroadCast, IntentFilter(ACTION_STATE_CHANGED))
        mListener = SplashActivityEventListener(eventHandler)
    }
    private fun initBinding()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_splash)
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
        searchBluetooth()
        cleanEvents()
    }

    override fun onDestroy()
    {
        super.onDestroy()
        unregisterReceiver(mEnableBluetoothBroadCast)
    }
    private fun searchBluetooth()
    {
        val bluetoothAdapter = getSystemService(BluetoothManager::class.java).adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.toast_bluetooth_not_found_text, Toast.LENGTH_LONG).show()
            finish()
        }
        else if (!bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(ACTION_REQUEST_ENABLE)
            bluetoothActivityResultLauncher.launch(enableBluetoothIntent)
        }
        else
            initCountDownTimer()
    }
    private fun handleBroadcastReceiver() : BroadcastReceiver
    {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action

                if (action == ACTION_STATE_CHANGED) {
                    if (resultCode == RESULT_OK)
                        initCountDownTimer()
                    else
                        finish()
                }
            }
        }
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
        threadPool.execute {
            try {
                mListener.startEventListener()
                Thread.sleep(2000)
                mListener.stopListener()
            } catch (_: InterruptedException) {}
        }
    }
}