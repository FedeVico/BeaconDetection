package com.example.beacondetection.BeaconEntities

data class BeaconData(
    var uuid: String = "",
    var macAddress: String = "",
    var major: Int = 0,
    var minor: Int = 0,
    var rssi: Int = 0,
    var distance: Double = 0.0,
    var timestamp: String = "",
    var numDevices: Int = 0
) {
    // Constructor sin argumentos necesario para Firestore
    constructor() : this("", "", 0, 0, 0, 0.0, "", 0)
}
