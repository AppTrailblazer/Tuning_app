package com.willeypianotuning.toneanalyzer.ui.settings.weights.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.willeypiano.libs.lazystring.LazyString
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle

class TuningStyleListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ListItem> = emptyList()
    var tuningStyleClickListener: ((TuningStyle) -> Unit)? = null

    fun setItems(styles: List<TuningStyle>) {
        val newItems = ArrayList<ListItem>()
        newItems.add(TuningStyleListItem(TuningStyle.DEFAULT))
        for (style in styles.filter { !it.mutable }) {
            newItems.add(TuningStyleListItem(style))
        }
        newItems.add(TuningStyleCategoryListItem(LazyString.Res(R.string.tuning_style_category_custom)))
        for (style in styles.filter { it.mutable }) {
            newItems.add(TuningStyleListItem(style))
        }

        this.items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        if (items[position] is TuningStyleListItem) {
            return VIEW_TYPE_STYLE
        } else if (items[position] is TuningStyleCategoryListItem) {
            return VIEW_TYPE_STYLE_CATEGORY
        }
        throw IllegalArgumentException("Unsupported view type")
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        when (viewType) {
            VIEW_TYPE_STYLE -> return TuningStyleViewHolder(
                inflater.inflate(
                    R.layout.list_item_tuning_style,
                    viewGroup,
                    false
                )
            )
            VIEW_TYPE_STYLE_CATEGORY -> return CategoryViewHolder(
                inflater.inflate(
                    R.layout.list_item_category_header,
                    viewGroup,
                    false
                )
            )
        }
        throw IllegalArgumentException("Unsupported view type")
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_STYLE -> (viewHolder as TuningStyleViewHolder).bind(items[position] as TuningStyleListItem)
            VIEW_TYPE_STYLE_CATEGORY -> (viewHolder as CategoryViewHolder).bind(items[position] as TuningStyleCategoryListItem)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private abstract inner class ListItem

    private inner class TuningStyleListItem(val style: TuningStyle) : ListItem()

    private inner class TuningStyleCategoryListItem(val categoryName: LazyString) : ListItem()

    private inner class TuningStyleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.styleNameTextView)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && tuningStyleClickListener != null) {
                    tuningStyleClickListener?.invoke((items[position] as TuningStyleListItem).style)
                }
            }
        }

        fun bind(item: TuningStyleListItem) {
            nameTextView.text = item.style.name
        }
    }

    private inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.categoryNameTextView)

        fun bind(item: TuningStyleCategoryListItem) {
            nameTextView.text = item.categoryName.resolve(itemView.context)
        }
    }

    companion object {
        private const val VIEW_TYPE_STYLE = 1
        private const val VIEW_TYPE_STYLE_CATEGORY = 2
    }
}
