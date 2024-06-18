package com.example.beacondetection.BeaconEntities

data class BeaconData(
    val uuid: String,
    val macAddress: String,
    val major: Int,
    val minor: Int,
    val rssi: Int,
    val distance: Double,
    val timestamp: String
)

