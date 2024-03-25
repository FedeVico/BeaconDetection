package com.example.beacondetection.DB

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
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

    @SuppressLint("Range")
    fun getAllDevicesUuidAndDistance(): ArrayList<Pair<String, Double>> {
        val devices = ArrayList<Pair<String, Double>>()
        val cursor: Cursor? = database.rawQuery("SELECT uuid, distance FROM beacons", null)
        cursor?.use {
            while (it.moveToNext()) {
                val uuid = it.getString(it.getColumnIndex("uuid"))
                val distance = it.getDouble(it.getColumnIndex("distance"))
                devices.add(Pair(uuid, distance))
            }
        }
        return devices
    }
}
