package com.willeypianotuning.toneanalyzer.ui.files

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import com.willeypianotuning.toneanalyzer.R

class FileSortOrderDialog(context: Context, initialSortOrder: Int) : Dialog(context) {
    private var currentSortOrder = initialSortOrder

    private val ibNameAscending: ImageButton by lazy { findViewById(R.id.sortButtonNameAscending) }
    private val ibNameDescending: ImageButton by lazy { findViewById(R.id.sortButtonNameDescending) }
    private val ibMakeAscending: ImageButton by lazy { findViewById(R.id.sortButtonMakeAscending) }
    private val ibMakeDescending: ImageButton by lazy { findViewById(R.id.sortButtonMakeDescending) }
    private val ibDateDescending: ImageButton by lazy { findViewById(R.id.sortButtonDateDescending) }
    private val ibDateAscending: ImageButton by lazy { findViewById(R.id.sortButtonDateAscending) }
    private val makeDefaultCheckBox: CheckBox by lazy { findViewById(R.id.checkBoxMakeDefault) }

    var onSortOrderChangeListener: OnSortOrderChangeListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sort_popup)

        ibNameAscending.setOnClickListener {
            currentSortOrder = FileSortOrder.NAME_ASCENDING
            updateSortPopup()
        }
        ibNameDescending.setOnClickListener {
            currentSortOrder = FileSortOrder.NAME_ASCENDING
            updateSortPopup()
        }
        ibMakeAscending.setOnClickListener {
            currentSortOrder = FileSortOrder.MAKE_DESCENDING
            updateSortPopup()
        }
        ibMakeDescending.setOnClickListener {
            currentSortOrder = FileSortOrder.MAKE_ASCENDING
            updateSortPopup()
        }
        ibDateDescending.setOnClickListener {
            currentSortOrder = FileSortOrder.DATE_DESCENDING
            updateSortPopup()
        }
        ibDateAscending.setOnClickListener {
            currentSortOrder = FileSortOrder.DATE_ASCENDING
            updateSortPopup()
        }

        val okButton = findViewById<Button>(R.id.buttonPositive)

        updateSortPopup()

        okButton.setOnClickListener {
            onSortOrderChangeListener?.onSortOrderChanged(
                currentSortOrder,
                makeDefaultCheckBox.isChecked
            )
            dismiss()
        }

        setCanceledOnTouchOutside(true)
        setTitle(context.getString(R.string.activity_files_sort_dialog_title))
    }

    private fun updateSortPopup() {
        val colorNormal = ContextCompat.getColor(context, R.color.transparent)
        val colorActive = ContextCompat.getColor(context, R.color.files_sort_active)
        ibNameAscending.setBackgroundColor(colorNormal)
        ibNameDescending.setBackgroundColor(colorNormal)
        ibMakeAscending.setBackgroundColor(colorNormal)
        ibMakeDescending.setBackgroundColor(colorNormal)
        ibDateDescending.setBackgroundColor(colorNormal)
        ibDateAscending.setBackgroundColor(colorNormal)
        when (currentSortOrder) {
            FileSortOrder.NAME_ASCENDING -> ibNameDescending.setBackgroundColor(colorActive)
            FileSortOrder.MAKE_ASCENDING -> ibMakeDescending.setBackgroundColor(colorActive)
            FileSortOrder.DATE_ASCENDING -> ibDateAscending.setBackgroundColor(colorActive)
            FileSortOrder.NAME_DESCENDING -> ibNameAscending.setBackgroundColor(colorActive)
            FileSortOrder.MAKE_DESCENDING -> ibMakeAscending.setBackgroundColor(colorActive)
            FileSortOrder.DATE_DESCENDING -> ibDateDescending.setBackgroundColor(colorActive)
        }
    }

    fun interface OnSortOrderChangeListener {
        fun onSortOrderChanged(sortOrder: Int, makeDefault: Boolean)
    }
}