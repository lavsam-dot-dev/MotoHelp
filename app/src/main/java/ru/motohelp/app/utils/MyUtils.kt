package ru.motohelp.app.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import ru.motohelp.app.App
import ru.motohelp.app.R
import ru.motohelp.app.data.MarkerPoint
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

fun Toast.showCustomToast(message: String, color: Int, activity: Activity) {
    val layout = activity.layoutInflater.inflate(
        R.layout.custom_toast,
        activity.findViewById(R.id.toast_container)
    )
    val toastColor = layout.findViewById<FrameLayout>(R.id.button_accent_border)
    when (color) {
        0 -> toastColor.setBackgroundColor(Color.GREEN);
        1 -> toastColor.setBackgroundColor(Color.GRAY);
        2 -> toastColor.setBackgroundColor(Color.RED);
    }

    val textView = layout.findViewById<TextView>(R.id.toast_text)
    textView.text = message

    this.apply {
        setGravity(Gravity.BOTTOM, 0, 40)
        duration = Toast.LENGTH_SHORT
        view = layout
        show()
    }
}

fun dateTimeFromTimeStamp(timestamp: Long): String {
//    return SimpleDateFormat("HH:mm:ss dd.MM.yy", Locale.getDefault()).format(timestamp) // старая форма
    return SimpleDateFormat("dd.MM.yy / HH:mm", Locale.getDefault()).format(timestamp)
}

fun dateTimeEvent(): String { // функция даты/времени для якоря маркера
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd yyyy, hh:mm:ss a"))
}

private fun dayFromTimeStamp(timestamp: Long): String {
    return SimpleDateFormat("dd-MM-yy", Locale.UK).format(timestamp)
}

private fun timeFromTimeStamp(timestamp: Long): String {
    return SimpleDateFormat("HH-mm-ss", Locale.UK).format(timestamp)
}

// установка широты, долготы, даты, времени в один String
fun setServiceInfoMarkerPoint(markerPoint: MarkerPoint): String {
    val serviceInfoMarkerPoint = if (markerPoint.timestamp != null) {
        "lan: ${markerPoint.lat},\nlon: ${markerPoint.lon}\ntime: ${
            dateTimeFromTimeStamp(markerPoint.timestamp)
        }"
    } else {
        "lan: ${markerPoint.lat}, \nlon: ${markerPoint.lat}\ntime: ${dateTimeEvent()}"
    }
    return serviceInfoMarkerPoint
}

// установка даты, времени из маркера в один String
fun setDateTimeMarketPoint(markerPoint: MarkerPoint, context: Context): String {
    val textDataTime = context.resources.getText(R.string.date_time)
    val serviceInfoMarkerPoint = if (markerPoint.timestamp != null) {
        "$textDataTime ${dateTimeFromTimeStamp(markerPoint.timestamp)}"
    } else {
        "$textDataTime ${dateTimeEvent()}"
    }
    return serviceInfoMarkerPoint
}

// Возвращает сколько раз строка символов из паттерна встречалась в строке
fun countMatches(string: String, pattern: String): Int {
    var index = 0
    var count = 0

    while (true) {
        index = string.indexOf(pattern, index)
        index += if (index != -1) {
            count++
            pattern.length
        } else {
            return count
        }
    }
}
