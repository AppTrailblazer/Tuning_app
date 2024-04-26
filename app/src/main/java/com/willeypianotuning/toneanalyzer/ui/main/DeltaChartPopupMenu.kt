package com.willeypianotuning.toneanalyzer.ui.main

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.core.content.ContextCompat
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.extensions.dpToPx

typealias OnDeltaChartPopupItemClicked = ((action: Int, note: Int?) -> Unit)

class DeltaChartPopupMenu {
    companion object {
        const val ACTION_RESET_MEASURED_PITCH_DATA = 1
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show(anchor: View, onItemClick: OnDeltaChartPopupItemClicked) {
        val context = anchor.context
        val layout: View = LayoutInflater.from(context).inflate(R.layout.popup_menu_custom, null)
        val pwindow = PopupWindow(context)
        pwindow.contentView = layout

        val items = arrayOf<String>(
            context.getString(R.string.action_reset_measured_pitch_data)
        )
        val listview: ListView = pwindow.contentView.findViewById<ListView>(android.R.id.list)
        val adapterTypeSelection: ArrayAdapter<CharSequence> = ArrayAdapter<CharSequence>(
            context,
            R.layout.popup_menu_custom_item,
            android.R.id.text1,
            items
        )
        listview.adapter = adapterTypeSelection

        listview.onItemClickListener =
            OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                onItemClick.invoke(ACTION_RESET_MEASURED_PITCH_DATA, null)
                pwindow.dismiss()
            }
        pwindow.width = anchor.measuredWidth
        pwindow.isTouchable = true
        pwindow.isOutsideTouchable = true
        pwindow.setTouchInterceptor { v: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                pwindow.dismiss()
            }
            false
        }
        pwindow.height = LinearLayout.LayoutParams.WRAP_CONTENT
        pwindow.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(context, R.color.black)))
        pwindow.showAsDropDown(anchor, 0, (-anchor.height - 56f.dpToPx(context.resources)).toInt())
    }

}