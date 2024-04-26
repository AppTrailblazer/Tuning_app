package com.willeypianotuning.toneanalyzer.ui.pitch_raise

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import com.willeypianotuning.toneanalyzer.R

class SingleChoiceArrayAdapter(context: Context, private val items: Array<CharSequence>) :
    BaseAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var selectedPosition = -1

    private var onSelectionChangedListener: OnSelectionChangedListener? = null

    fun setSelectedPosition(selectedPosition: Int) {
        this.selectedPosition = selectedPosition
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int {
        return selectedPosition
    }

    fun setOnSelectionChangedListener(onSelectionChangedListener: OnSelectionChangedListener?) {
        this.onSelectionChangedListener = onSelectionChangedListener
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView
            ?: inflater.inflate(R.layout.list_item_simple_single_choice, parent, false).also {
                it.tag = ViewHolder(it)
            }
        val holder = itemView.tag as ViewHolder
        holder.txt.text = items[position]
        holder.txt.isChecked = position == selectedPosition
        holder.txt.setOnClickListener {
            selectedPosition = position
            onSelectionChangedListener?.onSelectionChanged(position)
            notifyDataSetChanged()
        }
        return itemView
    }

    private class ViewHolder(view: View) {
        val txt: CheckedTextView = view.findViewById(android.R.id.text1)
    }

    fun interface OnSelectionChangedListener {
        fun onSelectionChanged(position: Int)
    }

}