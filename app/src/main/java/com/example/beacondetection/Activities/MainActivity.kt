package com.example.beacondetection.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.beacondetection.R
import org.altbeacon.beacon.service.BeaconService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val btnBeaconScan = findViewById<Button>(R.id.btnBeaconScan)
        val btnMap = findViewById<Button>(R.id.btnMap)
        val btnBeacon = findViewById<Button>(R.id.btnBeacon)
        val btnFaq = findViewById<Button>(R.id.btnFaq)
        val btnStopAll = findViewById<Button>(R.id.btnStopAll) // Button to stop all subprocesses

        btnBeaconScan.setOnClickListener {
            val intent = Intent(this, BeaconScanActivity::class.java)
            startActivity(intent)
        }

        btnMap.setOnClickListener {
            showMapDialog()
        }

        btnBeacon.setOnClickListener {
            val intent = Intent(this, BeaconActivity::class.java)
            startActivity(intent)
        }

        btnFaq.setOnClickListener {
            val intent = Intent(this, FAQActivity::class.java)
            startActivity(intent)
        }

        btnStopAll.setOnClickListener {
            showStopAllDialog()
        }
    }

    private fun showMapDialog() {
        val options = arrayOf("Mostrar mi ubicación", "Mostrar balizas detectadas", "Mostrar ambas")
        val checkedItems = booleanArrayOf(false, false, false) // Inicializa todos los elementos no seleccionados

        AlertDialog.Builder(this)
            .setTitle("Elija lo que desea mostrar en el mapa")
            .setMultiChoiceItems(options, checkedItems) { dialog, which, isChecked ->
                checkedItems[which] = isChecked // Actualiza el estado del ítem seleccionado
            }
            .setPositiveButton("OK") { dialog, which ->
                // Acción después de que el usuario presiona OK
                val intent = Intent(this, MapActivity::class.java)
                intent.putExtra("showMyLocation", checkedItems[0])
                intent.putExtra("showBeacons", checkedItems[1])
                intent.putExtra("showBoth", checkedItems[2])
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showStopAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("Detener y salir")
            .setMessage("Vas a detener todos los procesos y salir de la app. ¿Estas seguro?")
            .setPositiveButton("Sí") { dialog, which ->
                stopAllProcesses()
                finishAffinity() // Close the app
            }
            .setNegativeButton("No") { dialog, which ->
                dialog.dismiss() // Close the dialog
            }
            .show()
    }

    // Aquí se detendrán todos los subprocesos, servicios, etc
    private fun stopAllProcesses() {
        val stopIntent = Intent(this, BeaconService::class.java)
        stopService(stopIntent)
    }
}
