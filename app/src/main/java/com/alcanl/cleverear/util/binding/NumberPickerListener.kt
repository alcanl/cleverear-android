package com.alcanl.cleverear.util.binding

import android.widget.NumberPicker
import androidx.databinding.BindingAdapter

object NumberPickerListener {

    @JvmStatic
    @BindingAdapter(value = ["onValueChangeListener"])
    fun setOnValueChangeListener(numberPicker: NumberPicker, listener: OnValueChanged)
    {
        numberPicker.setOnValueChangedListener { _, _, newVal ->
            listener.onValueChanged(newVal)
        }
    }

    interface OnValueChanged {
        fun onValueChanged(value: Int)
    }
}