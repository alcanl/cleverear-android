package com.alcanl.cleverear

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.alcanl.cleverear.databinding.ActivityMainContainerBinding
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.fragment.AdvancedFragment
import com.alcanl.cleverear.fragment.BeaconFragment
import com.alcanl.cleverear.fragment.ConnectionFragment
import com.alcanl.cleverear.fragment.ControlFragment
import com.alcanl.cleverear.util.ACCESS_COARSE_LOCATION_PERMISSION_REQUEST_CODE
import com.alcanl.cleverear.util.ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE
import com.alcanl.cleverear.util.SMS_PERMISSION_REQUEST_CODE
import com.alcanl.cleverear.util.dialog.CustomDialogSmsInformation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainContainerActivity : AppCompatActivity() {

    @Inject
    lateinit var configuration: ConfigurationModule

    lateinit var binding : ActivityMainContainerBinding

    private val mDialogSmsInformation: CustomDialogSmsInformation by lazy {
        CustomDialogSmsInformation(this)
    }

    private val mLocationFinePermission by lazy {
        ContextCompat.checkSelfPermission(
            this,
            ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val mLocationCoarsePermission by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ContextCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        else true
    }

    val mSendSmsPermission by lazy {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_container)

        if (!hasPermissionsForBluetoothLocation())
            requestLocationPermission()

        initializeNavController()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == ACCESS_COARSE_LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
            grantResults[1] == PackageManager.PERMISSION_GRANTED
        )
            supportFragmentManager.beginTransaction().replace(binding.navHostMainContainer.id, ConnectionFragment()).commit()

        else if (requestCode == ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        )
            supportFragmentManager.beginTransaction().replace(binding.navHostMainContainer.id, ConnectionFragment()).commit()

        else if (requestCode == SMS_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            supportFragmentManager.beginTransaction().replace(binding.navHostMainContainer.id, BeaconFragment()).commit()
            mDialogSmsInformation.show()
        }

        else {
            Toast.makeText(this, R.string.toast_no_permission_splash_activity, Toast.LENGTH_LONG).show()
            finish()
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
                R.id.navbar_beacon -> {
                    if (!configuration.isConnected)
                        false
                    else {
                        navigateTo(BeaconFragment()); true }
                }
                else -> {
                    if (!configuration.isConnected)
                        false
                    else {
                        navigateTo(AdvancedFragment()); true }
                }
            }
        }
    }

    private fun requestLocationPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
                ACCESS_COARSE_LOCATION_PERMISSION_REQUEST_CODE
            )
        else
            ActivityCompat.requestPermissions(
                this,
                arrayOf(ACCESS_FINE_LOCATION),
                ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE
            )
    }

    private fun hasPermissionsForBluetoothLocation() : Boolean
    {
        return mLocationCoarsePermission && mLocationFinePermission
    }

    fun requestSmsPermission()
    {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.SEND_SMS),
            SMS_PERMISSION_REQUEST_CODE
        )
    }

    fun stopService()
    {
        configuration.stopServiceFlag = true
    }

    fun onDialogSaveContactButtonClicked()
    {

    }

    fun onDialogCancelButtonClicked()
    {

    }
}
