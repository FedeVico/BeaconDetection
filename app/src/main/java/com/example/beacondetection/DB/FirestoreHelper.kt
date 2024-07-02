package com.example.beacondetection.DB

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import com.example.beacondetection.BeaconEntities.BeaconData
import com.example.beacondetection.BeaconEntities.IBeacon
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirestoreHelper(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: FirestoreHelper? = null

        fun getInstance(context: Context): FirestoreHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirestoreHelper(context).also { INSTANCE = it }
            }
        }
    }

    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun insertOrUpdateDevice(beacon: IBeacon) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val timestampString = dateFormat.format(Date())
        val beaconData = hashMapOf(
            "uuid" to beacon.getUUID(),
            "macAddress" to beacon.getAddress(),
            "major" to beacon.getMajor(),
            "minor" to beacon.getMinor(),
            "rssi" to beacon.getRssi(),
            "distance" to beacon.getDistance(),
            "timestamp" to timestampString,
            "numDevices" to 0 // Inicializar con 0 dispositivos
        )

        db.collection("beacons").document(beacon.getUUID())
            .set(beaconData)
            .addOnSuccessListener {
                // Handle success
            }
            .addOnFailureListener { e ->
                // Handle failure
            }
    }

    fun getAllDevicesUuidAndDistance(callback: (ArrayList<Pair<String, Double>>) -> Unit) {
        db.collection("beacons")
            .get()
            .addOnSuccessListener { result ->
                val devices = ArrayList<Pair<String, Double>>()
                for (document in result) {
                    val uuid = document.getString("uuid")
                    val distance = document.getDouble("distance")
                    if (uuid != null && distance != null) {
                        devices.add(Pair(uuid, distance))
                    }
                }
                callback(devices)
            }
            .addOnFailureListener { e ->
                // Handle failure
                callback(ArrayList())
            }
    }

    fun getAllBeacons(callback: (List<BeaconData>) -> Unit) {
        db.collection("beacons")
            .get()
            .addOnSuccessListener { result ->
                val beacons = mutableListOf<BeaconData>()
                for (document in result) {
                    val beacon = documentToBeaconData(document)
                    if (beacon != null) {
                        beacons.add(beacon)
                    }
                }
                callback(beacons)
            }
            .addOnFailureListener { e ->
                // Handle failure
                callback(emptyList())
            }
    }

    private fun documentToBeaconData(document: QueryDocumentSnapshot): BeaconData? {
        val uuid = document.getString("uuid")
        val macAddress = document.getString("macAddress")
        val major = document.getLong("major")?.toInt()
        val minor = document.getLong("minor")?.toInt()
        val rssi = document.getLong("rssi")?.toInt()
        val distance = document.getDouble("distance")
        val timestamp = document.getString("timestamp")
        val numDevices = document.getLong("numDevices")?.toInt() ?: 0

        return if (uuid != null && macAddress != null && major != null && minor != null && rssi != null && distance != null && timestamp != null) {
            BeaconData(uuid, macAddress, major, minor, rssi, distance, timestamp, numDevices)
        } else {
            null
        }
    }

    fun countDevicesForBeacon(uuid: String, callback: (Int) -> Unit) {
        db.collection("deviceInteractions")
            .whereEqualTo("beaconUuid", uuid)
            .get()
            .addOnSuccessListener { result ->
                callback(result.size())
            }
            .addOnFailureListener { e ->
                callback(0)
            }
    }

    fun insertDeviceInteraction(uuid: String, macAddress: String, distance: Double) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val timestampString = dateFormat.format(Date())
        val interactionData = hashMapOf(
            "beaconUuid" to uuid,
            "deviceMacAddress" to macAddress,
            "distance" to distance,
            "timestamp" to timestampString // Use Timestamp to store date and time
        )

        db.collection("deviceInteractions")
            .add(interactionData)
            .addOnSuccessListener {
                Log.d(TAG, "Interaction data added successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to add interaction data", e)
            }
    }
}
