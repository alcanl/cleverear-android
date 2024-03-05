package com.alcanl.cleverear.util.broadcast

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.util.Log
import com.alcanl.cleverear.R
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.service.receiver.HiltBroadcastReceiver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class BondDeviceBroadcastReceiver: HiltBroadcastReceiver() {
    @Inject
    lateinit var configuration : ConfigurationModule
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?)
        {
        val action = intent!!.action

        if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
            val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)

            if (state < 0)
                Log.e("test", "notWorkingBroadcast")

            else if (state == BluetoothDevice.BOND_BONDED)
                handleStateBonded()

            else if (state == BluetoothDevice.BOND_NONE)
                handleStateNotBonded(context!!)


        }
        super.onReceive(context, intent)
    }
    private fun handleStateBonded()
    {

        with(configuration) {

            if (!isDoubleDeviceConnection)
                hearingAid.apply {
                    bondStatus = BluetoothDevice.BOND_BONDED
                    readDeviceConfiguration()
                }
            else if (earSide == EarSide.Left)
                doubleDeviceMap[EarSide.Left]?.apply {
                    bondStatus = BluetoothDevice.BOND_BONDED
                    readDeviceConfiguration()
                }
            else
                doubleDeviceMap[EarSide.Right]?.apply {
                    bondStatus = BluetoothDevice.BOND_BONDED
                    readDeviceConfiguration()
                }

        }
    }
    private fun handleStateNotBonded(context: Context)
    {
        AlertDialog.Builder(context)
            .setCancelable(true)
            .setMessage(R.string.dialog_unpaired_device_text)
            .setPositiveButton(R.string.alert_dialog_connection_problem_ok_button) {_,_ -> configuration.isConnected = false; exitProcess(1)}
            .setTitle(R.string.alert_dialog_connection_problem_title)
            .create().show()
    }
}