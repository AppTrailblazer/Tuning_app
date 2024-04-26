package com.willeypianotuning.toneanalyzer.sync.json

import org.json.JSONArray

fun JSONArray.toDoubleArray(): DoubleArray {
    val arr = DoubleArray(length())
    for (i in 0 until length()) {
        arr[i] = getDouble(i)
    }
    return arr
}

fun JSONArray.toFloatArray(): FloatArray {
    val arr = FloatArray(length())
    for (i in 0 until length()) {
        arr[i] = getDouble(i).toFloat()
    }
    return arr
}

fun JSONArray.toIntArray(): IntArray {
    val arr = IntArray(length())
    for (i in 0 until length()) {
        arr[i] = getInt(i)
    }
    return arr
}

fun JSONArray.toFloatMatrix(): Array<FloatArray> {
    val arr = arrayOfNulls<FloatArray>(length())
    for (i in 0 until length()) {
        arr[i] = getJSONArray(i).toFloatArray()
    }
    return arr.requireNoNulls()
}

fun JSONArray.toDoubleMatrix(): Array<DoubleArray> {
    val arr = arrayOfNulls<DoubleArray>(length())
    for (i in 0 until length()) {
        arr[i] = getJSONArray(i).toDoubleArray()
    }
    return arr.requireNoNulls()
}

fun FloatArray.toJsonArray(): JSONArray {
    val jsonArray = JSONArray()
    for (v in this) {
        jsonArray.put(v)
    }
    return jsonArray
}

fun DoubleArray.toJsonArray(): JSONArray {
    val jsonArray = JSONArray()
    for (v in this) {
        jsonArray.put(v)
    }
    return jsonArray
}

fun IntArray.toJsonArray(): JSONArray {
    val jsonArray = JSONArray()
    for (v in this) {
        jsonArray.put(v)
    }
    return jsonArray
}

fun Array<DoubleArray>.toJsonArray(): JSONArray {
    val jsonMatrix = JSONArray()
    for (array in this) {
        jsonMatrix.put(array.toJsonArray())
    }
    return jsonMatrix
}

fun Array<FloatArray>.toJsonArray(): JSONArray {
    val jsonMatrix = JSONArray()
    for (array in this) {
        jsonMatrix.put(array.toJsonArray())
    }
    return jsonMatrix
}
