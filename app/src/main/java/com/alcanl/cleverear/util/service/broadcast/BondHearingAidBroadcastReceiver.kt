package com.alcanl.cleverear.util.service.broadcast

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.util.Log
import com.alcanl.cleverear.R
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.util.EarSide
import javax.inject.Inject
import kotlin.system.exitProcess
class BondHearingAidBroadcastReceiver @Inject constructor(): HiltBroadcastReceiver() {
    @Inject
    lateinit var configuration : ConfigurationModule
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?)
    {
        super.onReceive(context, intent)
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
    }
    private fun handleStateBonded()
    {

        with(configuration) {

            if (!isDoubleDeviceConnection)
                hearingAidMap[earSide]?.apply {
                    bondStatus = BluetoothDevice.BOND_BONDED
                    readDeviceConfiguration()
                }
            else if (earSide == EarSide.Left)
                hearingAidMap[EarSide.Left]?.apply {
                    bondStatus = BluetoothDevice.BOND_BONDED
                    readDeviceConfiguration()
                }
            else
                hearingAidMap[EarSide.Right]?.apply {
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