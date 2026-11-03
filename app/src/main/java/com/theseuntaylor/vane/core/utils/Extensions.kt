package com.theseuntaylor.vane.core.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun String.returnTime(): String {
    val parsedDate = LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
    return parsedDate.format(DateTimeFormatter.ofPattern("HH:mm"))
}