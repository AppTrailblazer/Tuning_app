package com.willeypianotuning.toneanalyzer.ui.tuning.temperament.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.willeypiano.libs.lazystring.LazyString
import com.willeypiano.libs.lazystring.asLazyString
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament

class TemperamentListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var items: List<ListItem> = emptyList()
    private var temperamentClickListener: TemperamentClickListener? = null

    fun setItems(temperaments: List<Temperament>) {
        val newItems = arrayListOf<ListItem>()
        newItems.add(TemperamentListItem(Temperament.EQUAL))
        val categories: MutableList<String?> = ArrayList()
        for ((_, _, _, category) in temperaments) {
            if (category != null && !categories.contains(category)) {
                categories.add(category)
            }
        }
        for (category in categories) {
            newItems.add(TemperamentCategoryListItem(category?.asLazyString()))
            for (temperament in temperaments) {
                if (temperament.category != null && temperament.category == category) {
                    newItems.add(TemperamentListItem(temperament))
                }
            }
        }
        newItems.add(TemperamentCategoryListItem(LazyString.Res(R.string.tuning_style_category_custom)))
        for (temperament in temperaments) {
            if (temperament.category == null) {
                newItems.add(TemperamentListItem(temperament))
            }
        }
        items = newItems
        notifyDataSetChanged()
    }

    fun setTemperamentClickListener(temperamentClickListener: TemperamentClickListener?) {
        this.temperamentClickListener = temperamentClickListener
    }

    override fun getItemViewType(position: Int): Int {
        if (items[position] is TemperamentListItem) {
            return VIEW_TYPE_TEMPERAMENT
        } else if (items[position] is TemperamentCategoryListItem) {
            return VIEW_TYPE_TEMPERAMENT_CATEGORY
        }
        throw IllegalArgumentException("Unsupported view type")
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        when (viewType) {
            VIEW_TYPE_TEMPERAMENT -> return TemperamentViewHolder(
                inflater.inflate(
                    R.layout.list_item_temperament,
                    viewGroup,
                    false
                )
            )

            VIEW_TYPE_TEMPERAMENT_CATEGORY -> return TemperamentCategoryViewHolder(
                inflater.inflate(
                    R.layout.list_item_category_header,
                    viewGroup,
                    false
                )
            )
        }
        throw IllegalArgumentException("Unknown type of view $viewType")
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_TEMPERAMENT -> (viewHolder as TemperamentViewHolder).bind(items[position] as TemperamentListItem)
            VIEW_TYPE_TEMPERAMENT_CATEGORY -> (viewHolder as TemperamentCategoryViewHolder).bind(
                items[position] as TemperamentCategoryListItem
            )
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private abstract inner class ListItem
    private inner class TemperamentListItem(val temperament: Temperament) : ListItem()

    private inner class TemperamentCategoryListItem(val categoryName: LazyString?) : ListItem()

    private inner class TemperamentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val temperamentNameTextView: TextView =
            itemView.findViewById(R.id.temperamentNameTextView)
        private val temperamentYearTextView: TextView =
            itemView.findViewById(R.id.temperamentYearTextView)

        fun bind(item: TemperamentListItem) {
            temperamentNameTextView.text = item.temperament.name
            temperamentYearTextView.text = item.temperament.year
        }

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    temperamentClickListener?.onTemperamentClicked((items[position] as TemperamentListItem).temperament)
                }
            }
        }
    }

    private inner class TemperamentCategoryViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val categoryNameTextView: TextView =
            itemView.findViewById(R.id.categoryNameTextView)

        fun bind(item: TemperamentCategoryListItem) {
            categoryNameTextView.text = item.categoryName?.resolve(itemView.context)
        }
    }

    companion object {
        private const val VIEW_TYPE_TEMPERAMENT = 1
        private const val VIEW_TYPE_TEMPERAMENT_CATEGORY = 2
    }
}