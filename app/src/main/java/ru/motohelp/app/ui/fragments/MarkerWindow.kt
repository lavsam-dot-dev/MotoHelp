package ru.motohelp.app.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.InfoWindow
import ru.motohelp.app.App
import ru.motohelp.app.BuildConfig
import ru.motohelp.app.R
import ru.motohelp.app.data.AppConstants.ROAD
import ru.motohelp.app.data.AppConstants.ROUTESTEPS
import ru.motohelp.app.data.MarkerPoint
import ru.motohelp.app.utils.PointState
import ru.motohelp.app.utils.PointState.*
import ru.motohelp.app.utils.getFullAddress
import ru.motohelp.app.utils.isDarkTheme
import ru.motohelp.app.utils.setDateTimeMarketPoint

class MarkerWindow(
    mapView: MapView,
    private val markerPoint: MarkerPoint,
    activityFragment: FragmentActivity
) : InfoWindow(R.layout.marker_info_window, mapView) {

    private val TAG = "happy"
    private val activityLocal = activityFragment
    private val user by lazy { FirebaseAuth.getInstance().currentUser }

    val mRoadNodeMarkers = FolderOverlay()

    override fun onOpen(item: Any?) {
        closeAllInfoWindowsOn(mapView)

        val helpGoButton = mView.findViewById<ImageButton>(R.id.help_go_button)
        val editButton = mView.findViewById<ImageButton>(R.id.edit_button)
        val deleteButton = mView.findViewById<ImageButton>(R.id.delete_button)

        if (markerPoint.pointState == Checked) {

            helpGoButton.background.setColorFilter(
                activityLocal.resources.getColor(R.color.accident_go_button),
                PorterDuff.Mode.MULTIPLY
            )
            val textHelpOnRoad = mView.findViewById<TextView>(R.id.help_banner)
            textHelpOnRoad.isVisible = true
            textHelpOnRoad.text = activityLocal.getText(R.string.help_on_road)
            textHelpOnRoad.setBackgroundColor(activityLocal.getColor(R.color.accident_go_button))
        }

        val addressInfoPoint = getFullAddress(markerPoint.lat, markerPoint.lon, 1, mapView.context)
        val textAddressEvent = mView.findViewById<TextView>(R.id.address_event)
        textAddressEvent.text = addressInfoPoint

        val serviceInfoMarkerPoint = setDateTimeMarketPoint(markerPoint, mapView.context)
        val serviceInfo = mView.findViewById<TextView>(R.id.service_info)
        serviceInfo.text = serviceInfoMarkerPoint

        val accidentDescriptionInfo =
            markerPoint.accidentDescription?.let { mapView.context.resources.getString(it.stringResId) }

        mView.findViewById<TextView>(R.id.accident_desc_info).text = accidentDescriptionInfo

        val severityAccidentInfo =
            markerPoint.severityAccident?.let { mapView.context.resources.getString(it.stringResId) }

        mView.findViewById<TextView>(R.id.severity_accident_info).text = severityAccidentInfo

        val statusAmbulance = mView.findViewById<TextView>(R.id.call_ambulance_car_info)
        if (markerPoint.callAmbulance) {
            statusAmbulance.text = activityLocal.getString(R.string.yes_call_ambulance)
        } else {
            statusAmbulance.text = activityLocal.getString(R.string.no_call_ambulance)
        }

        val accidentNoteText = mView.findViewById<TextView>(R.id.note_accident_info)

        accidentNoteText.text = markerPoint.accidentNotes


        if (user?.isAnonymous == true) {
            helpGoButton.visibility = View.GONE
            editButton.visibility = View.GONE
            deleteButton.visibility = View.GONE
            if (!accidentNoteText.text.toString().isNullOrEmpty()) {
                accidentNoteText.visibility = View.VISIBLE
            } else {
                accidentNoteText.visibility = View.GONE
            }
        }

        helpGoButton.setOnClickListener {
            val pointStateChange: PointState = if (markerPoint.pointState != Checked) {
                buildRoadToAccident()
                Checked
            } else {
                deleteRouteToAccident()
                Base
            }
            val markerPointChangeState = MarkerPoint(
                markerPoint.currentUser,
                markerPoint.accidentType,
                markerPoint.lat,
                markerPoint.lon,
                markerPoint.timestamp,
                pointStateChange,
                markerPoint.accidentDescription,
                markerPoint.severityAccident,
                markerPoint.callAmbulance,
                markerPoint.accidentNotes
            )
            saveConditionOfMarketPoint(markerPointChangeState)

            Log.d(TAG, "Статус после GO: ${markerPointChangeState.pointState}")

        }

        editButton.setOnClickListener {
// TODO вопрос про фрагмент - не будет ли утечка памяти при передаче activity?????
//            requireActivity().supportFragmentManager.apply {
            activityLocal.supportFragmentManager.apply {
                beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(
                        R.id.placeHolder,
                        MarkerInfoFragment.newInstance(Bundle().apply {
                            putParcelable(MarkerInfoFragment.BUNDLE_EXTRA, markerPoint)
                        })
                    )
                    .addToBackStack("")
                    .commit()
            }
        }

        deleteButton.setOnClickListener {
            deleteMarkerFromView()
        }

        mView.setOnClickListener {
            close()
        }
    }

    override fun onClose() {
        //
    }

    private fun deleteMarkerFromView() {

        val pointStateChange = NotRelevant
        val markerPointChangeState = MarkerPoint(
            markerPoint.currentUser,
            markerPoint.accidentType,
            markerPoint.lat,
            markerPoint.lon,
            markerPoint.timestamp,
            pointStateChange,
            markerPoint.accidentDescription,
            markerPoint.severityAccident,
            markerPoint.callAmbulance,
            markerPoint.accidentNotes
        )
//        Log.d(TAG, "Удаление точки : ${markerPointChangeState.pointState}")

        saveConditionOfMarketPoint(markerPointChangeState)
    }

    private fun buildRoadToAccident() {

        deleteRouteToAccident()

        val endRoad = markerPoint

        val startPoint = GeoPoint(startRoad.latitude, startRoad.longitude)
        val endPoint = GeoPoint(endRoad.lat, endRoad.lon)

        val roadManager: RoadManager = OSRMRoadManager(mMapView.context, BuildConfig.BUILD_TYPE)
        val waypoints = ArrayList<GeoPoint>()
        waypoints.add(GeoPoint(startPoint))
        waypoints.add(GeoPoint(endPoint))
        val road = roadManager.getRoad(waypoints)
        if (road.mStatus != Road.STATUS_OK)
            Toast.makeText(
                mView.context,
                "${this.activityLocal.resources.getString(R.string.enter_phone_number)} - status:" + road.mStatus,
                Toast.LENGTH_SHORT
            ).show();
        val roadOverlay = RoadManager.buildRoadOverlay(road)

        roadOverlay.id = ROAD
        if (isDarkTheme(activityLocal)) {
            roadOverlay.outlinePaint.color = Color.GREEN
        } else {
            roadOverlay.outlinePaint.color = Color.BLUE
        }
        roadOverlay.outlinePaint.strokeWidth = 13f
        //roadOverlay.outlinePaint.alpha = 130
        mapView.overlays.add(roadOverlay)

        //3. Шаги по карте
        mapView.overlays.add(mRoadNodeMarkers)
        mRoadNodeMarkers.name = ROUTESTEPS
        mapView.getOverlays().add(mRoadNodeMarkers)

        val nodeIcon = ResourcesCompat.getDrawable(mView.resources, R.drawable.circle_marker, null)
        for (i in 0 until road.mNodes.size) {
            val node: RoadNode = road.mNodes.get(i)
            val nodeMarker = Marker(mapView)
            nodeMarker.position = node.mLocation
            nodeMarker.icon = nodeIcon

            //4. наполняем подсказками маршрут
            nodeMarker.title = "${activityLocal.getString(R.string.road_step_number)} $i"
            nodeMarker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_CENTER)
            nodeMarker.snippet = node.mInstructions
            nodeMarker.subDescription =
                Road.getLengthDurationText((mMapView.context), node.mLength, node.mDuration)
            val iconContinue =
                ResourcesCompat.getDrawable(mView.resources, R.drawable.motorbike_32, null)
            nodeMarker.image = iconContinue
            // добавили подсказки
            mRoadNodeMarkers.add(nodeMarker)

        }
        closeAllInfoWindowsOn(mapView)
        mapView.invalidate()
    }

    private fun deleteRouteToAccident() {

        mapView.overlays.forEach {
            if (it is Polyline && it.id == ROAD) {
                mapView.overlays.remove(it)
                mapView.invalidate()
            }
            if (it is FolderOverlay && it.name == "Route Steps") {
                mapView.overlays.remove(it)
                mapView.invalidate()
            }
        }
        closeAllInfoWindowsOn(mapView)
        mapView.invalidate()
    }

    private fun saveConditionOfMarketPoint(markerPoint: MarkerPoint) {

        App().dbGeoPoints.child(markerPoint.timestamp.toString())
            .setValue(markerPoint)
            .addOnSuccessListener {

                Log.d(TAG, "Удачно записали в infoWindow")

            }.addOnFailureListener {

                Log.d(TAG, "Не удалось записать в infoWindow")
            }
        closeAllInfoWindowsOn(mapView)
        mapView.invalidate()
    }


}


