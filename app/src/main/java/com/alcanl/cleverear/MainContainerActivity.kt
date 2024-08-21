package com.alcanl.cleverear

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.alcanl.cleverear.databinding.ActivityMainContainerBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainContainerActivity : AppCompatActivity() {

    @Inject
    lateinit var configuration: ConfigurationModule

    lateinit var binding : ActivityMainContainerBinding

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_container)
        initializeNavController()
    }

    private fun navigateTo(id: Int)
    {
        findNavController(R.id.navHostMainContainer).navigate(id)
    }

    private fun initializeNavController()
    {
        binding.navigationViewContainer.setOnItemSelectedListener {

            when (it.itemId) {
                R.id.navbar_connection -> { navigateTo(R.id.navbar_connection); true }
                R.id.navbar_control -> {
                    if (!configuration.isConnected)
                        false
                    else {
                        navigateTo(R.id.navbar_control); true
                    }
                }
                else -> {
                    if (!configuration.isConnected)
                        false
                    else {
                        navigateTo(R.id.navbar_advanced); true
                }}
            }
        }
    }
}
