package ru.motohelp.app.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.library.BuildConfig
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsDisplay.HorizontalPosition
import org.osmdroid.views.CustomZoomButtonsDisplay.VerticalPosition
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import ru.motohelp.app.R
import ru.motohelp.app.app
import ru.motohelp.app.data.AppConstants.DEFAULT_LOCATIONS_LAT
import ru.motohelp.app.data.AppConstants.DEFAULT_LOCATIONS_LON
import ru.motohelp.app.data.AppConstants.DEFAULT_ZOOM
import ru.motohelp.app.data.AppConstants.MIN_ZOOM
import ru.motohelp.app.data.AppConstants.REQUEST_CHECK_SETTINGS
import ru.motohelp.app.data.MapViewModel
import ru.motohelp.app.data.MarkerPoint
import ru.motohelp.app.databinding.MapScreenFragmentBinding
import ru.motohelp.app.model.location.MyEventLocationSettingsChange
import ru.motohelp.app.utils.*
import timber.log.Timber
import java.sql.Timestamp
import java.util.*


lateinit var startRoad: Location


@SuppressLint("RestrictedApi")
class MapScreenFragment : Fragment(R.layout.map_screen_fragment) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient //смотрим здесь
    private lateinit var map: MapView
    private lateinit var mapControllerNew: IMapController
    private var activityResultLauncher: ActivityResultLauncher<Array<String>>
    private var lastLocation: Location? = null
    private var locationCallback: LocationCallback
    private var requestingLocationUpdates = false

    private var startPoint: GeoPoint = GeoPoint(DEFAULT_LOCATIONS_LAT, DEFAULT_LOCATIONS_LON)
    private var marker: Marker? = null
    private var path1: Polyline? = null

    private val locationRequest: LocationRequest by lazy { createLocationRequest() }

    private val preferences: SharedPreferences by lazy { PreferenceManager(requireContext()).sharedPreferences!! }

    private val user by lazy { FirebaseAuth.getInstance().currentUser }
    private val model: MapViewModel by activityViewModels()

    private val TAG = "happy"
    private var mAzimuthAngleSpeed = 0.0f

    private var _binding: MapScreenFragmentBinding? = null
    private val binding get() = _binding!!

    companion object {

        fun newInstance(): Fragment {
            val args = Bundle()
            val fragment = MapScreenFragment()
            fragment.arguments = args
            return fragment
        }
    }

    //    has comments
    init {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.let {
                    for (location in locationResult.locations) {
                        updateLocation(location)
                    }
                }
            }
        }

        this.activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            var allAreGranted = true
            for (b in result.values) {
                allAreGranted = allAreGranted && b
            }

            Timber.d("${resources.getString(R.string.gps_allowed)} $allAreGranted")

/*            Toast(requireActivity()).showCustomToast(
                "${resources.getString(R.string.gps_allowed)}", 0, requireActivity())*/

            if (allAreGranted) {
                initCheckLocationSettings()
                initMap() //если с настройками всё ок
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree()) //Init report type
        }
        getInstance().userAgentValue = BuildConfig.BUILD_TYPE
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        getInstance().load(
            requireActivity(),
            android.preference.PreferenceManager.getDefaultSharedPreferences(requireActivity())
        )

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        map = binding.mapView
//Выбор TileSource. Меняется во фрагменте настроек. Значения находятся в arrays.xml, задаются в settings_fragment.xml
        when (preferences.getString("MapType", "MAPNIK")) {
            "MAPNIK" -> {
                map.setTileSource(TileSourceFactory.MAPNIK)
                //map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS)
                //map.getOverlayManager().getTilesOverlay().setColorFilter(null)
            }
            "OpenTopo" -> map.setTileSource(TileSourceFactory.OpenTopo)
            "WIKIMEDIA" -> map.setTileSource(TileSourceFactory.WIKIMEDIA)
        }
        if (isDarkTheme(this.requireActivity())) {
            map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS)
        } else {
            map.getOverlayManager().getTilesOverlay().setColorFilter(null)
        }

        model.liveMarker.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            map.controller.animateTo(GeoPoint(it.lat, it.lon))
        }

        map.minZoomLevel = MIN_ZOOM
        binding.efAccident.isVisible = false
        binding.efAmbulance.isVisible = false
        binding.efBreakdown.isVisible = false

        map.isTilesScaledToDpi =
            preferences.getBoolean("tileScaleToDPI", true) // изменение масштаба карты
        map.setMultiTouchControls(true) // мультитач управление
        mapControllerNew = map.controller

        val appPerms = arrayOf( // массив разрешений
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
        )
        activityResultLauncher.launch(appPerms)

        binding.efEventMarkerAdd.setOnClickListener {
            binding.apply {
                efAmbulance.showFabMarkerFast()
                efBreakdown.showFabMarkerFast()
                efAccident.showFabMarkerFast()
                efEventMarkerAdd.hideFabMarkerFast()
            }
        }

        binding.efCurrentPosition.setOnClickListener {
            mapControllerNew.setCenter(startPoint)
            getPositionMarker().position = startPoint
            map.invalidate()
        }

//        model.getData().observe(viewLifecycleOwner, processDatabaseData())
//        model.getFreshData().observe(viewLifecycleOwner, processDatabaseData())

        model.getFreshRelevantData().observe(viewLifecycleOwner, processDatabaseData())

        // вызов быстрого маркера с выбором типа ДТП
        binding.apply {
            efBreakdown.setOnClick {
                addEvent(AccidentType.Breakdown)
            }
            efAccident.setOnClick {
                addEvent(AccidentType.Accident)
            }
            efAmbulance.setOnClick {
                addEvent(AccidentType.Ambulance)
            }
        }
        if (user?.isAnonymous == true) {
            binding.efEventMarkerAdd.isVisible = false
            binding.efSOS.isVisible = false
        } else {
            map.isClickable

            map.setOnTouchListener(OnTouchListener { _, motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {

                        binding.apply {
                            efAmbulance.hideFabMarkerFast()
                            efBreakdown.hideFabMarkerFast()
                            efAccident.hideFabMarkerFast()
                            efEventMarkerAdd.hideFabMarkerFast()
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        binding.efEventMarkerAdd.showFabMarkerFast()
                    }
                }
                false
            })

            binding.efSOS.setOnClickListener {
                val permissionCheck = ContextCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.SEND_SMS
                )
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    val phone = preferences.getString("EditTextPhone", "")
                    val message =
                        "${resources.getString(R.string.i_need_help)} https://www.google.com/maps/search/?api=1&query=${startPoint.latitude},${startPoint.longitude}"
                    //https://www.google.com/maps/search/?api=1&query=47.5951518%2C-122.3316393
                        //"${resources.getString(R.string.i_need_help)} https://yandex.ru/maps/?ll=${startPoint.latitude},${startPoint.longitude},z=13"
                    if (phone != null) {
                        mySOSMessage(phone, message, requireActivity())
                    } else {
                        Toast.makeText(
                            requireActivity(),
                            "${resources.getString(R.string.enter_phone_number)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    ActivityCompat.requestPermissions(
                        requireActivity(), arrayOf(Manifest.permission.SEND_SMS), 101
                    )
                }
            }

/*            val mReceive: MapEventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    return false
                }

                override fun longPressHelper(p: GeoPoint): Boolean {
//                    Toast.makeText(
//                        requireActivity(),
//                        p.latitude.toString() + " - " + p.longitude,
//                        Toast.LENGTH_LONG
//                    ).show()
                    val permissionCheck = ContextCompat.checkSelfPermission(
                        requireActivity(),
                        Manifest.permission.SEND_SMS
                    )
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        val phone = preferences.getString("EditTextPhone", "")
                        val message =
                            "Я в беде: https://yandex.ru/maps/?ll=${startPoint.latitude},${startPoint.longitude},z=13"
                        if (phone != null) {
                            mySOSMessage(phone, message, requireActivity())
                        } else {
                            Toast.makeText(
                                requireActivity(),
                                "Укажите в настройках номер телефона близкого человека",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        ActivityCompat.requestPermissions(
                            requireActivity(), arrayOf(Manifest.permission.SEND_SMS), 101
                        )
                    }
                    return true
                }
            }
            val overlayEvents = MapEventsOverlay(mReceive)
            map.overlays.add(overlayEvents)*/
        }
    }

    // отслеживаем изменения данных в БД
    private fun processDatabaseData(): Observer<List<MarkerPoint>> {
        val dataObserver = Observer<List<MarkerPoint>> { t ->

            map.overlays.forEach {
                if ((it is Marker) && (it.id != "MyLocation")) {
                    map.overlays.remove(it)
                    map.invalidate()
                }
            }

            t?.forEach { markerPoint ->
                val markerDB = Marker(map, requireContext())
                markerDB.apply {
                    id = markerPoint.timestamp.toString()
                    position.latitude = markerPoint.lat
                    position.longitude = markerPoint.lon
                    icon = when (markerPoint.accidentType) {
                        null -> ResourcesCompat.getDrawable(
                            resources, R.drawable.ico_moto_crash_36, null
                        )
                        AccidentType.Accident -> when (markerPoint.pointState) {
                            PointState.Checked -> ResourcesCompat.getDrawable(
                                resources, R.drawable.ico_moto_crash_36_help, null
                            )
                            PointState.Base, PointState.NotRelevant, null ->
                                ResourcesCompat.getDrawable(
                                    resources, R.drawable.ico_moto_crash_36, null
                                )
                        }
                        AccidentType.Ambulance -> when (markerPoint.pointState) {
                            PointState.Checked -> ResourcesCompat.getDrawable(
                                resources, R.drawable.ico_moto_help_36_help, null
                            )
                            PointState.Base, PointState.NotRelevant, null ->
                                ResourcesCompat.getDrawable(
                                    resources, R.drawable.ico_moto_help_36, null
                                )
                        }
                        AccidentType.Breakdown -> when (markerPoint.pointState) {
                            PointState.Checked ->
                                ResourcesCompat.getDrawable(
                                    resources, R.drawable.ico_moto_breakdown_36_help, null
                                )
                            PointState.Base, PointState.NotRelevant, null ->
                                ResourcesCompat.getDrawable(
                                    resources, R.drawable.ico_moto_breakdown_36, null
                                )
                        }
                    }

                    val infoWindow = activity?.let { MarkerWindow(map, markerPoint, it) }

                    this.infoWindow = infoWindow

                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                map.overlays.add(markerDB)
                map.invalidate()
            }
            map.invalidate()
        }
        return dataObserver
    }

    //    Функция для добавления и отправки маркера.
    private fun addEvent(accidentType: AccidentType) {
        user.let {
            if (!user?.email.isNullOrEmpty() or (user?.isAnonymous == false)) {

                val marker = Marker(map)
                marker.icon = when (accidentType) {
                    AccidentType.Breakdown -> ResourcesCompat.getDrawable(
                        resources, R.drawable.ico_moto_breakdown_36, null
                    )
                    AccidentType.Accident -> ResourcesCompat.getDrawable(
                        resources, R.drawable.ico_moto_crash_36, null
                    )
                    AccidentType.Ambulance -> ResourcesCompat.getDrawable(
                        resources, R.drawable.ico_moto_help_36, null
                    )
                }

                marker.position.latitude = startPoint.latitude
                marker.position.longitude = startPoint.longitude

                marker.title =
                    "lan: ${marker.position.latitude}, lon: ${marker.position.longitude}\n time: ${dateTimeEvent()}"
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                map.overlays.add(marker)
                map.invalidate()

//            Добавление маркера в БД. timestamp - id точки в БД.
                val timestamp = Timestamp(Date().time).time

                app.dbGeoPoints.child(timestamp.toString()).setValue(
                    MarkerPoint(
                        user?.email!!,
                        accidentType,
                        marker.position.latitude,
                        marker.position.longitude,
                        timestamp,
                        pointState = PointState.Base
                    )
                )
            }
        }
        binding.apply {
            efAmbulance.hideFabMarkerFast()
            efBreakdown.hideFabMarkerFast()
            efAccident.hideFabMarkerFast()
            efEventMarkerAdd.showFabMarkerFast()
        }
    }

    fun setMyLocation() {
        Toast.makeText(requireContext(), "Location", Toast.LENGTH_SHORT).show()
        mapControllerNew.setCenter(startPoint)
        getPositionMarker().position = startPoint
        map.invalidate()
    }

    //has comments
    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 500
            smallestDisplacement = 10f
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            maxWaitTime = 1000
        }

    }

    //has comments
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMsg(status: MyEventLocationSettingsChange) {
        if (status.on) {
            initMap()
        } else {
            Timber.i("Stop something")
        }
    }

    //
    private fun initLocation() { //call in create
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())//getActivity() = this
        readLastKnownLocation()
    }

    //has comments
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() { //используем в onPause
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    //has comments
    @SuppressLint("MissingPermission") //permission are checked before
    fun readLastKnownLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let { updateLocation(it) }
        }
    }

    // взял с документации библиотеки
    private fun initCheckLocationSettings() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(requireContext())
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
//            Toast(requireActivity()).showCustomToast(
//                resources.getString(R.string.init_check_location_settings_message_ok),
//                0,
//                requireActivity()
//            )
            Timber.d(resources.getString(R.string.init_check_location_settings_message_ok))
            MyEventLocationSettingsChange.globalState = true //default
            initMap()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {

                Timber.d(resources.getString(R.string.add_on_failure_listener))
                try {

                    exception.startResolutionForResult(
                        requireActivity(), REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {

                    Timber.d("Settings Location sendEx??")
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("Settings onActivityResult for $requestCode result $resultCode")
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                initMap()
            }
        }
    }

    //     обновляем текущую геопозицию
    fun updateLocation(newLocation: Location) {
        lastLocation = newLocation
        binding.tvDebugInfo.text = buildString {
            append("lat:${newLocation.latitude}\n")
            append("lon:${newLocation.longitude}\n")
            append("speed (m/sec):${newLocation.speed}\n")
        }
        binding.tvSpeedText.text = buildString {
            append(((newLocation.speed) * 60 * 60 / 1000).toInt())
            append("km")
        }
        startRoad = newLocation
        startPoint.longitude = newLocation.longitude
        startPoint.latitude = newLocation.latitude
        mapControllerNew.setCenter(startPoint)
        getPositionMarker().position = startPoint

        //когда скорость возрастает более 10 км
        if ((((newLocation.speed) * 60 * 60 / 1000)) >= 10) {
            mAzimuthAngleSpeed = newLocation.getBearing()
            map.setMapOrientation(-mAzimuthAngleSpeed)
        } else {
            map.setMapOrientation(0.0f)
        }
        map.invalidate()
    }

    //has comments
    private fun initMap() {
        initLocation()
        if (!requestingLocationUpdates) {
            requestingLocationUpdates = true
            startLocationUpdates()
        }

        val prefs: SharedPreferences = requireActivity().getSharedPreferences("MOTOHELP", Context.MODE_PRIVATE)

        var zoom = prefs.getFloat("MAP_ZOOM_LEVEL_F", DEFAULT_ZOOM.toFloat()).toDouble()
        mapControllerNew.setZoom(zoom)
        map.zoomController.display.setPositions(
            false, HorizontalPosition.RIGHT, VerticalPosition.CENTER
        )
        map.overlays.add(CopyrightOverlay(requireContext())) //копирайт

        // это было
        // mapControllerNew.setCenter(startPoint)
        mapControllerNew.setCenter(
            GeoPoint(
                prefs.getFloat("MAP_CENTER_LAT", DEFAULT_LOCATIONS_LAT.toFloat()).toDouble(),
                prefs.getFloat("MAP_CENTER_LON", DEFAULT_LOCATIONS_LAT.toFloat()).toDouble()
            )
        )
        map.invalidate()
    }

    //     отрисовка маршрута проезда
    private fun getPath(): Polyline { //Singleton
        if (path1 == null) {
            path1 = Polyline()
            with(path1!!) {
                outlinePaint.color = Color.RED
                outlinePaint.strokeWidth = 10f
                addPoint(startPoint.clone())
            }
            map.overlayManager.add(path1)
        }
        return path1!!
    }

    //     маркер текущей геопозиции
    private fun getPositionMarker(): Marker { //Singleton
// ===
/*

        if (marker == null) {
            marker = Marker(map)

            with(marker!!) {
                id = "MyLocation"
                title =
                    "lan: ${startPoint.latitude}, lon: ${startPoint.longitude}\n time: ${dateTimeEvent()}"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = ResourcesCompat.getDrawable(resources, (R.drawable.dot), null)
            }
            Log.d(TAG, "getPositionMarker() called: $marker")
//            map.overlays.add(marker)
        }

*/


        // ===
        if (marker == null) {
            marker = Marker(map)
//            Log.d(TAG, "getPositionMarker() called: $marker")
        }
        marker!!.apply {
            id = "MyLocation"
            title =
                "lan: ${startPoint.latitude}, lon: ${startPoint.longitude}\n time: ${dateTimeEvent()}"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = ResourcesCompat.getDrawable(resources, (R.drawable.dot), null)
        }

        map.overlays.add(marker)

//        Log.d(TAG, "getPositionMarker() marker ЕСТЬ ===========> ${marker!!.id}")

        return marker!!
    }

    fun saveMapPrefs(){
        val prefs: SharedPreferences = requireActivity().getSharedPreferences("MOTOHELP", Context.MODE_PRIVATE)
        val editPref = prefs.edit()
        editPref.putFloat("MAP_ZOOM_LEVEL_F", map.getZoomLevelDouble().toFloat())
        val mapCenter = map.getMapCenter()
        editPref.putFloat("MAP_CENTER_LAT", mapCenter.latitude.toFloat())
        editPref.putFloat("MAP_CENTER_LON", mapCenter.longitude.toFloat())
        editPref.apply()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = MapScreenFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        initMap()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()

        if (requestingLocationUpdates) {
            requestingLocationUpdates = false
            stopLocationUpdates() // отстанавливаем обновление геопозиции
        }
        binding.mapView.onPause()
        saveMapPrefs()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


//Comments section
/*
init {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.let {
                    for (location in locationResult.locations) {
//                         обновление UI данных о местоположении
                        updateLocation(location)
                    }
                }

            }
        }
            Класс данных, представляющий географическое местоположение, полученный от объединенного поставщика местоположения.
            Все местоположения, возвращаемые getLocations() , гарантированно имеют допустимую широту,
             долготу и отметку времени UTC. На уровне API 17 или выше они также гарантированно пройдут
             в режиме реального времени с момента загрузки. Все остальные параметры являются необязательными.*/

//**********************************************************************************

/*private fun createLocationRequest(): LocationRequest {
    return LocationRequest.create().apply {
        interval = 1000
        fastestInterval = 500
        smallestDisplacement = 10f
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        maxWaitTime = 1000
    }
 Комментарии
        return LocationRequest.create() //Объекты LocationRequest используются для запроса качества
            // обслуживания для обновлений местоположения от FusedLocationProviderApi
            .apply { //https://stackoverflow.com/questions/66489605/is-constructor-locationrequest-deprecated-in-google-maps-v2
                interval = 1000 //можно поставить и большее значение
                fastestInterval = 500
                smallestDisplacement =
                    10f //10m Установите минимальное смещение между обновлениями местоположения в метрах
                // По умолчанию это 0. Наименьшее смещение в метрах, которое пользователь должен перемещать между обновлениями местоположения.
                priority =
                    LocationRequest.PRIORITY_HIGH_ACCURACY //Установить приоритет запроса.
                // Используйте с константой приоритета, такой как PRIORITY_HIGH_ACCURACY . Никакие другие значения не принимаются.
                // Приоритет запроса является подсказкой для LocationClient, какие источники местоположения следует использовать.
                Например, PRIORITY_HIGH_ACCURACY , скорее всего, будет использовать GPS, а PRIORITY_BALANCED_POWER_ACCURACY ,
                скорее всего, будет использовать WIFI и позиционирование вышек сотовой связи,
                но это также зависит от многих других факторов (например, от доступных источников) и зависит от реализации.
                // setPriority(int) и setInterval(long) являются наиболее важными параметрами в запросе местоположения.
                maxWaitTime =
                    1000 // Устанавливает максимальное время ожидания в миллисекундах для обновлений местоположения.
            }
}*/

//**********************************************************************************

/*@Subscribe(threadMode = ThreadMode.MAIN)
fun onMsg(status: MyEventLocationSettingsChange) {
    if (status.on) {
        initMap()
    } else {
        Timber.i("Stop something")
    }
        Каждый метод подписчика имеет режим потока,
        который определяет, в каком потоке метод должен быть вызван EventBus.
        EventBus заботится о потоках независимо от потока сообщений.

}*/

//**********************************************************************************

/*@SuppressLint("MissingPermission")
private fun startLocationUpdates() {
    fusedLocationClient.requestLocationUpdates(
        locationRequest, locationCallback, Looper.getMainLooper()
    )
/*onResume
         Класс, используемый для запуска цикла сообщений для потока.
         Потоки по умолчанию не имеют связанного с ними цикла сообщений; чтобы создать его,
         вызовите prepare в потоке, который должен запустить цикл, а затем выполните loop ,
         чтобы он обрабатывал сообщения, пока цикл не будет остановлен.*/
}*/

//**********************************************************************************

/* https://developer.android.com/training/location/retrieve-current
 Используя API местоположения служб Google Play, ваше приложение может запросить последнее
 известное местоположение устройства пользователя. В большинстве случаев вас интересует
 текущее местоположение пользователя, которое обычно эквивалентно последнему известному
 местоположению устройства.
@SuppressLint("MissingPermission") //permission are checked before
fun readLastKnownLocation() {
    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
        location?.let { updateLocation(it) }
    }
}*/

//**********************************************************************************

/*
private fun initMap() {
    initLocation()
    if (!requestingLocationUpdates) {
        requestingLocationUpdates = true
        startLocationUpdates()
    }
    *//* включение ночного режима путем инверсии карты. получается неплохо.
    var displayMode = AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS)*//*

    mapControllerNew.setZoom(DEFAULT_ZOOM)
    map.zoomController.display.setPositions(
        false, HorizontalPosition.RIGHT, VerticalPosition.CENTER
    )
    map.overlays.add(CopyrightOverlay(requireContext())) //копирайт

    mapControllerNew.setCenter(startPoint)
    map.invalidate()
}*/

//**********************************************************************************

/*эксперименты с яндекс артами
fun openYandexMap(lat:Double,lon:Double){
    val uri: Uri = Uri.parse("yandexmaps://maps.yandex.ru/?ll=${lat},${lon},z=12")
    var intent = Intent(Intent.ACTION_VIEW, uri)
    // Проверяем, установлено ли хотя бы одно приложение, способное выполнить это действие.
    // Проверяем, установлено ли хотя бы одно приложение, способное выполнить это действие.
    val packageManager: PackageManager = requireActivity().getPackageManager()
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

// исходные данные по функции getFullAddress
/* val addresses = geocoder.getFromLocation(markerPoint.lat, markerPoint.lon, 1)
 val addressPoint = addresses?.get(0)?.getAddressLine(0)*/
// было // val addresses = geocoder.getFromLocation(markerPoint.lat, markerPoint.lon, 1)
///val addresses = getAddressResult(markerPoint.lat, markerPoint.lon, 1, requireContext())
// было // val addressPoint = addresses?.get(0)?.getAddressLine(0)
////val addressPoint = fullAddress(addresses)
// команетарии в Extensions. Потом это можно удалить как все просмотрят

