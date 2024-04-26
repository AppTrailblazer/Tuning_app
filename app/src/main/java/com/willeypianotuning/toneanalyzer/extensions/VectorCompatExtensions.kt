package com.willeypianotuning.toneanalyzer.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources

private fun getDrawableCompat(context: Context, @DrawableRes resId: Int): Drawable? {
    if (resId == 0) {
        return null
    }
    return AppCompatResources.getDrawable(context, resId)
}

fun View.setBackgroundResourceCompat(@DrawableRes resId: Int) {
    background = AppCompatResources.getDrawable(context, resId)
}

fun TextView.setCompoundDrawableLeft(@DrawableRes resId: Int, withIntrinsicBounds: Boolean = true) {
    val drawable = getDrawableCompat(context, resId)?.apply {
        if (withIntrinsicBounds) {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
    }
    val drawables = this.compoundDrawables
    setCompoundDrawables(drawable, drawables[1], drawables[2], drawables[3])
}

fun TextView.setCompoundDrawableTop(@DrawableRes resId: Int, withIntrinsicBounds: Boolean = true) {
    val drawable = getDrawableCompat(context, resId)?.apply {
        if (withIntrinsicBounds) {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
    }
    val drawables = this.compoundDrawables
    setCompoundDrawables(drawables[0], drawable, drawables[2], drawables[3])
}

fun TextView.setCompoundDrawableRight(
    @DrawableRes resId: Int,
    withIntrinsicBounds: Boolean = true
) {
    val drawable = getDrawableCompat(context, resId)?.apply {
        if (withIntrinsicBounds) {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
    }
    val drawables = this.compoundDrawables
    setCompoundDrawables(drawables[0], drawables[1], drawable, drawables[3])
}

fun TextView.setCompoundDrawableBottom(
    @DrawableRes resId: Int,
    withIntrinsicBounds: Boolean = true
) {
    val drawable = getDrawableCompat(context, resId)?.apply {
        if (withIntrinsicBounds) {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
    }
    val drawables = this.compoundDrawables
    setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawable)
}