package com.jjrapps.aquihaytomate.data.local.db

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * `java.time.LocalDate` ↔ ISO `TEXT`.
 *
 * `java.time` needs no desugaring here: `minSdk` is 31 and the API landed in 26. ISO text rather than
 * an epoch day so the column is readable in a database dump and sorts correctly as a string, which is
 * what the daily `GROUP BY` relies on.
 */
class LocalDateConverter {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
}
