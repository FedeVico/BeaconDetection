package com.example.beacondetection

data class BeaconData(
    val uuid: String,
    val macAddress: String,
    val major: Int,
    val minor: Int,
    val rssi: Int,
    val distance: Double
)
