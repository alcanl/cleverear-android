package com.alcanl.cleverear.util.extension

import android.content.Context
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentAdvancedBinding
import com.alcanl.cleverear.databinding.FragmentControlBinding
import com.google.android.material.slider.Slider

private fun Slider.setAuxMicSlidersSecondAnimation(binding: FragmentAdvancedBinding) : AnimationListener
{
    return object : AnimationListener {
        override fun onAnimationStart(animation: Animation?) {
            with(binding) {
                sliderAuxCommon.apply {
                    visibility = View.INVISIBLE
                    isEnabled = false
                }
                sliderMicCommon.apply {
                    visibility = View.INVISIBLE
                    isEnabled = false
                }

                textViewAuxL.visibility = View.VISIBLE
                textViewAuxR.visibility= View.VISIBLE
                textViewMicL.visibility = View.VISIBLE
                textViewMicR.visibility = View.VISIBLE
            }
        }
        override fun onAnimationEnd(animation: Animation?) {
            this@setAuxMicSlidersSecondAnimation.apply {
                visibility = View.VISIBLE
                isEnabled = true
            }
        }
        override fun onAnimationRepeat(animation: Animation?) {}
    }
}

private fun Slider.setAuxMicSlidersFirstAnimation(context: Context, binding: FragmentAdvancedBinding) : AnimationListener
{
    return object : AnimationListener {
        override fun onAnimationStart(animation: Animation?) {
            this@setAuxMicSlidersFirstAnimation.visibility = View.INVISIBLE
        }
        override fun onAnimationEnd(animation: Animation?) {

            binding.sliderAuxCommon.startAuxMicCommonSlidersAnimation(context)
            binding.sliderMicCommon.startAuxMicCommonSlidersAnimation(context)

            with(binding) {
                textViewAuxL.visibility = View.INVISIBLE
                textViewAuxR.visibility= View.INVISIBLE
                textViewMicL.visibility = View.INVISIBLE
                textViewMicR.visibility = View.INVISIBLE
            }
        }
        override fun onAnimationRepeat(animation: Animation?) {}
    }
}

private fun Slider.setVolumeSlidersFirstAnimation(context: Context, binding: FragmentControlBinding) : AnimationListener
{
    return object : AnimationListener {
        override fun onAnimationStart(animation: Animation?)
        {
            this@setVolumeSlidersFirstAnimation.isEnabled = false
        }

        override fun onAnimationEnd(animation: Animation?)
        {

            this@setVolumeSlidersFirstAnimation.visibility = View.INVISIBLE
            binding.textViewLeftSeekBar.visibility = View.INVISIBLE
            binding.textViewRightSeekBar.visibility = View.INVISIBLE

            if (binding.sliderCommon.visibility != View.VISIBLE) {
                binding.sliderCommon.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(
                        context, R.anim.alpha_blink_anim
                    ).also {it.setAnimationListener(object : AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {
                            binding.sliderCommon.visibility = View.VISIBLE
                            binding.sliderCommon.isEnabled = true
                        }
                        override fun onAnimationEnd(animation: Animation?) {}
                        override fun onAnimationRepeat(animation: Animation?) {}
                    })

                    })
            }
        }
        override fun onAnimationRepeat(animation: Animation?) {}
    }
}

private fun Slider.setVolumeSlidersSecondAnimation(binding: FragmentControlBinding) : AnimationListener
{
    return object : AnimationListener {
        override fun onAnimationStart(animation: Animation?)
        {
            binding.sliderCommon.visibility = View.GONE
            binding.sliderCommon.isEnabled = false
        }

        override fun onAnimationEnd(animation: Animation?)
        {
            this@setVolumeSlidersSecondAnimation.visibility = View.VISIBLE
            this@setVolumeSlidersSecondAnimation.isEnabled = true
            binding.textViewRightSeekBar.visibility = View.VISIBLE
            binding.textViewLeftSeekBar.visibility = View.VISIBLE
        }

        override fun onAnimationRepeat(animation: Animation?) {}
    }
}

fun Slider.startAuxMicSlidersSecondAnimation(binding: FragmentAdvancedBinding, context: Context, anim: Int)
{
    with(this) {
        startAnimation(
            android.view.animation.AnimationUtils.loadAnimation(
                context,
                anim
            ).also { it.setAnimationListener(this.setAuxMicSlidersSecondAnimation(binding)) })
    }
}

fun Slider.startAuxMicSlidersFirstAnimation(binding: FragmentAdvancedBinding, context: Context, anim: Int)
{
    with(this) {
        startAnimation(
            android.view.animation.AnimationUtils.loadAnimation(
                context,
                anim
            ).also { it.setAnimationListener(this.setAuxMicSlidersFirstAnimation(context, binding)) })
    }
}

fun Slider.startAuxMicCommonSlidersAnimation(context: Context) {
    this.startAnimation(
        android.view.animation.AnimationUtils.loadAnimation(
            context, R.anim.alpha_blink_anim
        ).also { it.setAnimationListener(object : AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}

                    override fun onAnimationEnd(animation: Animation?) {
                        this@startAuxMicCommonSlidersAnimation.visibility = View.VISIBLE
                        this@startAuxMicCommonSlidersAnimation.isEnabled = true
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}
                }
            )
        })
}

fun Slider.startVolumeSliderFirstAnimation(context: Context, binding: FragmentControlBinding, anim: Int)
{
    this.startAnimation(
        android.view.animation.AnimationUtils.loadAnimation(
            context,
            anim
        ).also { it.setAnimationListener(setVolumeSlidersFirstAnimation(context, binding)) })
}

fun Slider.startVolumeSliderSecondAnimation(context: Context, binding: FragmentControlBinding, anim: Int)
{
    this.startAnimation(
        android.view.animation.AnimationUtils.loadAnimation(
            context,
            anim
        ).also { it.setAnimationListener(setVolumeSlidersSecondAnimation(binding)) }
    )
}