package com.alcanl.cleverear

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.alcanl.cleverear.databinding.ActivityRemoteFittingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RemoteFittingActivity : AppCompatActivity() {
    private lateinit var mBinding : ActivityRemoteFittingBinding
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_remote_fitting)
        val navController = findNavController(R.id.navHostFragmentActivityRemote)
        mBinding.navigationView.setupWithNavController(navController)

    }
}