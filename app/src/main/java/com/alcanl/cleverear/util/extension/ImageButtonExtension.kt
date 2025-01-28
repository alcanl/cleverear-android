package com.alcanl.cleverear.util.extension

import android.content.Context
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.widget.ImageButton
import com.alcanl.cleverear.R

private fun setStreamChannelAnimation(function : () -> Unit) : AnimationListener {
    return object : AnimationListener {
        override fun onAnimationStart(animation: Animation?) { function() }
        override fun onAnimationEnd(animation: Animation?) {}
        override fun onAnimationRepeat(animation: Animation?) {}
    }
}

fun ImageButton.startSetStreamChannelAnimation(context: Context, function: () -> Unit)
{
    this.startAnimation( android.view.animation.AnimationUtils.loadAnimation(
        context, R.anim.alpha_blink_anim
    ).also { setStreamChannelAnimation(function) })
}