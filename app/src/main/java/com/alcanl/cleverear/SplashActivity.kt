package com.alcanl.cleverear

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var mBinding : ActivitySplashBinding


    private fun initCountDownTimer()
    {
        object: CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long)
            {

            }
            override fun onFinish()
            {
                finish()
                Intent(this@SplashActivity, MainActivity::class.java).apply { startActivity(this) }
            }
        }.start()
    }
    private fun initialize()
    {
        initBinding()
        initCountDownTimer()
    }
    private fun initBinding()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_splash)
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
    }
}