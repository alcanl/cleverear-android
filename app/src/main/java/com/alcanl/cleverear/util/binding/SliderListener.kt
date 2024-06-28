package com.alcanl.cleverear.util.binding

import android.content.res.ColorStateList
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
    @JvmStatic
    @BindingAdapter(value = ["app:haloColor"])
    fun bindSliderHaloColor(slider: Slider?, id: Int)
    {
        slider?.haloTintList = ColorStateList.valueOf(id)
    }
    @JvmStatic
    @BindingAdapter(value = ["app:thumbColor"])
    fun bindSliderThumbColor(slider: Slider?, id: Int)
    {
        slider?.haloTintList = ColorStateList.valueOf(id)
    }
    @JvmStatic
    @BindingAdapter(value = ["app:trackColorActive"])
    fun bindSliderTrackColorActive(slider: Slider?, id: Int)
    {
        slider?.haloTintList = ColorStateList.valueOf(id)
    }

    interface OnValueChangeListener {
        fun onValueChanged(value: Float)
    }
}
