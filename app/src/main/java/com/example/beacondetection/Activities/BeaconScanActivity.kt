package com.example.beacondetection.Activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.beacondetection.Adapters.DeviceListAdapter
import com.example.beacondetection.DB.SQLiteHelper
import com.example.beacondetection.R
import com.example.beacondetection.Services.ScanService
import com.example.beacondetection.databinding.ActivityBeaconScanBinding

class BeaconScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBeaconScanBinding
    private val TAG = "MainActivity"

    private lateinit var scanService: ScanService
    private lateinit var adapter: DeviceListAdapter
    private lateinit var deviceList: ArrayList<Any>
    private lateinit var databaseHelper: SQLiteHelper


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

        // check for permission to scan BLE
        if (isPermissionGranted(this)) {
            Log.d(TAG, "@onCreate init scan service")
            // Initialize scanService here
            scanService = ScanService(this, this.deviceList, this.adapter)
        }
    }

    private fun startScan(context: Context) {
        // Check if scanService is initialized
        if (::scanService.isInitialized) {
            // check Bluetooth
            if (!scanService.isBluetoothEnabled()) {
                Log.d(TAG, "@startScan Bluetooth is disabled")
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestBluetooth.launch(intent)
            } else {
                scanService.initScanner()
                // start scanning BLE device
                if (scanService.isScanning()) {
                    binding.scanBtn.text = resources.getString(R.string.label_scan)
                    scanService.stopBLEScan(context)
                } else {
                    scanService.startBLEScan(context)
                    binding.scanBtn.text = resources.getString(R.string.label_scanning)
                }
            }
        } else {
            Log.e(TAG, "scanService is not initialized")
        }
    }

    /**
     * Start BLE scan
     * Check Bluetooth before scanning.
     * If Bluetooth is disabled, request user to turn on Bluetooth
     */
    // necessary permissions on Android <12
    private val BLE_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // necessary permissions on Android >=12
    @RequiresApi(Build.VERSION_CODES.S)
    private val ANDROID_12_BLE_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    /**
     * Determine whether the location permission has been granted
     * if not, request the permission
     *
     * @param context
     * @return true if user has granted permission
     */
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
                ActivityCompat.requestPermissions(this, ANDROID_12_BLE_PERMISSIONS, 2)
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
                ActivityCompat.requestPermissions(this, BLE_PERMISSIONS, 3)
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
}
