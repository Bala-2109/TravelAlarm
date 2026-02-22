package com.travelapp.alarm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.travelapp.alarm.R
import com.travelapp.alarm.data.model.Trip

class TripAdapter(
    private val onTripClick: (Trip) -> Unit,
    private val onStartClick: (Trip) -> Unit,
    private val onDeleteClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    private var trips = emptyList<Trip>()

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvDestination: TextView = itemView.findViewById(R.id.tvDestination)
        val tvCheckpointCount: TextView = itemView.findViewById(R.id.tvCheckpointCount)
        val tvActiveIndicator: TextView = itemView.findViewById(R.id.tvActiveIndicator)
        val btnStart: Button = itemView.findViewById(R.id.btnStart)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = trips[position]

        holder.tvName.text = trip.name
        holder.tvDestination.text = "📍 ${trip.originalDestinationName}"
        holder.tvCheckpointCount.text = "${trip.checkpoints.size} checkpoints"

        // Show/hide active indicator
        if (trip.id == getCurrentActiveTripId()) {
            holder.tvActiveIndicator.visibility = View.VISIBLE
            holder.btnStart.text = "ACTIVE"
            holder.btnStart.isEnabled = false
        } else {
            holder.tvActiveIndicator.visibility = View.GONE
            holder.btnStart.text = "START"
            holder.btnStart.isEnabled = true
        }

        // Click listeners
        holder.itemView.setOnClickListener {
            onTripClick(trip)
        }

        holder.btnStart.setOnClickListener {
            onStartClick(trip)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(trip)
        }
    }

    override fun getItemCount(): Int = trips.size

    fun submitList(newTrips: List<Trip>) {
        trips = newTrips
        notifyDataSetChanged()
    }

    // This should be set from TripListActivity
    private var activeTripId: String? = null

    fun setActiveTripId(id: String?) {
        activeTripId = id
        notifyDataSetChanged()
    }

    private fun getCurrentActiveTripId(): String? = activeTripId
}