package com.alcanl.cleverear.util.extension

import android.content.Context
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.core.view.children
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentHomeBinding
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.WHAT_LEFT_EAR_INITIALIZED
import com.alcanl.cleverear.util.WHAT_RIGHT_EAR_INITIALIZED
import ir.programmerplus.groupbox.GroupBoxLayout
import pl.bclogic.pulsator4droid.library.PulsatorLayout

fun PulsatorLayout.startAlphaBlinkAnimation()
{
    this.start()

    val anim: Animation = AlphaAnimation(0.0f, 1.0f)
    anim.duration = 600
    anim.startOffset = 20
    anim.repeatCount = 0
    this.startAnimation(anim)
}

fun GroupBoxLayout.startAlphaBlinkAnimation()
{
    this.visibility = View.VISIBLE
    val anim: Animation = AlphaAnimation(0.0f, 1.0f)
    anim.duration = 400
    anim.startOffset = 20
    anim.repeatCount = 0
    this.startAnimation(anim)
}

fun FrameLayout.postDelayAnimation(delay: Long, context: Context)
{
    this.apply {
        postDelayed({
            this.setChildrenVisible()
            startAnimation(AnimationUtils.loadAnimation(context, R.anim.translate_textview_device_init))
        }, delay)
    }
}
fun getFrameLayoutAnimationListener(side: EarSide, binding: FragmentHomeBinding, handler: Handler) : Animation.AnimationListener
{
    return object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation?)
        {
            handler.sendEmptyMessage(if (side == EarSide.Left) WHAT_LEFT_EAR_INITIALIZED else WHAT_RIGHT_EAR_INITIALIZED)
        }
        override fun onAnimationEnd(animation: Animation?) {  }
        override fun onAnimationRepeat(animation: Animation?) { binding.invalidateAll() }
    }
}

fun FrameLayout.setChildrenInvisible()
{
    this.visibility = View.INVISIBLE
    this.children.forEach {
        it.visibility = View.INVISIBLE
    }
}
fun FrameLayout.setChildrenVisible()
{
    this.visibility = View.INVISIBLE
    this.children.forEach {
        it.visibility = View.VISIBLE
    }
}
fun ViewGroup.setLayoutDisable()
{
    this.children.iterator().forEachRemaining {
        if (it is ViewGroup)
            it.setLayoutDisable()
        else
            it.isEnabled = false
    }
}
fun ViewGroup.setLayoutEnable()
{
    this.children.iterator().forEachRemaining {
        if (it is ViewGroup && it !is FrameLayout)
            it.setLayoutEnable()
        else
            it.isEnabled = true
    }
}