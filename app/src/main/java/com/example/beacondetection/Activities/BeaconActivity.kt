package com.example.beacondetection.Activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.beacondetection.Adapters.BeaconAdapter
import com.example.beacondetection.DB.SQLiteHelper
import com.example.beacondetection.R

class BeaconActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BeaconAdapter
    private lateinit var databaseHelper: SQLiteHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beacon)

        recyclerView = findViewById(R.id.recyclerViewBeacons)
        recyclerView.layoutManager = LinearLayoutManager(this)

        databaseHelper = SQLiteHelper.getInstance(this)
        val beaconList = databaseHelper.getAllBeacons()

        adapter = BeaconAdapter(beaconList)
        recyclerView.adapter = adapter
    }
}
