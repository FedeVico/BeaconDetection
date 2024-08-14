package com.example.beacondetection.DB

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import com.example.beacondetection.BeaconEntities.BLEDevice
import com.example.beacondetection.BeaconEntities.IBeacon
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirestoreHelper(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: FirestoreHelper? = null
        private var firestoreInstance: FirebaseFirestore? = null

        @JvmStatic
        fun getInstance(context: Context): FirestoreHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirestoreHelper(context).also { INSTANCE = it }
            }
        }

        @JvmStatic
        fun setFirestoreInstance(instance: FirebaseFirestore) {
            firestoreInstance = instance
        }
    }

    val db: FirebaseFirestore by lazy { firestoreInstance ?: FirebaseFirestore.getInstance() }

    fun insertOrUpdateDevice(beacon: IBeacon) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val timestampString = dateFormat.format(Date())
        val beaconData = hashMapOf(
            "uuid" to beacon.getUUID(),
            "macAddress" to beacon.getAddress(),
            "major" to beacon.getMajor(),
            "minor" to beacon.getMinor(),
            "rssi" to beacon.calculateRssi(),
            "distance" to beacon.getDistance(),
            "timestamp" to timestampString,
            "numDevices" to 0 // Inicializar con 0 dispositivos
        )

        db.collection("beacons").document(beacon.getUUID())
            .set(beaconData, SetOptions.merge()) // Use merge to avoid overwriting
            .addOnSuccessListener {
                Log.d(TAG, "Beacon data added/updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to add/update beacon data", e)
            }
    }

    fun insertBLEDevice(device: BLEDevice) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val timestampString = dateFormat.format(Date())

        val deviceData = hashMapOf(
            "address" to device.getAddress(),
            "rssi" to device.calculateRssi(),
            "distance" to device.getDistance(),
            "name" to device.name,
            "timestamp" to timestampString
        )

        db.collection("BLEs")
            .document(device.getAddress())
            .set(deviceData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "BLE device data added successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to add BLE device data", e)
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

    fun countDevicesInRange(uuid: String, callback: (Int) -> Unit) {
        val currentTime = Date()
        val oneHourAgo = Date(currentTime.time - 3600000) // 1 hour in milliseconds

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val oneHourAgoString = dateFormat.format(oneHourAgo)

        db.collection("deviceInteractions")
            .whereEqualTo("beaconUuid", uuid)
            .whereGreaterThan("timestamp", oneHourAgoString)
            .get()
            .addOnSuccessListener { result ->
                val interactionCount = result.documents.size // Cuenta todas las interacciones
                callback(interactionCount)
            }
            .addOnFailureListener { e ->
                callback(0)
            }
    }

    fun insertDeviceInteraction(uuid: String, macAddress: String, distance: Double) {
        if (distance < 5.0) { // Check if the distance is less than 5 meters
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val currentTime = Date()
            val timestampString = dateFormat.format(currentTime)

            // Check the last interaction time
            db.collection("deviceInteractions")
                .whereEqualTo("beaconUuid", uuid)
                .whereEqualTo("deviceMacAddress", macAddress)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    val shouldInsert = if (documents.isEmpty) {
                        true
                    } else {
                        val lastInteraction = documents.first().getString("timestamp")
                        val lastInteractionDate = dateFormat.parse(lastInteraction)
                        val difference = currentTime.time - lastInteractionDate.time
                        difference > 10000
                    }

                    if (shouldInsert) {
                        val interactionData = hashMapOf(
                            "beaconUuid" to uuid,
                            "deviceMacAddress" to macAddress,
                            "distance" to distance,
                            "timestamp" to timestampString
                        )

                        db.collection("deviceInteractions")
                            .add(interactionData)
                            .addOnSuccessListener {
                                Log.d(TAG, "Interaction data added successfully")
                                // Update the number of devices interacting with the beacon
                                countDevicesInRange(uuid) { numDevices ->
                                    updateNumDevices(uuid, numDevices)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to add interaction data", e)
                            }
                    } else {
                        Log.d(TAG, "Interaction within cooldown period, not adding new record for beacon $uuid and device $macAddress")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking for existing interaction", e)
                }
        }
    }

    fun updateNumDevices(uuid: String, count: Int) {
        db.collection("beacons")
            .document(uuid)
            .update("numDevices", count)
            .addOnSuccessListener {
                Log.d(TAG, "Updated numDevices for beacon $uuid to $count")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update numDevices for beacon $uuid", e)
            }
    }
}
