package com.example.beacondetection.BeaconEntities

import android.bluetooth.le.ScanResult
import com.example.beacondetection.utils.ConversionUtils

class IBeacon(scanResult: ScanResult, packetData: ByteArray) : BLEDevice(scanResult) {
    // UUID
    var uuid: String = ""

    // full packet
    private var rawByteData: ByteArray = ByteArray(30)

    // Major and minor values
    private var major: Int? = null
    private val majorPosStart = 25
    private val majorPosEnd = 26

    private var minor: Int? = null
    private val minorPosStart = 27
    private val minorPosEnd = 28

    init {
        rawByteData = packetData
    }

    // Parse UUID from packet
    private fun parseUUID() {
        var startByte = 2
        while (startByte <= 5) {
            if (rawByteData[startByte + 2].toInt() and 0xff == 0x02 && rawByteData[startByte + 3].toInt() and 0xff == 0x15) {
                val uuidBytes = ByteArray(16)
                System.arraycopy(rawByteData, startByte + 4, uuidBytes, 0, 16)
                val hexString = ConversionUtils.bytesToHex(uuidBytes)
                if (!hexString.isNullOrEmpty()) {
                    uuid = hexString.substring(0, 8) + "-" +
                            hexString.substring(8, 12) + "-" +
                            hexString.substring(12, 16) + "-" +
                            hexString.substring(16, 20) + "-" +
                            hexString.substring(20, 32)
                    return
                }
            }
            startByte++
        }
    }

    // UUID getter method
    fun getUUID(): String {
        if (uuid.isEmpty()) {
            parseUUID()
        }
        return uuid
    }

    // Get iBeacon major
    fun getMajor(): Int {
        if (major == null)
            major = (rawByteData[majorPosStart].toInt() and 0xff) * 0x100 + (rawByteData[majorPosEnd].toInt() and 0xff)
        return major as Int
    }

    // Get iBeacon minor
    fun getMinor(): Int {
        if (minor == null)
            minor = (rawByteData[minorPosStart].toInt() and 0xff) * 0x100 + (rawByteData[minorPosEnd].toInt() and 0xff)
        return minor as Int
    }

    override fun toString(): String {
        return "UUID= $uuid Major= ${major.toString()} Minor= ${minor.toString()} rssi= ${calculateRssi()} distance= ${getDistance()}"
    }
}
