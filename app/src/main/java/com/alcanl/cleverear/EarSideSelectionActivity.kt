package com.alcanl.cleverear

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityEarSideSelectionBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.helpers.EarSide
import com.alcanl.cleverear.viewmodel.EarSideSelectionActivityListenersViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
@AndroidEntryPoint
class EarSideSelectionActivity : AppCompatActivity() {
    @Inject
    lateinit var configuration: ConfigurationModule
    private lateinit var mBinding : ActivityEarSideSelectionBinding
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
    }

    override fun onResume()
    {
        super.onResume()
        mBinding.frameLayoutLeftEar.startAnimation(android.view.animation.AnimationUtils.loadAnimation
            (this, R.anim.translate_ear_button_up_to_down))
        mBinding.frameLayoutRightEar.startAnimation(android.view.animation.AnimationUtils.loadAnimation
            (this, R.anim.translate_ear_button_down_to_up))
    }
    private fun initialize()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_ear_side_selection)
        mBinding.viewModel = EarSideSelectionActivityListenersViewModel(this)
    }
    fun leftEarButtonClicked()
    {
        configuration.earSide = EarSide.Left
        Intent(this, ConnectionActivity::class.java).apply { startActivity(this) }
    }
    fun rightEarButtonClicked()
    {
        configuration.earSide = EarSide.Right
        Intent(this, ConnectionActivity::class.java).apply { startActivity(this) }
    }
}