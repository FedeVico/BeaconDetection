package com.example.beacondetection.Activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.beacondetection.Adapters.DeviceListAdapter
import com.example.beacondetection.DB.FirestoreHelper
import com.example.beacondetection.R
import com.example.beacondetection.Services.ScanService
import com.example.beacondetection.databinding.ActivityBeaconScanBinding

class BeaconScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBeaconScanBinding
    private val TAG = "BeaconScanActivity"

    private lateinit var scanService: ScanService
    private lateinit var adapter: DeviceListAdapter
    private lateinit var deviceList: ArrayList<Any>
    private lateinit var firestoreHelper: FirestoreHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_BeaconDetection)
        binding = ActivityBeaconScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scanBtn.setOnClickListener { startScan(this) }

        val recycleView: RecyclerView = findViewById(R.id.deviceList)
        deviceList = ArrayList()
        this.adapter = DeviceListAdapter(this.deviceList)
        recycleView.adapter = this.adapter

        // Initialize scanService
        scanService = ScanService(this, this.deviceList, this.adapter)

        // Initialize FirestoreHelper
        firestoreHelper = FirestoreHelper.getInstance(this)
    }

    override fun onBackPressed() {
        if (scanService.isScanning()) {
            val alertDialog = AlertDialog.Builder(this)
                .setTitle("Detener escaneo")
                .setMessage("¿Desea detener el escaneo de dispositivos?")
                .setPositiveButton("Sí") { _, _ ->
                    scanService.stopBLEScan(this)
                    super.onBackPressed() // Volver atrás después de detener el escaneo
                }
                .setNegativeButton("No") { dialog, _ ->
                    super.onBackPressed()
                }
                .create()

            alertDialog.setOnShowListener {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE)
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE)
            }

            alertDialog.show()
        } else {
            super.onBackPressed() // Volver atrás sin hacer nada
        }
    }


    private fun startScan(context: Context) {
        // Check for permissions before starting the scan
        if (isPermissionGranted(context)) {
            // Check if Bluetooth is enabled
            if (!scanService.isBluetoothEnabled()) {
                Log.d(TAG, "@startScan Bluetooth is disabled")
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestBluetooth.launch(intent)
            } else {
                scanService.initScanner()
                // Start scanning BLE device
                if (scanService.isScanning()) {
                    binding.scanBtn.text = resources.getString(R.string.label_scan)
                    scanService.stopBLEScan(context)
                } else {
                    scanService.startBLEScan(context)
                    binding.scanBtn.text = resources.getString(R.string.label_scanning)
                }
            }
        }
    }

    // Necessary permissions on Android <12
    private val BLE_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Necessary permissions on Android >=12
    @RequiresApi(Build.VERSION_CODES.S)
    private val ANDROID_12_BLE_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private fun isPermissionGranted(context: Context): Boolean {
        Log.d(TAG, "@isPermissionGranted: checking bluetooth and location")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if ((ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED) ||
                (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED) ||
                (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                Log.d(TAG, "@isPermissionGranted: requesting Bluetooth and Location on Android >= 12")
                requestPermissions(ANDROID_12_BLE_PERMISSIONS, 2)
                return false
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "@isPermissionGranted: requesting Location on Android < 12")
                requestPermissions(BLE_PERMISSIONS, 3)
                return false
            }
        }

        // Check if location is enabled
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!isLocationEnabled) {
            Log.d(TAG, "@isPermissionGranted: requesting user to enable location")
            requestLocationEnable()
            return false
        }

        Log.d(TAG, "@isPermissionGranted Bluetooth and Location permission is ON")
        return true
    }

    private fun requestLocationEnable() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    private var requestBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d(TAG, "@requestBluetooth Bluetooth is enabled")
            } else {
                Log.d(TAG, "@requestBluetooth Bluetooth usage is denied")
            }
        }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2 || requestCode == 3) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d(TAG, "All permissions granted")
                startScan(this)
            } else {
                Log.d(TAG, "Permissions denied")
            }
        }
    }
}
