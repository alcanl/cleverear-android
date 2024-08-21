package com.alcanl.cleverear.fragment

import java.lang.ref.WeakReference

class ActiveConnectionDialogListenersViewModelFragment(fragment: ConnectionFragment){
    private val mWeakReference = WeakReference(fragment)

    fun handlePositiveButton()
    {
        mWeakReference.get()!!.alertDialogPositiveButtonClicked()
    }
    fun handleNegativeButton()
    {
        mWeakReference.get()!!.alertDialogNegativeButtonClicked()
    }

}