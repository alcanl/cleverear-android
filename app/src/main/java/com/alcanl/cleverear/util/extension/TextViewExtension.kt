package com.alcanl.cleverear.util.extension

import android.content.Context
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.alcanl.cleverear.R

fun TextView.startTextAlphaAnimation(context: Context, string: String)
{
    val animationListener = object: AnimationListener{
        override fun onAnimationStart(p0: Animation?) { this@startTextAlphaAnimation.text = string }
        override fun onAnimationEnd(p0: Animation?) {}
        override fun onAnimationRepeat(p0: Animation?) {}
    }
    val animation = AnimationUtils.loadAnimation(context, R.anim.alpha_blink_anim)
    animation.setAnimationListener(animationListener)
    this.startAnimation(animation)
}