package com.willeypianotuning.toneanalyzer.store.db

import androidx.room.TypeConverter
import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle
import com.willeypianotuning.toneanalyzer.sync.json.TemperamentSerializer
import com.willeypianotuning.toneanalyzer.sync.json.TuningStyleSerializer
import org.json.JSONObject
import java.util.*

class TypeSerializers {
    private val temperamentSerializer = TemperamentSerializer()
    private val styleSerializer = TuningStyleSerializer()

    @TypeConverter
    fun dateToLong(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun longToDate(value: Date?): Long? {
        return value?.time
    }

    @TypeConverter
    fun doubleArrayToString(array: DoubleArray): String {
        return array.joinToString(", ")
    }

    @TypeConverter
    fun stringToDoubleArray(array: String): DoubleArray {
        return array.split(", ").map { it.trim().toDouble() }.toDoubleArray()
    }

    @TypeConverter
    fun doubleMatrixToString(matrix: Array<DoubleArray>): String {
        return matrix.joinToString(";") { it.joinToString(", ") }
    }

    @TypeConverter
    fun stringToDoubleMatrix(matrix: String): Array<DoubleArray> {
        return matrix.split(";").map { array ->
            array.split(", ").map { it.trim().toDouble() }.toDoubleArray()
        }.toTypedArray()
    }

    @TypeConverter
    fun temperamentToString(temperament: Temperament?): String? {
        return if (temperament == null) null else temperamentSerializer.toJson(temperament)
            .toString()
    }

    @TypeConverter
    fun stringToTemperament(json: String?): Temperament? {
        return if (json == null) null else temperamentSerializer.fromJson(JSONObject(json))
    }

    @TypeConverter
    fun styleToString(style: TuningStyle?): String? {
        return if (style == null) null else styleSerializer.toJson(style).toString()
    }

    @TypeConverter
    fun stringToStyle(json: String?): TuningStyle? {
        return if (json == null) null else styleSerializer.fromJson(JSONObject(json))
    }

}
