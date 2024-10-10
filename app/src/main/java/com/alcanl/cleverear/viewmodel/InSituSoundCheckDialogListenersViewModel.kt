package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.fitting.fragment.InSituFragment
import java.lang.ref.WeakReference

class InSituSoundCheckDialogListenersViewModel(fragment: InSituFragment) {
    private val mWeakReference = WeakReference(fragment)

    fun handleYesButton()
    {
        mWeakReference.get()?.dialogPositiveButtonClicked()
    }
    fun handleNoButton()
    {
        mWeakReference.get()?.dialogNegativeButtonClicked()
    }
    fun handleEndTestButton()
    {
        mWeakReference.get()?.dialogEndTestButtonClicked()
    }
}