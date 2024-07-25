package com.alcanl.cleverear

import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.alcanl.cleverear.databinding.ActivityRemoteFittingBinding
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.fitting.event.DisconnectEvent
import com.alcanl.cleverear.fitting.event.FittingEvent
import com.alcanl.cleverear.fitting.event.InSituEvent
import com.alcanl.cleverear.fitting.event.listener.IEventListener
import com.alcanl.cleverear.util.dialog.CustomAlertDialogDeviceDisconnected
import com.alcanl.cleverear.util.dialog.CustomAlertDialogFittingUnsaved
import com.alcanl.cleverear.util.dialog.CustomAlertDialogInSituInProgress
import com.alcanl.cleverear.util.isConnected
import com.ark.ArkException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import java.util.EventObject

@AndroidEntryPoint
class SelfFittingActivity : AppCompatActivity(), IEventListener {

    private val mUnsavedFittingDialog : CustomAlertDialogFittingUnsaved by lazy {
        CustomAlertDialogFittingUnsaved(this)
    }
    private val mInSituInProgressDialog: CustomAlertDialogInSituInProgress by lazy {
        CustomAlertDialogInSituInProgress(this)
    }
    private val mDeviceDisconnectedDialog: CustomAlertDialogDeviceDisconnected by lazy {
        CustomAlertDialogDeviceDisconnected(this)
    }
    lateinit var binding : ActivityRemoteFittingBinding

    @Volatile
    var mSelectedFragmentId : Int? = null
    @Volatile
    var isFittingChangesMade : Boolean = false
    @Volatile
    var isInSituInProgress : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_remote_fitting)
        initializeNavController()
        EventBus.getDefault().register(this)

    }

    override fun onDestroy()
    {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onEventReceive(event: EventObject)
    {
        if (event is DisconnectEvent) {
            handleDisconnect(event.hearingAid)
        }

    }
    @OptIn(DelicateCoroutinesApi::class)
    private fun handleDisconnect(hearingAid: HearingAid?)
    {
        mDeviceDisconnectedDialog.show()

        if (hearingAid == null) {
            mDeviceDisconnectedDialog.binding.apply {
                progressBar.visibility = View.INVISIBLE
                buttonOk.visibility = View.VISIBLE
                textViewMainMessage.text = getString(R.string.toast_unknown_problem_text)
            }

        }
        else {
            mDeviceDisconnectedDialog.binding.textViewMainMessage.text = String.format(
                mDeviceDisconnectedDialog.binding.textViewMainMessage.text.toString(), hearingAid.side, hearingAid.name
            )

            GlobalScope.launch { handleDisconnectCallback(this, hearingAid) }
        }

    }
    private suspend fun handleDisconnectCallback(scope: CoroutineScope, hearingAid: HearingAid)
    {
        scope.async {
            try {
                hearingAid.communicationAdaptor?.connect()
                runBlocking {
                    delay(8000)
                }
            } catch (ex: ArkException) {
                Log.e("handleDisconnectCallback", ex.message!!)
            }
        }.await()

        val adapter = getSystemService(BluetoothManager::class.java).adapter
        if (adapter != null && isConnected(adapter.getRemoteDevice(hearingAid.address))) {
            mDeviceDisconnectedDialog.binding.apply {
                progressBar.visibility = View.INVISIBLE
                    textViewMainMessage.text = getString(R.string.notification_reconnect_text) }
            Handler(Looper.myLooper()!!).postDelayed({ mDeviceDisconnectedDialog.dismiss() }, 2000) }

    }
    private fun navigateTo(id: Int)
    {
        findNavController(R.id.navHostFragmentActivityRemote).navigate(id)
    }
    private fun initializeNavController()
    {
        binding.navigationView.setOnItemSelectedListener {

            if (isFittingChangesMade) {
                mSelectedFragmentId = it.itemId
                mUnsavedFittingDialog.show()
                false
            }
            else if (isInSituInProgress) {
                mSelectedFragmentId = it.itemId
                mInSituInProgressDialog.show()
                false
            }
            else when (it.itemId) {
                R.id.navbar_fitting -> { navigateTo(R.id.navbar_fitting); true }
                R.id.navbar_home -> { navigateTo(R.id.navbar_home); true }
                else -> { navigateTo(R.id.navbar_insitu); true }
            }
        }
    }
    fun unsavedFittingDialogPositiveButtonClicked()
    {
        EventBus.getDefault().post(FittingEvent(this))
        mUnsavedFittingDialog.dismiss()
    }
    fun unsavedFittingDialogNegativeButtonClicked()
    {
        isFittingChangesMade = false
        mUnsavedFittingDialog.dismiss()
        binding.navigationView.children.find { it.id == mSelectedFragmentId }
            ?.isSelected = true
        navigateTo(mSelectedFragmentId!!)
    }
    fun inSituInProgressDialogPositiveButtonClicked()
    {
        EventBus.getDefault().post(InSituEvent(this))
        mUnsavedFittingDialog.dismiss()

    }
    fun inSituInProgressDialogNegativeButtonClicked()
    {
        isInSituInProgress = false
        mUnsavedFittingDialog.dismiss()
        binding.navigationView.children.find { it.id == mSelectedFragmentId }
            ?.isSelected = true
        navigateTo(mSelectedFragmentId!!)
    }
    fun disconnectDialogOkButtonClicked()
    {
        mDeviceDisconnectedDialog.dismiss()

        Intent(this, MainActivity::class.java).apply {
            startActivity(this)
        }
        finish()
    }

}