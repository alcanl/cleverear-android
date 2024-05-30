package com.alcanl.cleverear

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.alcanl.cleverear.databinding.ActivityRemoteFittingBinding

class RemoteFittingActivity : AppCompatActivity() {
    private lateinit var mBinding : ActivityRemoteFittingBinding
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_remote_fitting)
        val navController = findNavController(mBinding.navigationView.id)
        val bottomBarConfig = AppBarConfiguration(setOf(R.layout.fragment_home, R.layout.fragment_in_situ,
            R.layout.fragment_audiogram, R.layout.fragment_fitting))

        setupActionBarWithNavController(navController, bottomBarConfig)
        mBinding.navigationView.setupWithNavController(navController)
    }
}