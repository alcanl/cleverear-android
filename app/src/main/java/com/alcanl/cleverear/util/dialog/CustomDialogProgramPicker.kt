package com.alcanl.cleverear.util.dialog

import android.app.Dialog
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.LayoutDialogProgramPickerBinding
import com.alcanl.cleverear.fitting.fragment.FittingFragment
import com.alcanl.cleverear.viewmodel.ProgramPickerDialogListenersViewModel

class CustomDialogProgramPicker(fragment: FittingFragment) : Dialog(fragment.requireContext(), R.style.DialogStyle) {
    private var mBinding : LayoutDialogProgramPickerBinding
    init {

        window?.setBackgroundDrawableResource(R.drawable.drawable_alert_dialog)
        setCancelable(false)
        mBinding = DataBindingUtil.inflate(layoutInflater,
            R.layout.layout_dialog_program_picker, null, false)
        mBinding.viewModel = ProgramPickerDialogListenersViewModel(fragment)
        val memoryCount = fragment.configuration.hearingAidMap[fragment.fittingConfig.selectedEarSide]?.numberOfMemories ?: 8

        mBinding.numberPicker.apply {
            minValue = 0
            maxValue = memoryCount - 1
            displayedValues = resources.getStringArray(R.array.programs).copyOfRange(0, memoryCount)
            value = fragment.configuration.hearingAidMap[fragment.fittingConfig.selectedEarSide]?.currentMemory?.value() ?: 0
        }
        setContentView(mBinding.root)
    }
}