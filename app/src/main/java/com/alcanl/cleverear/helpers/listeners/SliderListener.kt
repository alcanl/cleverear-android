package com.alcanl.cleverear.helpers.listeners

import androidx.databinding.BindingAdapter
import com.google.android.material.slider.Slider

object SliderListener {
    @JvmStatic
    @BindingAdapter(value = ["onValueChangeListener"])
    fun setOnValueChangeListener(slider: Slider, listener: OnValueChangeListener)
    {
        slider.addOnChangeListener { _: Slider?, value: Float, fromUser: Boolean ->
            if (fromUser)
                listener.onValueChanged(value)
        }
    }
    interface OnValueChangeListener {
        fun onValueChanged(value: Float)
    }
}
