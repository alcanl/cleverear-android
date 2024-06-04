package com.alcanl.cleverear.util

import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import com.alcanl.cleverear.R

fun FrameLayout.postDelayAnimation(delay: Long, context: Context)
{
    this.apply {
        postDelayed({
            visibility = View.VISIBLE
            startAnimation(AnimationUtils.loadAnimation(context, R.anim.translate_textview_device_init))
        }, delay)
    }
}