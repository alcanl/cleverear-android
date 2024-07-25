package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.SelfFittingActivity
import java.lang.ref.WeakReference

class InSituInProgressDialogListenersViewModel(activity: SelfFittingActivity) {
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