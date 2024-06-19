package com.example.beacondetection.BeaconEntities

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import kotlin.math.pow

@SuppressLint("MissingPermission")
open class BLEDevice(scanResult: ScanResult) {

    /**
     * The measured signal strength of the Bluetooth packet
     */
    private var rssi: Int = 0

    /**
     * Device mac address
     */
    private var address: String = ""

    /**
     * Device distance based on RSSI
     */
    private var distance: Int = 0

    /**
     * Device friendly name
     */
    private var name: String = ""

    private var rssiList: MutableList<Int> = mutableListOf()
    private val rssiListSize = 5 // Tamaño del historial de lecturas de RSSI
    init {
        if (scanResult.device.name != null) {
            name = scanResult.device.name
        }
        address = scanResult.device.address
        rssiList.add(scanResult.rssi)
    }

    fun getAddress(): String {
        return address
    }

    fun addRssi(rssi: Int) {
        if (rssiList.size >= rssiListSize) {
            rssiList.removeAt(0)
        }
        rssiList.add(rssi)
    }

    fun getRssi(): Int {
        return rssiList.average().toInt()
    }

    fun getDistance(): Double {
        val measuredPower = -59 // Potencia medida en dBm a 1 metro de distancia
        val N = 2.0 // Factor de atenuación de la señal
        val averageRssi = rssiList.average()
        return 10.0.pow(((measuredPower - averageRssi) / (10 * N)))
    }

}