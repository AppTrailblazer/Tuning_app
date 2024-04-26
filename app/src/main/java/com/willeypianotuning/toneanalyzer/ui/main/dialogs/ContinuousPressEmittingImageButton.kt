package com.willeypianotuning.toneanalyzer.ui.main.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageButton

@SuppressLint("ClickableViewAccessibility")
class ContinuousPressEmittingImageButton : AppCompatImageButton {

    private val mainHandler = Handler(Looper.getMainLooper(), ValueChangeCallback())

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private inner class ValueChangeCallback : Handler.Callback {
        override fun handleMessage(msg: Message): Boolean {
            if (msg.obj != this@ContinuousPressEmittingImageButton) {
                return false
            }
            when (msg.what) {
                MESSAGE_UPDATE -> {
                    mainHandler.sendMessageDelayed(
                        mainHandler.obtainMessage(
                            MESSAGE_UPDATE,
                            this@ContinuousPressEmittingImageButton
                        ),
                        MESSAGE_REPEAT_DELAY
                    )
                    onContinuousPressListener?.onContinuouslyPressed(this@ContinuousPressEmittingImageButton)
                }

                MESSAGE_CANCEL -> {
                    mainHandler.removeMessages(
                        MESSAGE_UPDATE,
                        this@ContinuousPressEmittingImageButton
                    )
                }

                else -> return false
            }
            return true
        }
    }

    var onContinuousPressListener: OnContinuousPressListener? = null

    fun interface OnContinuousPressListener {
        fun onContinuouslyPressed(v: View)
    }

    init {
        setOnLongClickListener {
            mainHandler.sendMessage(mainHandler.obtainMessage(MESSAGE_UPDATE, this))
            return@setOnLongClickListener false
        }
        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                mainHandler.sendMessage(mainHandler.obtainMessage(MESSAGE_CANCEL, this))
            }
            return@setOnTouchListener false
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (!enabled) {
            mainHandler.sendMessage(mainHandler.obtainMessage(MESSAGE_CANCEL, this))
        }
    }

    companion object {
        private const val MESSAGE_UPDATE = 1
        private const val MESSAGE_CANCEL = 2
        private const val MESSAGE_REPEAT_DELAY: Long = 200
    }
}