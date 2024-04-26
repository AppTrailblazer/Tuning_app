package com.willeypianotuning.toneanalyzer.ui.files

import android.content.Context
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.extensions.setBackgroundResourceCompat
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuningInfo
import java.util.*

class TuningFilesAdapter constructor(
    context: Context,
    private var filesList: List<PianoTuningInfo>
) : ArrayAdapter<PianoTuningInfo>(context, 0, filesList) {

    private val images = intArrayOf(
        R.drawable.ic_piano_concert_grand,
        R.drawable.ic_piano_medium_grand,
        R.drawable.ic_piano_baby_grand,
        R.drawable.ic_piano_full_upright,
        R.drawable.ic_piano_studio_upright,
        R.drawable.ic_piano_console,
        R.drawable.ic_piano_spinet,
        R.drawable.ic_piano_other,
        R.drawable.ic_piano_noname
    )

    private var filteredFilesList: List<PianoTuningInfo> = ArrayList(filesList)
    private var filter: String? = null
    private var selectMode = false
    private val selectedFiles: MutableList<String> = ArrayList()

    fun setSelectMode(selectMode: Boolean) {
        this.selectMode = selectMode
        notifyDataSetChanged()
    }

    fun isSelectMode(): Boolean {
        return selectMode
    }

    fun selectedFiles(): List<PianoTuningInfo> {
        val result: MutableList<PianoTuningInfo> = ArrayList()
        for (file in filesList) {
            if (selectedFiles.contains(file.id)) {
                result.add(file)
            }
        }
        return result
    }

    fun setFilesList(filesList: List<PianoTuningInfo>) {
        this.filesList = filesList
        filter(filter)
        selectedFiles.clear()
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedFiles.clear()
        for ((id) in filesList) {
            selectedFiles.add(id)
        }
        notifyDataSetChanged()
    }

    fun deselectAll() {
        selectedFiles.clear()
        notifyDataSetChanged()
    }

    fun select(file: PianoTuningInfo) {
        if (!selectedFiles.contains(file.id)) {
            selectedFiles.add(file.id)
        }
        notifyDataSetChanged()
    }

    fun filter(query: String?) {
        if (query == null || query.trim { it <= ' ' }.isEmpty()) {
            filter = null
            filteredFilesList = ArrayList(filesList)
            notifyDataSetChanged()
            return
        }
        val terms = query.trim { it <= ' ' }.lowercase(Locale.getDefault()).split("\\s+".toRegex())
            .toTypedArray()
        val newList: MutableList<PianoTuningInfo> = ArrayList()
        for (file in filesList) {
            val name = file.name.lowercase(Locale.getDefault())
            val make = file.make.lowercase(Locale.getDefault())
            val model = file.model.lowercase(Locale.getDefault())
            var match = true
            for (term in terms) {
                if (!(name.contains(term) || make.contains(term) || model.contains(term))) {
                    match = false
                    break
                }
            }
            if (match) {
                newList.add(file)
            }
        }
        filter = query
        filteredFilesList = newList
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return filteredFilesList.size
    }

    override fun getItem(position: Int): PianoTuningInfo {
        return filteredFilesList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.list_item_tuning_file, parent, false)
                .also {
                    it.tag = ViewHolder(it)
                }
        val holder: ViewHolder = itemView.tag as ViewHolder
        val file = filteredFilesList[position]
        setViews(file, holder, position, itemView)
        return itemView
    }

    private fun setViews(file: PianoTuningInfo, holder: ViewHolder, position: Int, view: View?) {
        val date = DateFormat.getDateFormat(context).format(file.lastModified)
        holder.nameTxt.visibility = View.VISIBLE
        holder.authorTxt.visibility = View.VISIBLE
        holder.dateTxt.visibility = View.VISIBLE
        holder.img.setImageResource(images[file.type])
        holder.dateTxt.text = date
        holder.nameTxt.text = file.name
        holder.authorTxt.text = "${file.make} ${file.model}"
        holder.checkBox.isVisible = selectMode
        holder.checkBox.isChecked = selectedFiles.contains(file.id)
        holder.checkBox.setOnClickListener {
            if (selectedFiles.contains(file.id)) {
                selectedFiles.remove(file.id)
            } else {
                selectedFiles.add(file.id)
            }
        }
    }

    private class ViewHolder(view: View) {
        val img: ImageView = view.findViewById<ImageView>(R.id.files_list_item_image).apply {
            setBackgroundResourceCompat(R.drawable.piano_type_item_bg_pressed)
        }
        val nameTxt: TextView = view.findViewById(R.id.files_list_item_title)
        val authorTxt: TextView = view.findViewById(R.id.files_list_item_author)
        val dateTxt: TextView = view.findViewById(R.id.files_list_item_date)
        val checkBox: CheckBox = view.findViewById(R.id.files_list_item_checkbox)
    }
}