package ru.motohelp.app

import android.app.Application
import android.content.Context
import androidx.fragment.app.Fragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import ru.motohelp.app.data.AppConstants

class App : Application() {

    private val firebaseDB: FirebaseDatabase by lazy { Firebase.database("https://moto112-45b33-default-rtdb.asia-southeast1.firebasedatabase.app/") }
    val dbGeoPoints: DatabaseReference by lazy { firebaseDB.getReference(AppConstants.REFERENCE_GROUP) }
}

val Context.app: App get() = applicationContext as App
val Fragment.app: App get() = requireContext().applicationContext as App