package com.alcanl.cleverear.util.extension

import android.content.Context
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.ImageView
import com.alcanl.cleverear.R

fun ImageView.startAlphaAnimation(context: Context)
{
    this.startAnimation(AnimationUtils.loadAnimation(context, R.anim.translate_textview_device_init)
        .also { it.setAnimationListener(object: AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                this@startAlphaAnimation.visibility = View.VISIBLE
            }
            override fun onAnimationEnd(p0: Animation?) {}
            override fun onAnimationRepeat(p0: Animation?) {}

        }) })
}