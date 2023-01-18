package ru.motohelp.app.data

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import ru.motohelp.app.R
import ru.motohelp.app.databinding.ListItemBinding
import ru.motohelp.app.utils.*
import java.util.*

class RecyclerViewAdapter(private val listener: Listener) :
    ListAdapter<MarkerPoint, RecyclerViewAdapter.RecyclerViewHolder>(ExampleItemDiffCallback) {
    class RecyclerViewHolder(
        private val itemBinding: ListItemBinding,
    ) : RecyclerView.ViewHolder(itemBinding.root) {

        private val user by lazy { FirebaseAuth.getInstance().currentUser }

        /* Bind league name and image. */
        @SuppressLint("SimpleDateFormat")
        fun bind(markerPoint: MarkerPoint, listener: Listener) = with(itemBinding) {

            if (user?.isAnonymous == false) {
                itemView.setOnClickListener {
                    listener.onClick(markerPoint)
                }
            }

            // TODO доделать переход на точку
            /*
                       imageViewGoToMarker.setOnClickListener {
                           listener.onClickImage(markerPoint)
                       }
           */

            twEventTime.text = setDateTimeMarketPoint(markerPoint, imageViewAmbulance.context)

            val addressText =
                getFullAddress(markerPoint.lat, markerPoint.lon, 1, imageViewAmbulance.context)!!
            twAddress.text = addressText

            imageViewShare.setOnClickListener {
                val string = "${markerPoint.lat},${markerPoint.lon}"
                shareUrl(it.context, string)
            }

            twWhatHappened.text = markerPoint.accidentDescription?.let { imageViewAmbulance.context.resources.getString(it.stringResId) }

            twAftermathInfo.text = markerPoint.severityAccident?.let { imageViewAmbulance.context.resources.getString(it.stringResId) }


            if (!markerPoint.accidentNotes.isNullOrEmpty()) {
                twNote.isVisible = true
                twNote.text = markerPoint.accidentNotes
            }

            if (markerPoint.callAmbulance) {
                imageViewAmbulance.setImageResource(
                    R.drawable.ic_baseline_add_alert_24_red
                )
            } else {
                imageViewAmbulance.setImageResource(
                    R.drawable.ic_baseline_add_alert_24_grey
                )
            }

            if (markerPoint.pointState == PointState.Checked) {
                imageViewHelp.setBackgroundResource(R.color.green)
            } else {
                imageViewHelp.background = null
            }

            if (markerPoint.accidentType == AccidentType.Accident) {
                conLayout.setBackgroundResource(R.color.card_accident)
            }
            if (markerPoint.accidentType == AccidentType.Breakdown) {
                conLayout.setBackgroundResource(R.color.card_breakdown)
            }
            if (markerPoint.accidentType == AccidentType.Ambulance) {
                conLayout.setBackgroundResource(R.color.card_ambulance)
            }
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = ListItemBinding.inflate(layoutInflater, parent, false)
        return RecyclerViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {

        val logs = getItem(position)
        holder.bind(logs, listener)

    }

    object ExampleItemDiffCallback : DiffUtil.ItemCallback<MarkerPoint>() {
        override fun areItemsTheSame(oldItem: MarkerPoint, newItem: MarkerPoint): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: MarkerPoint, newItem: MarkerPoint): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }
    }


    interface Listener {
        fun onClick(marker: MarkerPoint)
        fun onClickImage(marker: MarkerPoint)
    }

}
