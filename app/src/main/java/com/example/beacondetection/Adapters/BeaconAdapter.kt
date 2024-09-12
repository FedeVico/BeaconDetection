package com.example.beacondetection.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.beacondetection.BeaconEntities.BeaconData
import com.example.beacondetection.R

class BeaconAdapter(private val beaconList: List<BeaconData>) : RecyclerView.Adapter<BeaconAdapter.BeaconViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.ibeacon_item, parent, false)
        return BeaconViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BeaconViewHolder, position: Int) {
        val beacon = beaconList[position]
        holder.uuid.text = beacon.uuid
        holder.macAddress.text = beacon.macAddress
        holder.major.text = beacon.major.toString()
        holder.minor.text = beacon.minor.toString()
        holder.rssi.text = beacon.rssi.toString()
        holder.distance.text = beacon.distance.toString()
        holder.timestamp.text = beacon.timestamp
        holder.numDevices.text = beacon.numDevices.toString()
    }

    override fun getItemCount() = beaconList.size

    class BeaconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val uuid: TextView = itemView.findViewById(R.id.text_uuid_value)
        val macAddress: TextView = itemView.findViewById(R.id.text_address_value)
        val major: TextView = itemView.findViewById(R.id.text_major_value)
        val minor: TextView = itemView.findViewById(R.id.text_minor_value)
        val rssi: TextView = itemView.findViewById(R.id.text_rssi_value)
        val distance: TextView = itemView.findViewById(R.id.text_distance_value)
        val timestamp: TextView = itemView.findViewById(R.id.text_timestamp_value)
        val numDevices: TextView = itemView.findViewById(R.id.text_num_devices_value)
    }
}
