package com.willeypianotuning.toneanalyzer.ui.main

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.core.content.ContextCompat
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.audio.note_names.NoteNames
import com.willeypianotuning.toneanalyzer.extensions.dpToPx

typealias OnInharmonicityChartPopupItemClicked = ((action: Int, note: Int) -> Unit)

class InharmonicityChartPopupMenu(
    private val appSettings: AppSettings
) {
    companion object {
        const val ACTION_DELETE_INHARMONICITY_FOR_NOTE = 1
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show(anchor: View, currentNote: Int, onItemClick: OnInharmonicityChartPopupItemClicked) {
        val context = anchor.context
        val layout: View = LayoutInflater.from(context).inflate(R.layout.popup_menu_custom, null)
        val pwindow = PopupWindow(context)
        pwindow.contentView = layout

        val noteNamingConvention = NoteNames.getNamingConvention(context, appSettings.noteNames)

        val clearInharmonicityItem: Spanned =
            SpannableStringBuilder(context.getString(R.string.action_delete_inharmonicity_for_current_note))
                .append(" (")
                .append(noteNamingConvention.pianoNoteName(currentNote - 1))
                .append(")")

        val items = arrayOf(
            clearInharmonicityItem
        )
        val listview: ListView = pwindow.contentView.findViewById(android.R.id.list)
        val adapterTypeSelection: ArrayAdapter<CharSequence> = ArrayAdapter<CharSequence>(
            context,
            R.layout.popup_menu_custom_item,
            android.R.id.text1,
            items
        )
        listview.adapter = adapterTypeSelection

        listview.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, _: Int, _: Long ->
                onItemClick.invoke(ACTION_DELETE_INHARMONICITY_FOR_NOTE, currentNote)
                pwindow.dismiss()
            }
        pwindow.width = anchor.measuredWidth
        pwindow.isTouchable = true
        pwindow.isOutsideTouchable = true
        pwindow.setTouchInterceptor { _: View?, event: MotionEvent ->
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