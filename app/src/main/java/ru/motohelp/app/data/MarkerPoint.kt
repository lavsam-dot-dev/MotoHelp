package ru.motohelp.app.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.motohelp.app.utils.AccidentDescription
import ru.motohelp.app.utils.AccidentType
import ru.motohelp.app.utils.PointState
import ru.motohelp.app.utils.SeverityAccident

@Parcelize
data class MarkerPoint(
    val currentUser: String = "",
    val accidentType: AccidentType? = AccidentType.Accident,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val timestamp: Long? = 0L,
    val pointState: PointState? = PointState.Base,
    val accidentDescription: AccidentDescription? = AccidentDescription.None,
    val severityAccident: SeverityAccident? = SeverityAccident.None,
    val callAmbulance: Boolean = false,
    val accidentNotes: String? = null
) : Parcelable