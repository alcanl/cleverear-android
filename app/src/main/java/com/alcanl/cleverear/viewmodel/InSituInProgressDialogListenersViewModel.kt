package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.RemoteFittingActivity
import java.lang.ref.WeakReference

class InSituInProgressDialogListenersViewModel(activity: RemoteFittingActivity) {
    private val mWeakReference = WeakReference(activity)

    fun handleYesButton()
    {
        mWeakReference.get()?.inSituInProgressDialogPositiveButtonClicked()
    }
    fun handleNoButton()
    {
        mWeakReference.get()?.inSituInProgressDialogNegativeButtonClicked()
    }
}