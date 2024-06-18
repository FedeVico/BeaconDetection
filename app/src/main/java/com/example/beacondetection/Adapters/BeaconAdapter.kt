package com.example.beacondetection.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.beacondetection.BeaconEntities.BeaconData
import com.example.beacondetection.R

class BeaconAdapter(private val beacons: List<BeaconData>) : RecyclerView.Adapter<BeaconAdapter.BeaconViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.ibeacon_item, parent, false)
        return BeaconViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BeaconViewHolder, position: Int) {
        val beacon = beacons[position]
        holder.uuid.text = "UUID: ${beacon.uuid}"
        holder.major.text = "Major: ${beacon.major}"
        holder.minor.text = "Minor: ${beacon.minor}"
        holder.address.text = "Dirección: ${beacon.macAddress}"
        holder.rssi.text = "RSSI: ${beacon.rssi}"
        holder.distance.text = "Distancia: ${beacon.distance}"
        holder.timestamp.text = "Fecha: ${beacon.timestamp}"

    }

    override fun getItemCount(): Int {
        return beacons.size
    }

    class BeaconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val uuid: TextView = itemView.findViewById(R.id.text_uuid_value)
        val major: TextView = itemView.findViewById(R.id.text_major_value)
        val minor: TextView = itemView.findViewById(R.id.text_minor_value)
        val address: TextView = itemView.findViewById(R.id.text_address_value)
        val rssi: TextView = itemView.findViewById(R.id.text_rssi_value)
        val distance: TextView = itemView.findViewById(R.id.text_distance_value)
        val timestamp: TextView = itemView.findViewById(R.id.text_timestamp_value)
    }
}
