package com.alcanl.cleverear

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
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
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.SMS_PERMISSION_REQUEST_CODE
import com.alcanl.cleverear.util.dialog.CustomDialogSmsInformation
import com.alcanl.cleverear.util.hasBackgroundLocationPermission
import com.alcanl.cleverear.util.service.AutoConnectService
import com.example.awesomedialog.AwesomeDialog
import com.example.awesomedialog.body
import com.example.awesomedialog.icon
import com.example.awesomedialog.onNegative
import com.example.awesomedialog.onPositive
import com.example.awesomedialog.position
import com.example.awesomedialog.title
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.properties.Delegates


@AndroidEntryPoint
class MainContainerActivity : AppCompatActivity() {

    @Inject
    lateinit var configuration: ConfigurationModule

    @Inject
    lateinit var handler: Handler

    lateinit var binding : ActivityMainContainerBinding

    private var mSelectedNavItem by Delegates.notNull<Int>()

    private lateinit var mTrackingButton: AppCompatButton

    private lateinit var mSelectedTrackingEddystoneEarSide: EarSide

    private lateinit var mDialogSmsInformation: CustomDialogSmsInformation

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

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            if (hasBackgroundLocationPermission(this))
                Toast.makeText(this, "Uygulama cihaz bağlantısını takip ediyor", Toast.LENGTH_LONG).show()
            else
                Toast.makeText(this, "Gerekli izin verilmedi", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_container)
        configuration.isMainActivityOnScreen = true

        if (!hasPermissionsForBluetoothLocation())
            requestLocationPermission()

        mSelectedNavItem = R.id.navbar_connection
        initializeNavController()
    }

    override fun onDestroy()
    {
        super.onDestroy()
        configuration.isMainActivityOnScreen = false
    }

    override fun onResume()
    {
        super.onResume()

        if (configuration.isConnected && !hasBackgroundLocationPermission(this)) {
            showBackgroundLocationPermissionActionDialog()
            return
        }

        if (configuration.isConnected)
            handler.postDelayed({binding.navigationViewContainer.selectedItemId = R.id.navbar_control}, 1000)
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
            mDialogSmsInformation = CustomDialogSmsInformation(this)
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

    private fun showBackgroundLocationPermissionActionDialog()
    {
        AwesomeDialog.build(this)
            .onNegative(
                text = getString(R.string.dialog_no),
                buttonBackgroundColor = com.google.android.material.R.color.design_default_color_secondary_variant,
                textColor = ContextCompat.getColor(this, android.R.color.white))
            {
                if (configuration.isServiceOnBoard)
                    stopService(Intent(this, AutoConnectService::class.java))
            }
            .onPositive(
                text = getString(R.string.dialog_yes),
                buttonBackgroundColor = com.google.android.material.R.color.design_default_color_secondary_variant,
                textColor = ContextCompat.getColor(this, android.R.color.white))
            {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .apply { data = Uri.fromParts("package", packageName, null) }

                settingsLauncher.launch(intent)
            }
            .title(getString(R.string.dialog_info_title))
            .body("CleverEar uygulamasının cihaz bağlantısını takip edebilmesi için uygulamanın ayarlar sayfasından konum izninin  \"Her Zaman\" olarak verilmesi gerekmektedir.\n CleverEar'ın otomatik işitme cihazı bağlantısını takip etmesini ister misiniz?", color = ContextCompat.getColor(this, android.R.color.black))
            .icon(R.drawable.dialog_info, true)
            .position(AwesomeDialog.POSITIONS.BOTTOM)
            .show()
    }

    private fun initializeNavController()
    {
        binding.navigationViewContainer.setOnItemSelectedListener {

            when (it.itemId) {
                R.id.navbar_connection -> {
                    if (mSelectedNavItem == R.id.navbar_connection)
                        false
                    else {
                        mSelectedNavItem = R.id.navbar_connection
                        navigateTo(ConnectionFragment()); true }
                }
                R.id.navbar_control -> {
                    if (!configuration.isConnected || mSelectedNavItem == R.id.navbar_control)
                        false
                    else {
                        runOnUiThread { binding.frameLayoutLoading.visibility = View.VISIBLE }
                        mSelectedNavItem = R.id.navbar_control
                        navigateTo(ControlFragment()); true
                    }
                }
                R.id.navbar_beacon -> {
                    if (!configuration.isConnected || mSelectedNavItem == R.id.navbar_beacon)
                        false
                    else {
                        mSelectedNavItem = R.id.navbar_beacon
                        navigateTo(BeaconFragment()); true }
                }
                else -> {
                    if (!configuration.isConnected || mSelectedNavItem == R.id.navbar_advanced)
                        false
                    else {
                        mSelectedNavItem = R.id.navbar_advanced
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

    fun requestSmsPermission(earSide: EarSide, button: AppCompatButton)
    {
        mSelectedTrackingEddystoneEarSide = earSide
        mTrackingButton = button

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.SEND_SMS),
            SMS_PERMISSION_REQUEST_CODE
        )
    }

    fun onSMSContactDialogButtonClicked(button: AppCompatButton)
    {
        if (button.id == mDialogSmsInformation.binding.buttonSaveContact.id) {
            mDialogSmsInformation.saveSmsContactInformationToPref()
            configuration.eddyStoneMap[mSelectedTrackingEddystoneEarSide]?.isTrackingActive = true
            mTrackingButton.text = getString(R.string.button_disable_tracking_text)

            mDialogSmsInformation.binding.apply {
                frameLayoutOk.visibility = View.VISIBLE
                buttonOk.visibility = View.INVISIBLE
                textViewContactSave.visibility = View.INVISIBLE
                progressBar.visibility = View.VISIBLE

                handler.postDelayed({
                    progressBar.visibility = View.INVISIBLE
                    textViewContactSave.visibility = View.VISIBLE
                    buttonOk.visibility = View.VISIBLE
                }, 1000)
            }
            return
        }

        mDialogSmsInformation.dismiss()
    }

    fun showSMSInformationDialog(earSide: EarSide, button: AppCompatButton)
    {
        mSelectedTrackingEddystoneEarSide = earSide
        mTrackingButton = button
        mDialogSmsInformation = CustomDialogSmsInformation(this)
        mDialogSmsInformation.show()
    }
}
