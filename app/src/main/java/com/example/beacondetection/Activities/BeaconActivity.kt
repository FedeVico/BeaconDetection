package com.example.beacondetection.Activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.beacondetection.Adapters.BeaconAdapter
import com.example.beacondetection.BeaconEntities.BeaconData
import com.example.beacondetection.DB.FirestoreHelper
import com.example.beacondetection.R
import com.google.firebase.firestore.FirebaseFirestore

class BeaconActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BeaconAdapter
    private lateinit var databaseHelper: FirestoreHelper
    private lateinit var inputSearchUuid: EditText
    private lateinit var btnSearch: Button
    private val firestore = FirebaseFirestore.getInstance()
    private val beaconList = mutableListOf<BeaconData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beacon)

        recyclerView = findViewById(R.id.recyclerViewBeacons)
        recyclerView.layoutManager = LinearLayoutManager(this)

        inputSearchUuid = findViewById(R.id.input_search_uuid)
        btnSearch = findViewById(R.id.btn_search)
        databaseHelper = FirestoreHelper(this)

        adapter = BeaconAdapter(beaconList)
        recyclerView.adapter = adapter

        btnSearch.setOnClickListener {
            val uuid = inputSearchUuid.text.toString().trim()
            if (uuid.isNotEmpty()) {
                searchBeacon(uuid)
            } else {
                searchAllBeacons()
            }
        }
    }

    private fun searchBeacon(uuid: String) {
        Log.d("BeaconActivity", "Searching for beacons with UUID: $uuid")
        firestore.collection("beacons")
            .orderBy("uuid")
            .startAt(uuid)
            .endAt(uuid + '\uf8ff')
            .get()
            .addOnSuccessListener { documents ->
                beaconList.clear()
                for (document in documents) {
                    val beacon = document.toObject(BeaconData::class.java)
                    updateBeaconNumDevices(beacon) { updatedBeacon ->
                        beaconList.add(updatedBeacon)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("BeaconActivity", "Error getting documents: ", exception)
            }
    }

    private fun searchAllBeacons() {
        Log.d("BeaconActivity", "Searching for all beacons")
        firestore.collection("beacons")
            .get()
            .addOnSuccessListener { documents ->
                beaconList.clear()
                for (document in documents) {
                    val beacon = document.toObject(BeaconData::class.java)
                    updateBeaconNumDevices(beacon) { updatedBeacon ->
                        beaconList.add(updatedBeacon)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("BeaconActivity", "Error getting documents: ", exception)
            }
    }

    private fun updateBeaconNumDevices(beacon: BeaconData, callback: (BeaconData) -> Unit) {
        FirestoreHelper.getInstance(this).countDevicesInRange(beacon.uuid) { count ->
            val updatedBeacon = beacon.copy(numDevices = count)
            callback(updatedBeacon)
        }
    }
}
