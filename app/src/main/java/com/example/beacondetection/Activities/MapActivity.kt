package com.example.beacondetection.Activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.beacondetection.DB.SQLiteHelper
import com.example.beacondetection.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.Timer
import kotlin.concurrent.timerTask

class MapActivity : AppCompatActivity() {

    private lateinit var deviceList: ArrayList<Pair<String, Double>>
    private lateinit var mapView: MapView
    private var deviceMarker: Marker? = null // Referencia al marcador del dispositivo

    private val beaconsWithPosition = listOf(
        BeaconWithPosition("Sala de odenadores","11111111-1111-1111-1111-111111111111", Coordinate(37.58662, -4.64204), 0.0),
        BeaconWithPosition("Salón principal","22222222-2222-2222-2222-222222222222", Coordinate(37.58673, -4.64180), 0.0),
        BeaconWithPosition("Inventario","33333333-3333-3333-3333-333333333333", Coordinate(37.58656, -4.64189), 0.0)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Obtener las opciones seleccionadas
        val showMyLocation = intent.getBooleanExtra("showMyLocation", false)
        val showBeacons = intent.getBooleanExtra("showBeacons", false)
        val showBoth = intent.getBooleanExtra("showBoth", false)

        // Obtener una instancia de SQLiteHelper
        val dbHelper = SQLiteHelper.getInstance(this)
        dbHelper.openDatabase()

        // Configurar el agente de usuario
        val userAgentValue = "BeaconDetection"
        Configuration.getInstance().userAgentValue = userAgentValue

        // Configurar la ubicación inicial
        val initialPosition = Position(37.586621804773564, -4.641860098344363, 0.0)

        // Inicializar el mapa y su configuración
        mapView = findViewById(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        val mapController = mapView.controller
        val startPoint = GeoPoint(initialPosition.latitude, initialPosition.longitude)
        mapController.setCenter(startPoint)
        mapController.setZoom(20.0)

        // Crear el marcador del dispositivo una sola vez
        deviceMarker = Marker(mapView)
        mapView.overlays.add(deviceMarker)

        // Iniciar un temporizador para actualizar la posición cada medio segundo si es necesario
        if (showMyLocation || showBoth) {
            Timer().scheduleAtFixedRate(timerTask {
                runOnUiThread {
                    updatePositionOnMap()
                }
            }, 500, 500)
        }

        // Mostrar balizas detectadas si es necesario
        if (showBeacons || showBoth) {
            showBeaconsOnMap()
        }

        // Configurar el cuadro de texto y el botón
        val inputLocation = findViewById<EditText>(R.id.input_location)
        val btnGo = findViewById<Button>(R.id.btn_go)

        btnGo.setOnClickListener {
            val location = inputLocation.text.toString()
            if (location.isNotEmpty()) {
                val matchedBeacon = beaconsWithPosition.find { it.name.contains(location, ignoreCase = true) }
                if (matchedBeacon != null) {
                    val point = GeoPoint(matchedBeacon.position.latitude, matchedBeacon.position.longitude)
                    mapController.setCenter(point)
                    mapController.setZoom(20.0)
                } else {
                    Toast.makeText(this, "No se encontró ninguna baliza con ese nombre", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, ingresa una ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función para actualizar la posición en el mapa
    private fun updatePositionOnMap() {
        // Obtener una instancia de SQLiteHelper y la lista de dispositivos con uuid y distancia
        val dbHelper = SQLiteHelper.getInstance(this)
        dbHelper.openDatabase()
        deviceList = dbHelper.getAllDevicesUuidAndDistance()

        val estimatedPosition = calculatePosition()
        estimatedPosition?.let {
            // Actualizar la posición del marcador del dispositivo
            val devicePoint = GeoPoint(it.latitude, it.longitude)
            deviceMarker?.position = devicePoint
            deviceMarker?.title = "Mi posición estimada"
            mapView.invalidate() // Actualizar el mapa
        }
    }

    // Función para mostrar las balizas en el mapa
    private fun showBeaconsOnMap() {
        beaconsWithPosition.forEach { beacon ->
            // Crear un círculo de 5 metros de radio alrededor del marcador de la baliza
            val circle = Polygon(mapView)
            circle.points = Polygon.pointsAsCircle(GeoPoint(beacon.position.latitude, beacon.position.longitude), 5.0)
            circle.fillColor = 0x12121212 // Color de relleno con transparencia
            circle.strokeColor = 0x12121212 // Color del borde con transparencia
            circle.strokeWidth = 1f // Ancho del borde reducido a la mitad
            circle.infoWindow = null // Desactivar el InfoWindow para el círculo
            mapView.overlays.add(circle)

            // Crear el marcador de la baliza
            val beaconMarker = Marker(mapView)
            beaconMarker.position = GeoPoint(beacon.position.latitude, beacon.position.longitude)
            beaconMarker.title = beacon.name
            beaconMarker.icon = resources.getDrawable(R.drawable.ic_marker_red, null) // Usar un ícono de punto rojo
            beaconMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER) // Centrar el punto rojo

            mapView.overlays.add(beaconMarker)
        }
        mapView.invalidate() // Actualizar el mapa
    }

    // Función para calcular la posición relativa
    private fun calculatePosition(): Position? {
        // Verificar si hay suficientes balizas para la trilateración (al menos 3)
        if (deviceList.size < 3) {
            return null
        }

        // Obtener las coordenadas de las balizas
        val beaconCoordinates = beaconsWithPosition.map { it.position }

        // Obtener las distancias desde el dispositivo hasta las balizas
        val distances = beaconsWithPosition.map { beacon ->
            val distance = deviceList.find { it.first == beacon.uuid }?.second
            distance ?: beacon.distance // Usar la distancia predeterminada si no se encuentra en deviceList
        }

        // Aplicar el filtro de media móvil para suavizar las mediciones
        val filteredDistances = applyMovingAverageFilter(distances)

        // Realizar la trilateración para calcular la posición relativa del dispositivo
        return trilaterate(beaconCoordinates, filteredDistances)
    }

    private fun applyMovingAverageFilter(distances: List<Double>): List<Double> {
        val windowSize = 3 // Tamaño de la ventana del filtro de media móvil
        val filteredDistances = mutableListOf<Double>()

        for (i in distances.indices) {
            val startIndex = maxOf(0, i - windowSize + 1)
            val endIndex = i + 1
            val values = distances.subList(startIndex, endIndex)
            val average = values.average()
            filteredDistances.add(average)
        }
        return filteredDistances
    }

    // Función de trilateración para calcular la posición relativa
    private fun trilaterate(beaconCoordinates: List<Coordinate>, distances: List<Double>): Position? {
        if (beaconCoordinates.size != 3 || distances.size != 3) {
            return null // Necesitamos exactamente tres balizas y tres distancias
        }

        // Coordenadas de las balizas
        val p1 = beaconCoordinates[0]
        val p2 = beaconCoordinates[1]
        val p3 = beaconCoordinates[2]

        // Distancias a las balizas
        val d1 = distances[0]
        val d2 = distances[1]
        val d3 = distances[2]

        // Calcular las distancias geodésicas entre las balizas y el dispositivo
        val distanceP1 = haversine(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        val distanceP2 = haversine(p1.latitude, p1.longitude, p3.latitude, p3.longitude)
        val distanceP3 = haversine(p2.latitude, p2.longitude, p3.latitude, p3.longitude)

        // Calcular la posición del dispositivo utilizando la trilateración
        val x = (d1 * d1 - d2 * d2 + distanceP1 * distanceP1) / (2 * distanceP1)
        val y = ((d1 * d1 - d3 * d3 + distanceP2 * distanceP2 - d2 * d2 + distanceP1 * distanceP1) / (2 * distanceP1) -
                x * (d2 / distanceP1)) * (distanceP3 / distanceP1)

        // Calcular las coordenadas del dispositivo
        val latitude = p1.latitude + x * (p2.latitude - p1.latitude) / distanceP1 + y * (p3.latitude - p1.latitude) / distanceP1
        val longitude = p1.longitude + x * (p2.longitude - p1.longitude) / distanceP1 + y * (p3.longitude - p1.longitude) / distanceP1

        // Crear y devolver la posición estimada
        val position = Position(latitude, longitude, 0.0)
        Log.e("Position", "Mi nueva posición es: Latitud=${position.latitude}, Longitud=${position.longitude}")

        return position
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // Radio de la Tierra en km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c * 1000 // Convertir a metros
    }
}

data class Coordinate(val latitude: Double, val longitude: Double)

data class Position(val latitude: Double, val longitude: Double, val z: Double)

data class BeaconWithPosition(val name: String, val uuid: String, val position: Coordinate, val distance: Double)
