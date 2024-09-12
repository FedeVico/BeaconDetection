package com.example.beacondetection.Adapters
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.beacondetection.BeaconEntities.BLEDevice
import com.example.beacondetection.BeaconEntities.IBeacon
import com.example.beacondetection.DB.FirestoreHelper
import com.example.beacondetection.R

open class DeviceListAdapter(private val deviceList: ArrayList<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Define los tipos de vista
    companion object {
        const val VIEW_TYPE_BLE = 0
        const val VIEW_TYPE_IBEACON = 1
    }

    // Define los ViewHolder
    inner class BLEViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val address: TextView = view.findViewById(R.id.text_address_value)
        val rssi: TextView = view.findViewById(R.id.text_rssi_value)
        val distance: TextView = view.findViewById(R.id.text_distance_value)
        val name: TextView = view.findViewById(R.id.text_name_value)
    }

    inner class IBeaconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val uuid: TextView = view.findViewById(R.id.text_uuid_value)
        val major: TextView = view.findViewById(R.id.text_major_value)
        val minor: TextView = view.findViewById(R.id.text_minor_value)
        val address: TextView = view.findViewById(R.id.text_address_value)
        val rssi: TextView = view.findViewById(R.id.text_rssi_value)
        val distance: TextView = view.findViewById(R.id.text_distance_value)
        val numDevices: TextView = view.findViewById(R.id.text_num_devices_value)
    }

    // Obtiene el tipo de vista basado en el dispositivo BLE
    override fun getItemViewType(position: Int): Int {
        return if (deviceList[position] is IBeacon) VIEW_TYPE_IBEACON else VIEW_TYPE_BLE
    }

    // Crea nuevos ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_IBEACON) {
            val itemView = layoutInflater.inflate(R.layout.ibeacon_item, parent, false)
            IBeaconViewHolder(itemView)
        } else {
            val itemView = layoutInflater.inflate(R.layout.ble_item, parent, false)
            BLEViewHolder(itemView)
        }
    }

    // Reemplaza el contenido de la vista
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_IBEACON -> {
                val iBeaconHolder = holder as IBeaconViewHolder
                val iBeacon = deviceList[position] as IBeacon
                iBeaconHolder.uuid.text = iBeacon.getUUID()
                iBeaconHolder.major.text = iBeacon.getMajor().toString()
                iBeaconHolder.minor.text = iBeacon.getMinor().toString()
                iBeaconHolder.address.text = iBeacon.getAddress()
                iBeaconHolder.rssi.text = iBeacon.calculateRssi().toString()
                iBeaconHolder.distance.text = iBeacon.getDistance().toString()

                FirestoreHelper.getInstance(holder.itemView.context).countDevicesInRange(iBeacon.getUUID()) { numDevices ->
                    iBeaconHolder.numDevices.text = numDevices.toString()
                }            }
            VIEW_TYPE_BLE -> {
                val bleHolder = holder as BLEViewHolder
                val ble = deviceList[position] as BLEDevice
                bleHolder.address.text = ble.getAddress()
                bleHolder.rssi.text = ble.calculateRssi().toString()
                bleHolder.distance.text = ble.getDistance().toString()
                bleHolder.name.text = ble.name
            }
        }
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }
}
