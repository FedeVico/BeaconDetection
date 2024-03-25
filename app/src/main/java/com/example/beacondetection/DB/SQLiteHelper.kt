package com.example.beacondetection.DB

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.beacondetection.IBeacon

class SQLiteHelper(private val context: Context) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: SQLiteHelper? = null

        fun getInstance(context: Context): SQLiteHelper {
            return synchronized(this) {
                INSTANCE ?: SQLiteHelper(context).also { INSTANCE = it }
            }
        }
    }

    private val database: SQLiteDatabase by lazy { context.openOrCreateDatabase("beacons.db", Context.MODE_PRIVATE, null) }

    fun openDatabase() {
        database.execSQL("CREATE TABLE IF NOT EXISTS beacons (uuid TEXT PRIMARY KEY, macAddress TEXT, major INTEGER, minor INTEGER, rssi INTEGER, distance REAL)") // Cambios aquí
    }

    fun closeDatabase() {
        database.close()
    }

    fun insertOrUpdateDevice(beacon: IBeacon) {
        database.execSQL("INSERT OR REPLACE INTO beacons (uuid, macAddress, major, minor, rssi, distance) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf(beacon.getUUID(), beacon.getAddress(), beacon.getMajor(), beacon.getMinor(), beacon.getRssi(), beacon.getDistance()))
    }

    /*@SuppressLint("Range")
    fun getAllDevices(): List<IBeacon> { // Cambiada la estructura de retorno a IBeacon
        val cursor: Cursor = database.rawQuery("SELECT * FROM devices", null)
        val devices = mutableListOf<IBeacon>()
        while (cursor.moveToNext()) {
            val uuid = cursor.getString(cursor.getColumnIndex("uuid"))
            val macAddress = cursor.getString(cursor.getColumnIndex("macAddress"))
            val major = cursor.getInt(cursor.getColumnIndex("major"))
            val minor = cursor.getInt(cursor.getColumnIndex("minor"))
            val rssi = cursor.getInt(cursor.getColumnIndex("rssi"))
            val distance = cursor.getDouble(cursor.getColumnIndex("distance"))
            devices.add(IBeacon(uuid, macAddress, major, minor, rssi, distance)) // Ahora usamos el constructor de IBeacon
        }
        cursor.close()
        return devices
    }*/
}
