package com.alcanl.cleverear.fitting.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentInSituBinding
import com.alcanl.cleverear.di.module.FittingConfigurationModule
import com.alcanl.cleverear.viewmodel.InSituFragmentListenersViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@AndroidEntryPoint
class InSituFragment : Fragment() {
    @Inject
    lateinit var fittingConfig : FittingConfigurationModule
    @Inject
    lateinit var threadPool : ExecutorService

    private lateinit var mBinding : FragmentInSituBinding
    private lateinit var mHandler : InSituHandler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_in_situ, container, false)
        initialize()
        return mBinding.root
    }
    private fun initialize()
    {
        mBinding.viewModel = InSituFragmentListenersViewModel(this)
        mBinding.config = fittingConfig
        mHandler = InSituHandler(this)
    }
    private class InSituHandler(fragment: InSituFragment) : Handler(Looper.myLooper()!!) {
        private val mWeakReference = WeakReference(fragment)

        override fun handleMessage(msg: Message)
        {

        }

    }
}