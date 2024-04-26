package com.willeypianotuning.toneanalyzer.ui.files

import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuningInfo
import java.util.*

class PianoTuningInfoComparator(private val sortType: Int, isAscending: Boolean) :
    Comparator<PianoTuningInfo?> {
    private val order: Int = if (isAscending) 1 else -1

    constructor() : this(SORT_DATE, false)

    override fun compare(lhs: PianoTuningInfo?, rhs: PianoTuningInfo?): Int {
        val compareName = (lhs?.name ?: "").compareTo(rhs?.name ?: "")
        val compareMake = (lhs?.make ?: "").compareTo(rhs?.make ?: "")
        val compareModel = (lhs?.model ?: "").compareTo(rhs?.model ?: "")
        val compareDate = (lhs?.lastModified ?: Date(0)).compareTo(rhs?.lastModified ?: Date(0))
        val sortBy: IntArray = when (sortType) {
            SORT_NAME -> intArrayOf(order * compareName, compareMake, compareModel, -compareDate)
            SORT_MAKE -> intArrayOf(
                order * compareMake,
                order * compareModel,
                compareName,
                -compareDate
            )
            SORT_DATE -> intArrayOf(order * compareDate, compareName, compareMake, compareModel)
            else -> intArrayOf()
        }
        for (aSortBy in sortBy) {
            if (aSortBy != 0) {
                return aSortBy
            }
        }
        return 0
    }

    companion object {
        const val SORT_NAME = 0
        const val SORT_MAKE = 1
        const val SORT_DATE = 2
    }
}