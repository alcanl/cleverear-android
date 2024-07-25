package com.alcanl.cleverear.viewmodel

import com.alcanl.cleverear.fitting.fragment.FittingFragment
import java.lang.ref.WeakReference

class ProgramPickerDialogListenersViewModel(fragment: FittingFragment) {
    private val mWeakReference = WeakReference(fragment)

    fun handleNumberPickerOnValueChanged(pos: Int)
    {
        mWeakReference.get()?.numberPickerOnValueChanged(pos)
    }
    fun handleSelectProgramButtonClick()
    {
        mWeakReference.get()?.dialogSelectProgramClicked()
    }
}