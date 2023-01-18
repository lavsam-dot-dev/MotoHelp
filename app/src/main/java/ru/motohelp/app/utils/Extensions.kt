package ru.motohelp.app.utils


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.telephony.SmsManager
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import ru.motohelp.app.R
import java.util.*


fun Fragment.showFragment(f: Fragment) {
    (activity as AppCompatActivity).supportFragmentManager
        .beginTransaction()
        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        .addToBackStack(f.id.toString())
        //.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        .replace(R.id.placeHolder, f)
        .commit()
}



fun AppCompatActivity.showFragment(f: Fragment) {
    supportFragmentManager
        .beginTransaction()
        .addToBackStack(f.id.toString())
        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        .replace(R.id.placeHolder, f)
        .commit()
}

// нажатие для MotionLayout - анимация, а потом клик
@SuppressLint("ClickableViewAccessibility")
fun View.setOnClick(clickEvent: () -> Unit) {
    this.setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            clickEvent.invoke()
        }
        false
    }
}

// анимация FAB кнопок быстрого маркера - развернуть
fun ExtendedFloatingActionButton.showFabMarkerFast() {
    this.apply {
        animate().cancel()
        scaleX = 0f
        scaleY = 0f
        alpha = 0f
        isVisible = true
        animate().setDuration(400).scaleX(1f).scaleY(1f).alpha(0.8f).start()
    }
}

// анимация FAB кнопок быстрого маркера - свернуть
fun ExtendedFloatingActionButton.hideFabMarkerFast() {
    this.apply {
        animate().cancel()
        scaleX = 1f
        scaleY = 1f
        alpha = 0.8f
        animate().setDuration(300).scaleX(0f).scaleY(0f).alpha(0f).start()
        isVisible = false
    }
}

// получение списка адресов
// в замен val addresses = geocoder.getFromLocation(markerPoint.lat, markerPoint.lon, 1)
fun getAddressResult(lat: Double, lon: Double, int: Int, context: Context): List<Address>? {
    var geocoder = Geocoder(context, Locale.getDefault())
    val addresses =
        geocoder.getFromLocation(lat, lon, 1)
    return addresses
}

// получение списка адреса
// в замен val addressPoint = addresses?.get(0)?.getAddressLine(0)
fun fullAddress(addresses: List<Address>?): String? {
    val addressPoint = addresses?.get(0)?.getAddressLine(0)
    return addressPoint
}

// полный адрес, если он есть, получаем в виде строки? int = 1
fun getFullAddress(lat: Double, lon: Double, int: Int, context: Context): String? {

    val textMessageError = context.resources.getText(R.string.message_error_geocoder)
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        val addressPoint = addresses?.get(0)?.getAddressLine(0)
        addressPoint
    } catch (e:Exception) {
        Toast.makeText(
            context,
            "$textMessageError",
            Toast.LENGTH_LONG
        ).show()
        "$textMessageError"
    }
}
fun mySOSMessage(phone: String, message: String, context: Context) {
    val message = message
    val textMessageSent = context.resources.getText(R.string.message_sent)
    val textMessageOnError = context.resources.getText(R.string.message_error_sos)
    try {
        val smsManager: SmsManager = SmsManager.getDefault()
        val parts: ArrayList<String> = smsManager.divideMessage(message)
        // длинное СМС:
        smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
        // одиночное СМС: smsManager.sendTextMessage(phone, null, message, null, null)
        Toast.makeText(context, "$textMessageSent", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        // в блоке catch мы показываем всплывающее сообщение об ошибке
        Toast.makeText(
            context,
            "$textMessageOnError",
            Toast.LENGTH_LONG
        ).show()
    }
}


fun shareUrl(context: Context, addressText: String) {
    // отправляем в гугл карты
    //val gmmIntentUri = Uri.parse("geo:${addressText}")
    val gmmIntentUri = Uri.parse("geo:0,0?q=${addressText}(${context.resources.getString(R.string.text_for_share)})")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
    mapIntent.setPackage("com.google.android.apps.maps")
    context.startActivity(mapIntent)
}

// определяем какая системная сейчас тема
fun isDarkTheme(activity: Activity): Boolean {
    return activity.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

/*
fun openYandexMap(lat:Double, lon: Double, context: Context){
    val uri: Uri = Uri.parse("yandexmaps://maps.yandex.ru/?ll=$lat,$lon&z=12")
    var intent = Intent(Intent.ACTION_VIEW, uri)
// Проверяем, установлено ли хотя бы одно приложение, способное выполнить это действие.
    //val packageManager: PackageManager = getPackageManager()
    val packageManager: PackageManager = context.packageManager
    val activities = packageManager.queryIntentActivities(intent, 0)
    val isIntentSafe = activities.size > 0
    if (isIntentSafe) {
        startActivity(intent)
    } else {
// Открываем страницу приложения Яндекс.Карты в Google Play.
        intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("market://details?id=ru.yandex.yandexmaps")
        startActivity(intent)
    }
}*/
