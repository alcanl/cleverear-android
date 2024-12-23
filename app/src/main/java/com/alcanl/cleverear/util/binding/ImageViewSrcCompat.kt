package com.alcanl.cleverear.util.binding

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.databinding.BindingAdapter

object ImageViewSrcCompat {

    @JvmStatic
    @BindingAdapter(value = ["srcCompat"])
    fun bindSrcCompat(imageView: ImageView?, drawable: Drawable?)
    {
        imageView?.setImageDrawable(drawable)
    }
}