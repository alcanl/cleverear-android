package com.alcanl.cleverear

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityMainBinding
import com.alcanl.cleverear.helpers.BUNDLE_EAR_SIDE_KEY
import com.alcanl.cleverear.helpers.DOUBLE_DEVICE_SELECTION
import com.alcanl.cleverear.helpers.LEFT
import com.alcanl.cleverear.viewmodel.MainActivityListenersViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initBinding()
    }

    override fun onResume()
    {
        super.onResume()
        mBinding.buttonSingleDevice.startAnimation(android.view.animation.AnimationUtils.loadAnimation
            (this, R.anim.translate_button_left_to_right))
        mBinding.buttonDoubleDevice.startAnimation(android.view.animation.AnimationUtils.loadAnimation
            (this, R.anim.translate_button_right_to_left))
    }
    fun buttonSingleDeviceClick()
    {
        Intent(this, EarSideSelectionActivity::class.java).apply { startActivity(this) }
    }
    fun buttonDoubleDeviceClick()
    {
        Intent(this, ConnectionActivity::class.java).apply {
            putExtra(BUNDLE_EAR_SIDE_KEY, LEFT)
            putExtra(DOUBLE_DEVICE_SELECTION, true)
            setResult(RESULT_OK, this)
            startActivity(this)
        }
    }
    private fun initBinding()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mBinding.viewModel = MainActivityListenersViewModel(this)
    }
}