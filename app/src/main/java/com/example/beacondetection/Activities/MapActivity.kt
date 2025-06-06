package com.example.beacondetection.Activities

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.beacondetection.DB.FirestoreHelper
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
    private var selectedBeacon: BeaconWithPosition? = null
    private lateinit var directionTextView: TextView // Añadir esta línea
    private var hasArrived = false

    private var positionUpdateTimer: Timer? = null

    private val beaconsWithPosition = listOf(
        BeaconWithPosition("Escritorio","11111111-1111-1111-1111-111111111111", Coordinate(37.18847724141154, -3.6031642616322315), 0.0),
        BeaconWithPosition("Terraza","22222222-2222-2222-2222-222222222222", Coordinate(37.18846670253076, -3.603250017296174), 0.0),
        BeaconWithPosition("Inventario","33333333-3333-3333-3333-333333333333", Coordinate(37.1885425870942, -3.6031311379982864), 0.0)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        directionTextView = findViewById(R.id.directionTextView)

        // Obtener las opciones seleccionadas
        val showMyLocation = intent.getBooleanExtra("showMyLocation", false)
        val showBeacons = intent.getBooleanExtra("showBeacons", false)
        val showBoth = intent.getBooleanExtra("showBoth", false)

        // Obtener una instancia de FirestoreHelper
        val dbHelper = FirestoreHelper.getInstance(this)

        // Configurar el agente de usuario
        val userAgentValue = "BeaconDetection"
        Configuration.getInstance().userAgentValue = userAgentValue

        // Configurar la ubicación inicial
        val initialPosition = Position(37.18847724141154, -3.6031642616322315, 0.0)

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

        // Mostrar balizas detectadas si es necesario
        if (showBeacons || showBoth) {
            showBeaconsOnMap()
        }

        // Iniciar un temporizador para actualizar la posición cada medio segundo si es necesario
        if (showMyLocation || showBoth) {
            positionUpdateTimer = Timer()
            positionUpdateTimer?.scheduleAtFixedRate(timerTask {
                runOnUiThread {
                    updatePositionOnMap()
                }
            }, 200, 200)
        }

        // Configurar el cuadro de texto y el botón
        val inputLocation = findViewById<EditText>(R.id.input_location)
        val btnGo = findViewById<Button>(R.id.btn_go)
        btnGo.setOnClickListener {
            val location = inputLocation.text.toString()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(inputLocation.windowToken, 0)

            if (location.isNotEmpty()) {
                val matchedBeacon =
                    beaconsWithPosition.find { it.name.contains(location, ignoreCase = true) }
                if (matchedBeacon != null) {
                    val point =
                        GeoPoint(matchedBeacon.position.latitude, matchedBeacon.position.longitude)
                    mapController.setCenter(point)
                    mapController.setZoom(22.0)

                    // Guardar la baliza seleccionada
                    selectedBeacon = matchedBeacon

                    // Mostrar información de la baliza en el marcador existente
                    showBeaconInfo(matchedBeacon.uuid)

                    mapView.invalidate()
                } else {
                    Toast.makeText(
                        this,
                        "No se encontró ninguna baliza con ese nombre",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(this, "Por favor, ingresa una ubicación", Toast.LENGTH_SHORT).show()
            }
            inputLocation.text.clear()
        }
    }
    override fun onBackPressed() {
        // Crear el diálogo de confirmación
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Detener actualización de posición")
            .setMessage("¿Desea detener el servicio de actualización de posición?")
            .setPositiveButton("Sí") { _, _ ->
                // Detener el Timer y servicio de cálculo
                positionUpdateTimer?.cancel()
                positionUpdateTimer = null
                super.onBackPressed()
            }
            .setNegativeButton("No") { _, _ ->
                super.onBackPressed()
            }
            .create()
        // Mostrar el diálogo
        alertDialog.show()
    }


    private fun showBeaconsOnMap() {
        beaconsWithPosition.forEach { beacon ->
            // Crear un círculo de 5 metros de radio alrededor del marcador de la baliza
            val circle = Polygon(mapView)
            circle.points = Polygon.pointsAsCircle(GeoPoint(beacon.position.latitude, beacon.position.longitude), 5.0)
            circle.fillColor = 0x12121212
            circle.strokeColor = 0x12121212
            circle.strokeWidth = 1f
            circle.infoWindow = null
            mapView.overlays.add(circle)

            // Crear el marcador de la baliza
            val beaconMarker = Marker(mapView)
            beaconMarker.position = GeoPoint(beacon.position.latitude, beacon.position.longitude)
            beaconMarker.title = beacon.name
            beaconMarker.icon = resources.getDrawable(R.drawable.ic_marker_red, null)
            beaconMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

            mapView.overlays.add(beaconMarker)
        }
        mapView.invalidate() // Actualizar el mapa
    }

    private fun showBeaconInfo(uuid: String) {
        val marker = mapView.overlays.find { it is Marker && (it as Marker).title == beaconsWithPosition.find { beacon -> beacon.uuid == uuid }?.name } as? Marker
        marker?.showInfoWindow()
    }

    // Función para actualizar la posición en el mapa
    private fun updatePositionOnMap() {
        val dbHelper = FirestoreHelper.getInstance(this)
        dbHelper.getAllDevicesUuidAndDistance { devices ->
            deviceList = devices
            val estimatedPosition = calculatePosition()
            estimatedPosition?.let {
                Log.d("MapActivity", "Estimated position: Latitude=${it.latitude}, Longitude=${it.longitude}")

                // Actualizar la posición del marcador del dispositivo
                val devicePoint = GeoPoint(it.latitude, it.longitude)
                deviceMarker?.position = devicePoint
                deviceMarker?.title = "Mi posición estimada"
                mapView.invalidate()

                // Calcular y mostrar la dirección hacia la baliza seleccionada
                selectedBeacon?.let { beacon ->
                    val distanceToBeacon = devices.find { it.first == beacon.uuid }?.second ?: Double.MAX_VALUE
                    Log.d("MapActivity", "Distance to beacon: $distanceToBeacon")
                    if (distanceToBeacon < 5.0 && !hasArrived) {
                        showArrivalPopup()
                        directionTextView.visibility = View.GONE
                        hasArrived = true
                    } else if (distanceToBeacon >= 5.0) {
                        val direction = calculateDirectionToBeacon(it, beacon.position)
                        directionTextView.text = direction
                        directionTextView.visibility = View.VISIBLE
                        hasArrived = false
                    }
                }
            } ?: run {
                Log.d("MapActivity", "Unable to estimate position.")
            }
        }
    }

    private fun showArrivalPopup() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("¡Has llegado!")
        builder.setMessage("Estás a menos de 5 metros de la baliza.")
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

    private fun calculateDirectionToBeacon(userPosition: Position, beaconPosition: Coordinate): String {
        val deltaX = beaconPosition.longitude - userPosition.longitude
        val deltaY = beaconPosition.latitude - userPosition.latitude
        val angle = Math.toDegrees(Math.atan2(deltaY, deltaX))
        return when {
            angle >= -45 && angle <= 45 -> "Camina hacia adelante"
            angle > 45 && angle < 135 -> "Camina hacia la derecha"
            angle >= 135 || angle <= -135 -> "Camina hacia atrás"
            else -> "Camina hacia la izquierda"
        }
    }


    // Función para calcular la posición relativa
    private fun calculatePosition(): Position? {
        if (deviceList.size < 3) {
            Log.d("MapActivity", "Not enough beacons for trilateration. Need at least 3.")
            return null
        }
        val knownBeacons = deviceList.filter { (uuid, _) ->
            beaconsWithPosition.any { it.uuid == uuid }
        }
        if (knownBeacons.size < 3) {
            return null
        }
        val closestBeacons = knownBeacons.sortedBy { it.second }.take(3)
        val beaconCoordinates = mutableListOf<Coordinate>()
        val distances = mutableListOf<Double>()

        closestBeacons.forEach { (uuid, distance) ->
            val beacon = beaconsWithPosition.find { it.uuid == uuid }
            if (beacon != null) {
                beaconCoordinates.add(beacon.position)
                distances.add(distance)
            }
        }
        if (beaconCoordinates.size < 3) {
            return null
        }

        val filteredDistances = applyMovingAverageFilter(distances)
        return trilaterate(beaconCoordinates, filteredDistances)
    }


    private fun applyMovingAverageFilter(distances: List<Double>): List<Double> {
        val windowSize = 3
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
            return null
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
