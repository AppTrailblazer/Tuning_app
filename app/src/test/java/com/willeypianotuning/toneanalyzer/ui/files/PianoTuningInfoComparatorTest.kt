package com.willeypianotuning.toneanalyzer.ui.files

import com.willeypianotuning.toneanalyzer.audio.enums.PianoType
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuningInfo
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.test.assertEquals

class PianoTuningInfoComparatorTest {
    private val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

    private fun dateFrom(str: String): Date {
        return requireNotNull(df.parse(str))
    }

    private val tuning1 = PianoTuningInfo(
        id = "1",
        name = "Custom 2020-01-01",
        make = "Bösendorfer",
        model = "M1",
        type = PianoType.UNSPECIFIED,
        lastModified = dateFrom("2020-02-03T12:00:00.123Z"),
    )
    private val tuning2 = PianoTuningInfo(
        id = "2",
        name = "Custom 2020-02-01",
        make = "Bechstein",
        model = "U1",
        type = PianoType.OTHER,
        lastModified = dateFrom("2020-03-04T15:00:00.123Z"),
    )
    private val items = listOf(tuning1, tuning2)

    @Test
    fun compareByDateAsc() {
        val sortedItems =
            items.sortedWith(FileSortOrder.getComparator(FileSortOrder.DATE_ASCENDING))
        assertEquals(sortedItems[0].id, "1")
    }

    @Test
    fun compareByDateDesc() {
        val sortedItems =
            items.sortedWith(FileSortOrder.getComparator(FileSortOrder.DATE_DESCENDING))
        assertEquals(sortedItems[0].id, "2")
    }

    @Test
    fun compareByMakeAsc() {
        val sortedItems =
            items.sortedWith(FileSortOrder.getComparator(FileSortOrder.MAKE_ASCENDING))
        assertEquals(sortedItems[0].id, "2")
    }

    @Test
    fun compareByMakeDesc() {
        val sortedItems =
            items.sortedWith(FileSortOrder.getComparator(FileSortOrder.MAKE_DESCENDING))
        assertEquals(sortedItems[0].id, "1")
    }
}