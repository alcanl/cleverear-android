package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.SplashActivity
import java.lang.ref.WeakReference

class ConnectedBeforeDialogListenersViewModel(activity: SplashActivity){
    private val mWeakReference = WeakReference(activity)

    fun handlePositiveButton()
    {
        mWeakReference.get()!!.alertDialogPositiveButtonClicked()
    }
    fun handleNegativeButton()
    {
        mWeakReference.get()!!.alertDialogNegativeButtonClicked()
    }

}