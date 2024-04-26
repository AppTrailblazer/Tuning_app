package com.willeypianotuning.toneanalyzer.ui.settings.backups

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.willeypianotuning.toneanalyzer.extensions.setDebounceOnClickListener

class BackupFileListAdapter : RecyclerView.Adapter<BackupFileListAdapter.BackupFileViewHolder>() {
    private val data: MutableList<String> = arrayListOf()

    var itemClickListener: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupFileViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
        return BackupFileViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: BackupFileViewHolder, position: Int) {
        holder.nameTextView.text = data[position]
    }

    fun replaceAll(data: List<String>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    inner class BackupFileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(android.R.id.text1)

        init {
            itemView.setDebounceOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return@setDebounceOnClickListener
                }
                itemClickListener?.invoke(data[position])
            }
        }
    }
}