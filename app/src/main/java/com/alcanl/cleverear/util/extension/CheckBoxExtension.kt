package com.alcanl.cleverear.util.extension

import android.util.Log
import android.widget.CheckBox

fun CheckBox.handleFittingCheckedChanged(checkedEnabled: ()-> Unit, checkedDisabled: ()-> Unit)
{
    try {
        this.apply {
                if (!isChecked)
                    checkedDisabled()
                else
                    checkedEnabled()
        }

    } catch (ex: Throwable) {
        Log.e("handleFittingCheckedChanged", ex.message!!)
    }
}