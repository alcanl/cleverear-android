package com.alcanl.cleverear.fitting.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.R
import com.alcanl.cleverear.databinding.FragmentRemoteConfigurationBinding
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class RemoteConfigurationFragment : Fragment() {
    private lateinit var mBinding : FragmentRemoteConfigurationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_remote_configuration, container, false)
        return mBinding.root
    }

}