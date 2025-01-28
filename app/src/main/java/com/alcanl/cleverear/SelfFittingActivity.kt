package com.alcanl.cleverear

import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.alcanl.cleverear.databinding.ActivitySelfFittingBinding
import com.alcanl.cleverear.entity.HearingAid
import com.alcanl.cleverear.fitting.event.DisconnectEvent
import com.alcanl.cleverear.fitting.event.FittingEvent
import com.alcanl.cleverear.fitting.event.InSituEvent
import com.alcanl.cleverear.fitting.event.listener.IEventListener
import com.alcanl.cleverear.fitting.fragment.FittingFragment
import com.alcanl.cleverear.fitting.fragment.HomeFragment
import com.alcanl.cleverear.fitting.fragment.InSituFragment
import com.alcanl.cleverear.util.GERMAN
import com.alcanl.cleverear.util.TURKISH
import com.alcanl.cleverear.util.dialog.CustomAlertDialogDeviceDisconnected
import com.alcanl.cleverear.util.dialog.CustomAlertDialogFittingUnsaved
import com.alcanl.cleverear.util.dialog.CustomAlertDialogInSituInProgress
import com.alcanl.cleverear.util.getSystemLanguage
import com.alcanl.cleverear.util.isConnected
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.EventObject

@AndroidEntryPoint
class SelfFittingActivity : AppCompatActivity(), IEventListener {

    private val mUnsavedFittingDialog : CustomAlertDialogFittingUnsaved
        get() = CustomAlertDialogFittingUnsaved(this)

    private val mInSituInProgressDialog: CustomAlertDialogInSituInProgress
        get() = CustomAlertDialogInSituInProgress(this)

    private val mDeviceDisconnectedDialog: CustomAlertDialogDeviceDisconnected
        get() = CustomAlertDialogDeviceDisconnected(this)

    lateinit var binding : ActivitySelfFittingBinding

    @Volatile
    var mSelectedFragmentId : Int? = null

    @Volatile
    var isFittingChangesMade : Boolean = false

    @Volatile
    var isInSituInProgress : Boolean = false

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean
    {
        finish()
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_self_fitting)
        initializeNavController()
        EventBus.getDefault().register(this)

    }

    override fun onDestroy()
    {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onEventReceive(event: EventObject)
    {
        if (event is DisconnectEvent)
           handleDisconnect(event.hearingAid)
    }

    private fun handleDisconnect(hearingAid: HearingAid?)
    {
        lifecycleScope.launch(Dispatchers.Main) {

            mDeviceDisconnectedDialog.show()

            if (hearingAid == null)
                mDeviceDisconnectedDialog.binding.apply {
                    progressBar.visibility = View.INVISIBLE
                    buttonOk.visibility = View.VISIBLE
                    textViewMainMessage.text = getString(R.string.toast_unknown_problem_text)
                }
            else {
                mDeviceDisconnectedDialog.binding.textViewMainMessage.text = String.format(
                    getString(R.string.dialog_textview_device_disconnected_text),
                    if(getSystemLanguage().lowercase() == TURKISH) hearingAid.side.getTR()
                    else if (getSystemLanguage().lowercase() == GERMAN) hearingAid.side.getGE()
                    else hearingAid.side.getEN(), hearingAid.name)

                launch(Dispatchers.IO) { handleDisconnectCallback(this, hearingAid) }
            }
        }
    }

    private suspend fun handleDisconnectCallback(scope: CoroutineScope, hearingAid: HearingAid)
    {
        scope.async {
            try {
                delay(8000)
                hearingAid.communicationAdaptor?.connect()
                delay(8000)

            } catch (ex: Exception) {
                Log.e(SelfFittingActivity::class.simpleName, ex.message!!)
            }
        }.await()

        scope.launch(Dispatchers.Main) {

            val adapter = getSystemService(BluetoothManager::class.java).adapter
            if (adapter != null && isConnected(adapter.getRemoteDevice(hearingAid.address))) {
                mDeviceDisconnectedDialog.binding.apply {
                    progressBar.visibility = View.INVISIBLE
                    textViewMainMessage.text = getString(R.string.notification_reconnect_text) }
                Handler(Looper.myLooper()!!).postDelayed({ mDeviceDisconnectedDialog.dismiss() }, 2000) }
            else
                mDeviceDisconnectedDialog.binding.apply {
                    progressBar.visibility = View.INVISIBLE
                    buttonOk.visibility = View.VISIBLE
                }
        }
    }

    private fun navigateTo(fragment: Fragment)
    {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.navHostSelfFitting.id, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
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
                R.id.navbar_fitting -> {
                    runOnUiThread { binding.frameLayoutLoading.visibility = View.VISIBLE }
                    navigateTo(FittingFragment()); true }
                R.id.navbar_home -> {
                    runOnUiThread { binding.frameLayoutLoading.visibility = View.VISIBLE }
                    navigateTo(HomeFragment()); true }
                else -> {
                    runOnUiThread { binding.frameLayoutLoading.visibility = View.VISIBLE }
                    navigateTo(InSituFragment()); true }
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
        navigateTo(when (mSelectedFragmentId!!) {
            R.id.navbar_home -> HomeFragment()
            R.id.navbar_insitu -> InSituFragment()
            else -> FittingFragment()
        })
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
        navigateTo(when (mSelectedFragmentId!!) {
            R.id.navbar_home -> HomeFragment()
            R.id.navbar_insitu -> InSituFragment()
            else -> FittingFragment()
        })
    }

    fun disconnectDialogOkButtonClicked()
    {
        mDeviceDisconnectedDialog.dismiss()
        Intent(this, MainActivity::class.java).apply { startActivity(this) }
        finish()
    }
}