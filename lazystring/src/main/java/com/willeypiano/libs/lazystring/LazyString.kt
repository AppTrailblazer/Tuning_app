package com.willeypiano.libs.lazystring

import android.content.Context
import android.os.Parcelable
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

sealed interface LazyString : Parcelable {
    @Parcelize
    data class Static(private val value: String) : LazyString {
        override fun resolve(ctx: Context): String = value
    }

    @Parcelize
    class Res(
        @StringRes private val resId: Int,
        private vararg val args: @RawValue Any,
    ) : LazyString {
        override fun resolve(ctx: Context): String {
            return ctx.getString(resId, *args)
        }

        override fun toString(): String {
            return "Res(resId=$resId, args=${args.contentToString()})"
        }

    }

    @Parcelize
    class Plural(
        @PluralsRes private val resId: Int,
        private val quantity: Int,
        private vararg val args: @RawValue Any,
    ) :
        LazyString {
        override fun resolve(ctx: Context): String {
            return ctx.resources.getQuantityString(resId, quantity, *args)
        }

        override fun toString(): String {
            return "Plural(resId=$resId, quantity=$quantity, args=${args.contentToString()})"
        }
    }

    fun resolve(ctx: Context): String
}

fun String.asLazyString(): LazyString {
    return LazyString.Static(this)
}