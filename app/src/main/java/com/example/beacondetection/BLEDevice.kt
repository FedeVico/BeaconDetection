package com.example.beacondetection

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


    init {
        if (scanResult.device.name != null) {
            name = scanResult.device.name
        }
        address = scanResult.device.address
        rssi = scanResult.rssi
    }

    fun getAddress(): String {
        return address
    }

    fun getRssi(): Int {
        return rssi
    }

    fun getDistance(): Double {
        val measuredPower = -59 // Potencia medida en dBm a 1 metro de distancia
        val N = 2.0 // Factor de atenuación de la señal
        return 10.0.pow(((measuredPower - rssi) / (10 * N)))
    }

}