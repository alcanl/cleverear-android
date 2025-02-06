package com.alcanl.cleverear.util.extension

import android.content.Context
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.alcanl.cleverear.R

fun TextView.startAlphaAnimation(context: Context, string: String)
{
    this.startAnimation(AnimationUtils.loadAnimation(context, R.anim.alpha_blink_anim).also()
    {
        it.setAnimationListener(object: AnimationListener{
            override fun onAnimationStart(p0: Animation?) {
                this@startAlphaAnimation.text = string
            }
            override fun onAnimationEnd(p0: Animation?) {}
            override fun onAnimationRepeat(p0: Animation?) {}
        })
    })
}