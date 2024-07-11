package com.alcanl.cleverear

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.alcanl.cleverear.databinding.ActivityRemoteFittingBinding
import com.alcanl.cleverear.fitting.event.FittingEvent
import com.alcanl.cleverear.fitting.event.InSituEvent
import com.alcanl.cleverear.util.dialog.CustomAlertDialogFittingUnsaved
import com.alcanl.cleverear.util.dialog.CustomAlertDialogInSituInProgress
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus

@AndroidEntryPoint
class RemoteFittingActivity : AppCompatActivity() {

    private val mUnsavedFittingDialog : CustomAlertDialogFittingUnsaved by lazy {
        CustomAlertDialogFittingUnsaved(this)
    }
    private val mInSituInProgressDialog: CustomAlertDialogInSituInProgress by lazy {
        CustomAlertDialogInSituInProgress(this)
    }
    lateinit var binding : ActivityRemoteFittingBinding
    private var mSelectedFragmentId : Int? = null
    var isFittingChangesMade = false
    var isInSituInProgress = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_remote_fitting)
        initializeNavController()

    }
    private fun navigateTo(id: Int)
    {
        findNavController(R.id.navHostFragmentActivityRemote).navigate(id)
    }
    private fun initializeNavController()
    {
        binding.navigationView.menu.forEach { menuItem ->
            menuItem.setOnMenuItemClickListener {
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
        navigateTo(mSelectedFragmentId!!)
    }

}