package com.akeshari.splitblind.data.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class Converters {
    @TypeConverter
    fun fromStringList(value: String): List<String> = Json.decodeFromString(value)
    @TypeConverter
    fun toStringList(list: List<String>): String = Json.encodeToString(list)
}
