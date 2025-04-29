package com.alcanl.cleverear.util.dialog

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.alcanl.cleverear.MainContainerActivity
import com.alcanl.cleverear.R
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.util.service.AutoConnectService
import com.example.awesomedialog.AwesomeDialog
import com.example.awesomedialog.body
import com.example.awesomedialog.icon
import com.example.awesomedialog.onNegative
import com.example.awesomedialog.onPositive
import com.example.awesomedialog.position
import com.example.awesomedialog.title

fun showBatteryVoltageForConnectionDialog(activity: AppCompatActivity)
{
    AwesomeDialog.build(activity)
        .position(AwesomeDialog.POSITIONS.CENTER)
        .title(
            activity.getString(R.string.dialog_info_title),
            titleColor = ContextCompat.getColor(activity, android.R.color.black)
        )
        .body(
            activity.getString(R.string.dialog_connection_requires_battery_voltage_text),
            color = ContextCompat.getColor(activity, android.R.color.black)
        )
        .icon(R.drawable.icon_warning_new, true)
        .onPositive(
            activity.getString(R.string.alert_dialog_connection_problem_ok_button),
            buttonBackgroundColor = com.google.android.material.R.color.design_default_color_secondary_variant,
            textColor = ContextCompat.getColor(activity, android.R.color.white)
        ).show()
}

fun showLowTemperatureWarningDialog(activity: Activity)
{
    AwesomeDialog.build(activity)
        .position(AwesomeDialog.POSITIONS.CENTER)
        .title(
            activity.getString(R.string.dialog_info_title),
            titleColor = ContextCompat.getColor(activity, android.R.color.black)
        )
        .body(
            "Anormal düşük sıcaklık algılandı!",
            color = ContextCompat.getColor(activity, android.R.color.black)
        )
        .icon(R.drawable.icon_warning_new, true)
        .onPositive(
            activity.getString(R.string.alert_dialog_connection_problem_ok_button),
            buttonBackgroundColor = com.google.android.material.R.color.design_default_color_secondary_variant,
            textColor = ContextCompat.getColor(activity, android.R.color.white)
        ).show()
}

fun showFallenDeviceWarningDialog(activity: Activity)
{
    AwesomeDialog.build(activity)
        .position(AwesomeDialog.POSITIONS.CENTER)
        .title(
            activity.getString(R.string.dialog_info_title),
            titleColor = ContextCompat.getColor(activity, android.R.color.black)
        )
        .body(
            "Cihaz düşme algılandı!",
            color = ContextCompat.getColor(activity, android.R.color.black)
        )
        .icon(R.drawable.icon_warning_new, true)
        .onPositive(
            activity.getString(R.string.alert_dialog_connection_problem_ok_button),
            buttonBackgroundColor = com.google.android.material.R.color.design_default_color_secondary_variant,
            textColor = ContextCompat.getColor(activity, android.R.color.white)
        ).show()
}

fun showHighTemperatureWarningDialog(activity: Activity)
{
    AwesomeDialog.build(activity)
        .position(AwesomeDialog.POSITIONS.CENTER)
        .title(
            activity.getString(R.string.dialog_info_title),
            titleColor = ContextCompat.getColor(activity, android.R.color.black)
        )
        .body(
            "Anormal yüksek sıcaklık algılandı!",
            color = ContextCompat.getColor(activity, android.R.color.black)
        )
        .icon(R.drawable.icon_warning_new, true)
        .onPositive(
            activity.getString(R.string.alert_dialog_connection_problem_ok_button),
            buttonBackgroundColor = com.google.android.material.R.color.design_default_color_secondary_variant,
            textColor = ContextCompat.getColor(activity, android.R.color.white)
        ).show()
}

fun showDeviceConnectedBeforeDialog(activity: AppCompatActivity, onPositive : () -> Unit, onNegative : () -> Unit)
{
    AwesomeDialog.build(activity)
        .position(AwesomeDialog.POSITIONS.BOTTOM)
        .title(
            activity.getString(R.string.dialog_info_title),
            titleColor = ContextCompat.getColor(activity, android.R.color.black)
        )
        .body(
            activity.getString(R.string.alert_dialog_connected_before_text),
            color = ContextCompat.getColor(activity, android.R.color.black)
        )
        .icon(R.drawable.dialog_info, true)
        .onPositive(
            activity.getString(R.string.dialog_yes),
            buttonBackgroundColor = com.google.android.material.R.color.design_default_color_secondary_variant,
            textColor = ContextCompat.getColor(activity, android.R.color.white)
        ) {
            onPositive()
        }
        .onNegative(
            activity.getString(R.string.dialog_no),
            buttonBackgroundColor = com.google.android.material.R.color.design_default_color_secondary_variant,
            textColor = ContextCompat.getColor(activity, android.R.color.white)
        ) {
            onNegative()
        }.show()
}

fun showBackgroundLocationPermissionActionDialog(activity: AppCompatActivity, configuration: ConfigurationModule)
{
    AwesomeDialog.build(activity)
        .onNegative(
            text = activity.getString(R.string.dialog_no),
            buttonBackgroundColor = com.google.android.material.R.color.design_default_color_secondary_variant,
            textColor = ContextCompat.getColor(activity, android.R.color.white))
        {
            configuration.apply {
                keepConnectionForServiceDestroy = true
                isSystemRestartOnService = false

                if (isServiceOnBoard)
                    (activity as MainContainerActivity).stopService(Intent(activity, AutoConnectService::class.java))
            }

            (activity as MainContainerActivity).handler.
            postDelayed({ activity.binding.navigationViewContainer.selectedItemId = R.id.navbar_control}, 1000)
        }
        .onPositive(
            text = activity.getString(R.string.dialog_yes),
            buttonBackgroundColor = com.google.android.material.R.color.design_default_color_secondary_variant,
            textColor = ContextCompat.getColor(activity, android.R.color.white))
        {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .apply { data = Uri.fromParts("package", activity.packageName, null) }

            (activity as MainContainerActivity).settingsLauncher.launch(intent)
        }
        .title(activity.getString(R.string.dialog_info_title))
        .body(activity.getString(R.string.alert_dialog_background_permission_required_text),
            color = ContextCompat.getColor(activity, android.R.color.black))
        .icon(R.drawable.dialog_info, true)
        .position(AwesomeDialog.POSITIONS.BOTTOM)
        .show()
}

fun showStopConnectionDialog(activity: FragmentActivity, onPositive: () -> Unit)
{
    AwesomeDialog.build(activity)
        .position(AwesomeDialog.POSITIONS.CENTER)
        .title(
            activity.getString(R.string.alert_dialog_connection_problem_title),
            titleColor = ContextCompat.getColor(activity, android.R.color.black)
        )
        .body(
            activity.getString(R.string.dialog_already_connected_message),
            color = ContextCompat.getColor(activity, android.R.color.black)
        )
        .icon(R.drawable.icon_warning_new)
        .onPositive(
            activity.getString(R.string.dialog_yes),
            buttonBackgroundColor = com.google.android.material.R.color.design_default_color_secondary_variant,
            textColor = ContextCompat.getColor(activity, android.R.color.white)
        ) {
            onPositive()
        }
        .onNegative(
            activity.getString(R.string.dialog_no),
            buttonBackgroundColor = com.google.android.material.R.color.design_default_color_secondary_variant,
            textColor = ContextCompat.getColor(activity, android.R.color.white)
        ).show()
}

fun createDeviceDisconnectSuccessDialog(activity: FragmentActivity) : AlertDialog
{
    return AwesomeDialog.build(activity)
        .position(AwesomeDialog.POSITIONS.CENTER)
        .title(activity.getString(R.string.dialog_device_disconnected_title))
        .body(activity.getString(R.string.dialog_device_disconnected_body))
        .icon(R.drawable.check_tick)
}

fun createBackgroundLocationPermissionGrantSuccessDialog(activity: FragmentActivity) : AlertDialog
{
    return AwesomeDialog.build(activity)
        .position(AwesomeDialog.POSITIONS.CENTER)
        .title(activity.getString(R.string.permission_given_success_text))
        .body(activity.getString(R.string.alert_dialog_foreground_service_active_text))
        .icon(R.drawable.check_tick)
}

fun createBackgroundLocationPermissionGrantUnSuccessDialog(activity: FragmentActivity) : AlertDialog
{
    return AwesomeDialog.build(activity)
        .position(AwesomeDialog.POSITIONS.CENTER)
        .title(activity.getString(R.string.permission_given_un_success_text))
        .body(activity.getString(R.string.alert_dialog_foreground_service_inactive_text))
        .icon(R.drawable.icon_warning_new)
}