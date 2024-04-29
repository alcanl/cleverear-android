package com.alcanl.cleverear

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.alcanl.cleverear.api.library.LibraryInfo
import com.alcanl.cleverear.databinding.ActivityRemoteFittingBinding
import com.alcanl.cleverear.fitting.WirelessFittingInitializer
import com.alcanl.cleverear.viewmodel.RemoteFittingActivityListenersViewModel
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
@AndroidEntryPoint
class RemoteFittingActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityRemoteFittingBinding
    private var mLibraryInfo: LibraryInfo? = null
    @Inject
    lateinit var wirelessFittingInitializer: WirelessFittingInitializer
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        initialize()
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
    fun getLibrary()
    {
        val modelName = "TIE_X_65_FPP_v4"
        val call = wirelessFittingInitializer.findLibrary(modelName)

        call.enqueue(object: Callback<LibraryInfo> {
            override fun onResponse(call: Call<LibraryInfo>, response: Response<LibraryInfo>)
            {
                val libraryInfo = response.body()

                if (libraryInfo != null) {
                    mLibraryInfo = libraryInfo
                    Log.d("getLibrary", libraryInfo.libraryId!!)
                    wirelessFittingInitializer.writeLibraryToFilesDir(libraryInfo.libraryFile!!)
                }
                else
                    Log.e("getLibrary", "Library is Null")

            }

            override fun onFailure(call: Call<LibraryInfo>, ex: Throwable)
            {
                Toast.makeText(this@RemoteFittingActivity, "Exception: ${ex.message}", Toast.LENGTH_LONG).show()
            }

        })
    }
}