package com.alcanl.cleverear.fitting.util

import android.os.Handler
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.widget.ImageView
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentHomeBinding
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.WHAT_LEFT_EAR_INITIALIZED
import com.alcanl.cleverear.util.WHAT_RIGHT_EAR_INITIALIZED

fun ImageView.getAnimationListener(side: EarSide, binding: FragmentHomeBinding, handler: Handler) : AnimationListener
{
    return object : AnimationListener {
        override fun onAnimationStart(animation: Animation?)
        {
            this@getAnimationListener.setImageResource(if (side == EarSide.Left) R.drawable.leftear_blue else R.drawable.rightear_red)
        }
        override fun onAnimationEnd(animation: Animation?)
        {
            binding.invalidateAll()
            handler.sendEmptyMessage(if (side == EarSide.Left) WHAT_LEFT_EAR_INITIALIZED else WHAT_RIGHT_EAR_INITIALIZED)
        }
        override fun onAnimationRepeat(animation: Animation?) {}
    }
}