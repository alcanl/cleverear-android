package com.alcanl.cleverear

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityAdvancedBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
@AndroidEntryPoint
class AdvancedActivity : AppCompatActivity(){
    @Inject
    lateinit var configuration : ConfigurationModule
    private lateinit var mBinding : ActivityAdvancedBinding
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
    }

    override fun onResume()
    {
        super.onResume()
        if (!configuration.isConnected)
            finish()

        mBinding.bottomNavigationView.selectedItemId = R.id.navbar_advanced
    }
    private fun initialize()
    {
        initBinding()
        initializeNavigationView()

    }
    private fun initBinding()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_advanced)
    }
    private fun initializeNavigationView()
    {
        mBinding.bottomNavigationView.setOnItemSelectedListener {

            when (it.itemId) {
                R.id.navbar_control -> {
                    initializeNavigationView(ControlActivity::class.java); finish(); true}
                R.id.navbar_connection -> {
                    initializeNavigationView(ConnectionActivity::class.java); finish(); true}
                else -> true
            }
        }
    }
    private inline fun <reified T : AppCompatActivity> initializeNavigationView(activity: Class<T>)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivity(Intent(this, T::class.java))
            overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
        } else {
            Intent(this, T::class.java).apply {
                val animationBundle = ActivityOptions.makeCustomAnimation(
                    this@AdvancedActivity,
                    R.anim.slide_in,
                    R.anim.slide_out
                ).toBundle()
                startActivity(this, animationBundle)
            }
        }
    }
}