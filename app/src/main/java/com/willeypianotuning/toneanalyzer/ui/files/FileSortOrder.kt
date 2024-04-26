package com.willeypianotuning.toneanalyzer.ui.files

object FileSortOrder {
    const val NAME_ASCENDING = 0
    const val NAME_DESCENDING = 10
    const val MAKE_ASCENDING = 1
    const val MAKE_DESCENDING = 11
    const val DATE_ASCENDING = 2
    const val DATE_DESCENDING = 12

    fun getComparator(sortOrder: Int): PianoTuningInfoComparator {
        when (sortOrder) {
            NAME_ASCENDING -> return PianoTuningInfoComparator(
                PianoTuningInfoComparator.SORT_NAME,
                true
            )
            NAME_DESCENDING -> return PianoTuningInfoComparator(
                PianoTuningInfoComparator.SORT_NAME,
                false
            )
            MAKE_ASCENDING -> return PianoTuningInfoComparator(
                PianoTuningInfoComparator.SORT_MAKE,
                true
            )
            MAKE_DESCENDING -> return PianoTuningInfoComparator(
                PianoTuningInfoComparator.SORT_MAKE,
                false
            )
            DATE_ASCENDING -> return PianoTuningInfoComparator(
                PianoTuningInfoComparator.SORT_DATE,
                true
            )
            DATE_DESCENDING -> return PianoTuningInfoComparator(
                PianoTuningInfoComparator.SORT_DATE,
                false
            )
        }
        return PianoTuningInfoComparator(PianoTuningInfoComparator.SORT_DATE, true)
    }
}