package com.willeypianotuning.toneanalyzer.extensions

import android.view.View

private const val DEFAULT_DEBOUNCE_INTERVAL = 400L

private class DebounceOnClickListener(
    private val interval: Long,
    private val listenerBlock: (View) -> Unit
) : View.OnClickListener {
    private var lastClickTime = 0L

    override fun onClick(v: View) {
        val time = System.currentTimeMillis()
        if (time - lastClickTime >= interval) {
            lastClickTime = time
            listenerBlock(v)
        }
    }
}

fun View.setDebounceOnClickListener(debounceInterval: Long = DEFAULT_DEBOUNCE_INTERVAL, listenerBlock: (View) -> Unit) =
    setOnClickListener(DebounceOnClickListener(debounceInterval, listenerBlock))