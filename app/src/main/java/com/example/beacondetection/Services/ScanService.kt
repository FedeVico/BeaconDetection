package com.example.beacondetection.Services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.beacondetection.Adapters.DeviceListAdapter
import com.example.beacondetection.BeaconEntities.BLEDevice
import com.example.beacondetection.BeaconEntities.IBeacon
import com.example.beacondetection.DB.SQLiteHelper
import com.example.beacondetection.R

class ScanService {

    private val bluetoothManager: BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter
    private val builder: ScanSettings.Builder
    private lateinit var bluetoothLeScanner: BluetoothLeScanner

    private val TAG = "ScanService"

    // Scan service flag
    private var isScanning = false
    private val CHANNEL_ID = "beacon_detection_channel"

    private lateinit var deviceList: ArrayList<Any>
    private lateinit var adapter: DeviceListAdapter

    private lateinit var databaseHelper: SQLiteHelper

    constructor(context: Context, deviceList: ArrayList<Any>, adapter: DeviceListAdapter) {
        this.deviceList = deviceList

        databaseHelper = SQLiteHelper.getInstance(context)
        databaseHelper.openDatabase()

        builder = ScanSettings.Builder()
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        Log.d(TAG, "set scan mode to low latency")
        this.adapter = adapter
        bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            throw Exception("Device doesn't support Bluetooth")
        }
        if (isBluetoothEnabled()) {
            bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        }
    }

    fun initScanner() {
        if (!this::bluetoothLeScanner.isInitialized) {
            bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        }
    }

    /**
     * Determine whether bluetooth is enabled or not
     *
     * @return true if bluetooth is enabled, false otherwise
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }

    /**
     * Start BLE scan using bluetoothLeScanner
     * if app is not scanning, start scan by calling startScan and passing a callback method
     * @return {none}
     */
    @SuppressLint("MissingPermission")
    fun startBLEScan(context: Context) {
        if (isScanning)
            return
        Log.d(TAG, "@startBLEScan iniciar escaneo de beacons")
        isScanning = true
        try {
            // Configurar ajustes de escaneo optimizados
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Modo de escaneo rápido
                .setReportDelay(0) // Sin retraso en la notificación de resultados
                .build()
            // Iniciar escaneo con los ajustes optimizados
            bluetoothLeScanner.startScan(null, settings, leScanCallback(context))
        } catch (e: SecurityException) {
            Log.e(TAG, "@startScan SecurityException: " + e.message)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBLEScan(context: Context) {
        if (!isScanning)
            return
        Log.d(TAG, "@startBLEScan start beacon scan")
        isScanning = false
        bluetoothLeScanner.stopScan(leScanCallback(context))
        // Detenemos las notificaciones
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    private val beaconStates = HashMap<String, Boolean>()

    private fun leScanCallback(context: Context): ScanCallback {
        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                if (result != null) {
                    val scanRecord = result.scanRecord
                    super.onScanResult(callbackType, result)
                    try {
                        if (scanRecord != null) {
                            if (isIBeacon(scanRecord.bytes)) {
                                val iBeacon = IBeacon(result, scanRecord.bytes)
                                val distance = iBeacon.getDistance()
                                insertBeacon(iBeacon)
                                val idx = checkDeviceExists(result)
                                if (idx == -1) {
                                    deviceList.add(iBeacon)
                                } else {
                                    deviceList[idx] = iBeacon
                                }
                                val currentInRange = distance < 5
                                val previousInRange = beaconStates[iBeacon.getAddress()] ?: false
                                if (currentInRange != previousInRange) {
                                    if (currentInRange) {
                                        showNotification(context, "Has entrado en el radio de ${iBeacon.uuid}")
                                    }
                                    beaconStates[iBeacon.getAddress()] = currentInRange
                                }
                                adapter.notifyDataSetChanged()
                            } else {
                                val ble = BLEDevice(result)
                                val idx = checkDeviceExists(result)
                                if (idx == -1) {
                                    // Desactivo las BLE temporalmente
                                    //deviceList.add(ble)
                                } else {
                                    deviceList[idx] = ble
                                }
                                // Insertar el dispositivo BLE en la base de datos
                                // insertBLEDevice(ble)
                                adapter.notifyDataSetChanged()
                            }

                            // Limpiar el estado de los beacons que ya no están presentes en los resultados del escaneo
                            val currentBeaconAddresses = deviceList.map { (it as IBeacon).getAddress() }
                            val beaconAddressesToRemove = beaconStates.keys.filterNot { currentBeaconAddresses.contains(it) }
                            beaconAddressesToRemove.forEach { beaconStates.remove(it) }
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "@startScan SecurityException: " + e.message)
                    }
                }
            }
        }
    }

    // Funciones para insertar en la base de datos (modifica según tu estructura)
    private fun insertBeacon(beacon: IBeacon) {
        databaseHelper.insertOrUpdateDevice(beacon)
    }
    /*private fun insertBLEDevice(device: BLEDevice) {
                databaseHelper.insertDevice(device)
            }*/

    // Función auxiliar para mostrar notificaciones
    private fun showNotification(context: Context, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear el canal de notificación para dispositivos con Android Oreo y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Beacon Detection", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // Construir la notificación
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Beacon Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)

        // Mostrar la notificación
        notificationManager.notify(1, builder.build())
    }

    /**
     * check if our device list already has a scan result whose MAC address is identical to the new incoming ScanResult
     * @param result BLE scan result
     * @return -1 if doesn't exist
     */
    fun checkDeviceExists(result: ScanResult): Int {
        val indexQuery = deviceList.indexOfFirst { (it as BLEDevice) .getAddress() == result.device.address }
        return indexQuery
    }

    /**
     * Check if packet is from an iBeacon
     * @param packetData packet data which app captured
     * @return true if packet is from iBeacon, otherwise false
     */
    fun isIBeacon(packetData: ByteArray): Boolean {
        var startByte = 2
        while (startByte <= 5) {
            if (packetData[startByte + 2].toInt() and 0xff == 0x02 && packetData[startByte + 3].toInt() and 0xff == 0x15) {
                // debug result: startByte = 5
                return true
            }
            startByte++
        }
        return false
    }

    fun isScanning(): Boolean {
        return isScanning
    }
}