package com.willeypianotuning.toneanalyzer.sync.json

import org.json.JSONObject
import java.io.File
import java.io.InputStream

abstract class ObjectSerializer<T> {
    fun fromFile(file: File): T {
        return file.inputStream().use {
            fromStream(it)
        }
    }

    fun fromStream(stream: InputStream): T {
        return fromJson(JSONObject(stream.bufferedReader().readText()))
    }

    abstract fun fromJson(json: JSONObject): T

    abstract fun toJson(obj: T): JSONObject
}