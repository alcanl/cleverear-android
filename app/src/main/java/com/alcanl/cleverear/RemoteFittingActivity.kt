package com.alcanl.cleverear

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.databinding.ActivityRemoteFittingBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.fitting.WirelessFittingInitializer
import com.alcanl.cleverear.viewmodel.RemoteFittingActivityListenersViewModel
import com.ark.Library
import com.ark.ProductManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
@AndroidEntryPoint
class RemoteFittingActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityRemoteFittingBinding
    private lateinit var mLibrary: Library
    @Inject
    lateinit var wirelessFittingInitializer: WirelessFittingInitializer
    @Inject
    lateinit var configuration: ConfigurationModule
    @Inject
    lateinit var productManager: ProductManager
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
    }
    override fun onStart()
    {
        super.onStart()
        initializeProduct()
    }
    private fun initialize()
    {
        initBinding()
    }
    private fun initBinding()
    {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_remote_fitting)
        mBinding.viewModel = RemoteFittingActivityListenersViewModel(this)
    }
    private fun initializeProduct()
    {
        val deviceInfo = configuration.hearingAid.communicationAdaptor?.detectDevice()
        val libFile = wirelessFittingInitializer.findLibraryByLibraryId(deviceInfo?.libraryId?.toString()!!)

        if (libFile != null) {
            mLibrary = productManager.loadLibraryFromFile(libFile.absolutePath)
            configuration.productLeft = wirelessFittingInitializer.createProduct(deviceInfo.productId)
        }
        else
            Toast.makeText(this, "Library is null", Toast.LENGTH_LONG).show()

    }
}