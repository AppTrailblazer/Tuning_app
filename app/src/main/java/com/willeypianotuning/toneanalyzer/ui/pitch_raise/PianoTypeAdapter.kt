package com.willeypianotuning.toneanalyzer.ui.pitch_raise

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.audio.enums.PianoType
import com.willeypianotuning.toneanalyzer.audio.enums.PianoTypeEnum
import com.willeypianotuning.toneanalyzer.extensions.setBackgroundResourceCompat

class PianoTypeAdapter(context: Context) : BaseAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val face: Typeface = Typeface.createFromAsset(context.assets, "calibri.ttf")
    private var selectedType = -1
    var onSelectionChangedListener: OnSelectionChangedListener? = null

    private val pianoTypes: List<PianoTypeItem>

    init {
        val types = intArrayOf(
            PianoType.CONCERT_GRAND,
            PianoType.MEDIUM_GRAND,
            PianoType.BABY_GRAND,
            PianoType.FULL_UPRIGHT,
            PianoType.STUDIO_UPRIGHT,
            PianoType.CONSOLE,
            PianoType.SPINET,
            PianoType.OTHER,
            PianoType.UNSPECIFIED
        )
        val titles = context.resources.getStringArray(R.array.piano_types)
        val images = intArrayOf(
            R.drawable.ic_piano_concert_grand,
            R.drawable.ic_piano_medium_grand,
            R.drawable.ic_piano_baby_grand,
            R.drawable.ic_piano_full_upright,
            R.drawable.ic_piano_studio_upright,
            R.drawable.ic_piano_console,
            R.drawable.ic_piano_spinet,
            R.drawable.ic_piano_other,
            R.drawable.ic_piano_noname
        ).toTypedArray()
        val pianoTypes = mutableListOf<PianoTypeItem>()
        for (i in types.indices) {
            pianoTypes.add(PianoTypeItem(types[i], titles[i], images[i]))
        }
        this.pianoTypes = pianoTypes
    }

    fun setSelectedType(selectedType: Int) {
        this.selectedType = selectedType
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return pianoTypes.size
    }

    override fun getItem(position: Int): Any {
        return pianoTypes[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: {
            val view = inflater.inflate(R.layout.grid_item_piano_type, null)
            val holder = ViewHolder(view)
            view.tag = holder
            view
        }()
        val holder: ViewHolder = itemView.tag as ViewHolder
        val pianoType = pianoTypes[position]

        holder.txt.text = pianoTypes[position].title
        holder.img.setImageResource(pianoTypes[position].imageRes)
        holder.img.setBackgroundResourceCompat(
            if (pianoType.type == selectedType)
                R.drawable.piano_type_item_bg_pressed
            else
                R.drawable.piano_type_item_bg
        )
        holder.txt.typeface = face
        holder.img.setOnClickListener {
            selectedType = pianoType.type
            onSelectionChangedListener?.onSelectionChanged(pianoType.type)
            notifyDataSetChanged()
        }
        return itemView
    }

    private data class PianoTypeItem(
        @PianoTypeEnum
        val type: Int,
        val title: String,
        @DrawableRes
        val imageRes: Int
    )

    private class ViewHolder(view: View) {
        val txt: TextView = view.findViewById(R.id.grid_item_text)
        val img: ImageButton = view.findViewById(R.id.grid_item_button)
    }

    fun interface OnSelectionChangedListener {
        fun onSelectionChanged(@PianoTypeEnum pianoType: Int)
    }
}