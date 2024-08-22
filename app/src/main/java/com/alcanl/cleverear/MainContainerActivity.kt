package com.alcanl.cleverear

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.alcanl.cleverear.databinding.ActivityMainContainerBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.fragment.AdvancedFragment
import com.alcanl.cleverear.fragment.ConnectionFragment
import com.alcanl.cleverear.fragment.ControlFragment
import com.alcanl.cleverear.util.service.AutoConnectService
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

    private fun navigateTo(fragment: Fragment)
    {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (fragment !is ControlFragment)
            fragmentTransaction.setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
        fragmentTransaction.replace(binding.navHostMainContainer.id, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    private fun initializeNavController()
    {
        binding.navigationViewContainer.setOnItemSelectedListener {

            when (it.itemId) {
                R.id.navbar_connection -> { navigateTo(ConnectionFragment()); true }
                R.id.navbar_control -> {
                    if (!configuration.isConnected)
                        false
                    else {
                        runOnUiThread { binding.frameLayoutLoading.visibility = View.VISIBLE }
                        navigateTo(ControlFragment()); true
                    }
                }
                else -> {
                    if (!configuration.isConnected)
                        false
                    else {
                        navigateTo(AdvancedFragment()); true
                }}
            }
        }
    }

    fun stopService()
    {
        stopService(Intent(this, AutoConnectService::class.java))
    }
}
