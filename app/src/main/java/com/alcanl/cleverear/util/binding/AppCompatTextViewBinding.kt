package com.alcanl.cleverear.util.binding

import android.annotation.SuppressLint
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter

object AppCompatTextViewBinding {

    @JvmStatic
    @BindingAdapter("textColor")
    fun bindTextColor(textView: AppCompatTextView?, @ColorRes colorRes: Int)
    {
        textView?.setTextColor(textView.context?.getColor(colorRes)!!)
    }

    @SuppressLint("SetTextI18n")
    @JvmStatic
    @BindingAdapter("text")
    fun bindText(textView: AppCompatTextView?, version: String)
    {
        textView?.text = version
    }
}