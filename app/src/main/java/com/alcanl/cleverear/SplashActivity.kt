package com.alcanl.cleverear

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE
import android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var mBinding : ActivitySplashBinding
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
        object: CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long)
            {

            }
            override fun onFinish()
            {
                Intent(this@SplashActivity, MainActivity::class.java).apply { startActivity(this) }
                finish()
            }
        }.start()
    }

    private fun initialize()
    {
        initBinding()
        mEnableBluetoothBroadCast = handleBroadcastReceiver()
        registerReceiver(mEnableBluetoothBroadCast, IntentFilter(ACTION_STATE_CHANGED))
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
}