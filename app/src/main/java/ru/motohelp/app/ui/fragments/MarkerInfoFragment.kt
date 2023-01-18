package ru.motohelp.app.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import ru.motohelp.app.R
import ru.motohelp.app.app
import ru.motohelp.app.data.MarkerPoint
import ru.motohelp.app.databinding.MarkerInfoFragmentBinding
import ru.motohelp.app.utils.*

class MarkerInfoFragment : Fragment(R.layout.marker_info_fragment) {

    private var _binding: MarkerInfoFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var markerPointBundle: MarkerPoint
    private lateinit var markerPointLocal: MarkerPoint

    private val TAG = "happy"

    companion object {
        const val BUNDLE_EXTRA = "pointMarkerInfo"
        fun newInstance(bundle: Bundle): MarkerInfoFragment {
            val fragment = MarkerInfoFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = MarkerInfoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            it.getParcelable<MarkerPoint>(BUNDLE_EXTRA)?.let { markerPoint ->
                markerPointBundle = markerPoint
            }
        }
        setEditMarkerPointInformation(markerPointBundle)
    }

    private fun setEditMarkerPointInformation(markerPointBundle: MarkerPoint) {

        binding.addressEventConst.text =
            getFullAddress(markerPointBundle.lat, markerPointBundle.lon, 1, app.applicationContext)

        binding.serviceInfoConst.text =
            setDateTimeMarketPoint(markerPointBundle, app.applicationContext)

        // spinner for AccidentDescription
        val spinnerAccidentDescription = binding.spinnerAccidentDescSwitch

        var accidentDescriptionInfo = markerPointBundle.accidentDescription

        val keysAccidentDesc: List<String> = AccidentDescription.values()
            .map { app.applicationContext.resources.getString(it.stringResId) }

        val positionAccidentDescFromMarkerPoint =
            AccidentDescription.valueOf(markerPointBundle.accidentDescription.toString()).ordinal

        val adapterDescription = ArrayAdapter(
            app.applicationContext,
            android.R.layout.simple_spinner_item,
            keysAccidentDesc
        )

        adapterDescription.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAccidentDescription.adapter = adapterDescription
        spinnerAccidentDescription.setSelection(positionAccidentDescFromMarkerPoint)

        spinnerAccidentDescription.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                accidentDescriptionInfo = AccidentDescription.values()[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //
            }
        }

        // spinner for AccidentDescription end

        // spinner for SeverityAccident

        val spinnerSeverityAccident = binding.spinnerSeverityAccidentSwitch
        var severityAccidentInfo = markerPointBundle.severityAccident
        val keysSeverityAccident: List<String> = SeverityAccident.values().map { app.applicationContext.resources.getString(it.stringResId) }

        val positionSeverityAccidentFromMakerPoint = SeverityAccident.valueOf(markerPointBundle.severityAccident.toString()).ordinal

        val adapterSeverity = ArrayAdapter(app.applicationContext,
            android.R.layout.simple_spinner_item,
            keysSeverityAccident
        )
        adapterSeverity.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSeverityAccident.adapter = adapterSeverity
        spinnerSeverityAccident.setSelection(positionSeverityAccidentFromMakerPoint)

        spinnerSeverityAccident.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                severityAccidentInfo = SeverityAccident.values()[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //
            }
        }

        // spinner for SeverityAccident END

        binding.switchAmbulance.isChecked = markerPointBundle.callAmbulance
        var ambulanceCall = markerPointBundle.callAmbulance
        binding.switchAmbulance.setOnCheckedChangeListener { _, isChecked ->
            ambulanceCall = isChecked
        }
        binding.noteEmotionAccidentEdit.setText(markerPointBundle.accidentNotes)

        if (markerPointBundle.pointState == PointState.Checked) {
            binding.helpBannerEditWin.apply {
                isVisible = true
                text = resources.getText(R.string.help_on_road)
                setBackgroundColor(resources.getColor(R.color.accident_go_button))
            }
        }

        binding.saveInformationButton.setOnClickListener {

            val note = binding.noteEmotionAccidentEdit.text.toString()

//            Log.d(TAG, "Note смотреть = $note")

            markerPointLocal = MarkerPoint(
                markerPointBundle.currentUser,
                markerPointBundle.accidentType,
                markerPointBundle.lat,
                markerPointBundle.lon,
                markerPointBundle.timestamp,
                markerPointBundle.pointState,
                accidentDescriptionInfo,
                severityAccidentInfo,
                ambulanceCall,
                note
            )

            saveMarkerPointAfterEdit(markerPointLocal)

            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.cancelInformationButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun saveMarkerPointAfterEdit(markerPointLocal: MarkerPoint) {
//            Добавление маркера в ДБ после редактирования
        app.dbGeoPoints.child(markerPointLocal.timestamp.toString())
            .setValue(markerPointLocal)
            .addOnSuccessListener {
                Log.d(TAG, "Удачно записали")

            }.addOnFailureListener {
                Log.d(TAG, "Не удалось записать")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
