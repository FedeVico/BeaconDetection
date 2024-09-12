package com.example.beacondetection.BeaconEntities

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult

@SuppressLint("MissingPermission")
open class BLEDevice(scanResult: ScanResult) {

    private var rssiList: MutableList<Int> = mutableListOf()
    private val kalmanFilter = KalmanFilter(0.0, 1.0, 0.125, 1.0)
    private val rssiListSize = 10
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
    var name: String = ""

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

    // Actualizar el filtro de Kalman y suavizar el RSSI
    fun addRssi(rssi: Int) {
        if (rssiList.size >= rssiListSize) {
            rssiList.removeAt(0)
        }
        val filteredRssi = kalmanFilter.update(rssi.toDouble())
        rssiList.add(filteredRssi.toInt())
    }

    fun calculateRssi(): Int {
        return rssiList.average().toInt()
    }

    fun getDistance(): Double {
        val A = 1.203420305
        val B = 6.170094565
        val C = -0.203420305
        val measuredPower = -5
        val ratio = calculateRssi().toDouble() / measuredPower
        return Math.exp(A) * Math.pow(ratio, B) + C
    }
}

class KalmanFilter(private var estimate: Double, private var errorEstimate: Double, private var processNoise: Double, private var measurementNoise: Double) {
    fun update(measurement: Double): Double {
        val kalmanGain = errorEstimate / (errorEstimate + measurementNoise)
        estimate = estimate + kalmanGain * (measurement - estimate)
        errorEstimate = (1 - kalmanGain) * errorEstimate + processNoise
        return estimate
    }
}


