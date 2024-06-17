package com.example.beacondetection

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val btnBeaconScan = findViewById<Button>(R.id.btnBeaconScan)
        val btnMap = findViewById<Button>(R.id.btnMap)
        val btnBeacon = findViewById<Button>(R.id.btnBeacon)
        val btnFaq = findViewById<Button>(R.id.btnFaq)

        btnBeaconScan.setOnClickListener {
            val intent = Intent(this, BeaconScanActivity::class.java)
            startActivity(intent)
        }

        btnMap.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        btnBeacon.setOnClickListener {
            val intent = Intent(this, BeaconActivity::class.java)
            startActivity(intent)
        }

        btnFaq.setOnClickListener {
            val intent = Intent(this, FAQActivity::class.java)
            startActivity(intent)
        }
    }
}
