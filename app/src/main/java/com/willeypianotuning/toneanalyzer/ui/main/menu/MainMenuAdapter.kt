package com.willeypianotuning.toneanalyzer.ui.main.menu

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.ui.colors.ColorFilter

class MainMenuAdapter(initialItems: List<MainMenuItem> = emptyList()) : BaseAdapter() {
    private var textColorPrimary: Int = Color.WHITE
    private var textColorSecondary: Int = Color.GRAY

    var items: List<MainMenuItem> = initialItems
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private var calibriTypefaceCache: Typeface? = null

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return items[position].id.toLong()
    }

    fun setTextColors(primary: Int, secondary: Int) {
        textColorPrimary = primary
        textColorSecondary = secondary
        notifyDataSetChanged()
    }

    private fun getTypeface(context: Context): Typeface {
        return calibriTypefaceCache ?: Typeface.createFromAsset(context.assets, "calibri.ttf")
            .apply {
                calibriTypefaceCache = this
            }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val context = parent.context
        val inflater = LayoutInflater.from(parent.context)
        val itemView = convertView
            ?: inflater.inflate(R.layout.menu_list_item, parent, false).also {
                it.tag = ViewHolder(it).apply {
                    txt.typeface = getTypeface(context)
                    pro.typeface = getTypeface(context)
                }
            }

        val item = items[position]
        val holder: ViewHolder = itemView.tag as ViewHolder

        val baseColor = if (item.showLock) textColorSecondary else textColorPrimary
        holder.txt.text = item.title
        holder.img.setImageResource(item.image)
        ColorFilter(baseColor).applyTo(holder.img)

        holder.txt.setTextColor(baseColor)
        holder.lock.isVisible = item.showLock
        holder.pro.isVisible = item.showLock

        return itemView
    }

    private class ViewHolder(view: View) {
        val txt: TextView = view.findViewById(R.id.item_title)
        val img: ImageView = view.findViewById(R.id.item_image)
        val lock: ImageView = view.findViewById(R.id.item_lock)
        val pro: TextView = view.findViewById(R.id.item_pro)
    }

}