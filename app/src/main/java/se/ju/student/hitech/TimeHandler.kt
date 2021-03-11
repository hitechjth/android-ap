package se.ju.student.hitech

import java.sql.Time
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class TimeHandler {

    fun getLocalZoneTimestamp(): Timestamp {
        return Timestamp
            .valueOf(
                DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss.ss")
                    .withZone(ZoneOffset.UTC)
                    .format(Instant.now())
            ).convertToLocalTime()!!
    }
}

fun String.convertTimeToTimestamp(): Timestamp? {

    return Timestamp.valueOf(
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.ss")
            .withZone(ZoneId.systemDefault())
            .format(java.sql.Date(this.toLong()).toInstant())
    )
}

fun Timestamp.convertToLocalTime(): Timestamp? {

    val dateFormat = "yyyy-MM-dd HH:mm:ss"

    val parser = SimpleDateFormat(dateFormat, Locale.getDefault())
    parser.timeZone = TimeZone.getTimeZone("UTC")
    val date = parser.parse(this.toString())!!

    val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
    formatter.timeZone = TimeZone.getDefault()
    val localDate = formatter.format(date)

    return Timestamp.valueOf(localDate.toString())

}