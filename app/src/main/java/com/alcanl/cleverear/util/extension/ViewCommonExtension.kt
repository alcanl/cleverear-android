package com.alcanl.cleverear.util.extension

import android.view.View

fun View.hide()
{
    this.visibility = View.INVISIBLE
}

fun View.show()
{
    this.visibility = View.VISIBLE
}

fun View.gone()
{
    this.visibility = View.GONE
}