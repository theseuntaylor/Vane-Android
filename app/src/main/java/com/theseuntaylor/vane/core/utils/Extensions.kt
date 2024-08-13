package com.theseuntaylor.vane.core.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
fun String.returnTime(): String {
    val parsedDate = LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
    return parsedDate.format(DateTimeFormatter.ofPattern("HH:mm"))
}