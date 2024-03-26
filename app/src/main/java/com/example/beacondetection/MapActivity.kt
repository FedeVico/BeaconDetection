package com.example.beacondetection

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.beacondetection.DB.SQLiteHelper
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapActivity : AppCompatActivity() {

    private lateinit var deviceList: ArrayList<Pair<String, Double>>

    private val beaconsWithPosition = listOf(
        BeaconWithPosition("11111111-1111-1111-1111-111111111111", Coordinate(37.58662, -4.64204), 0.0),
        BeaconWithPosition("22222222-2222-2222-2222-222222222222", Coordinate(37.58673, -4.64180), 0.0),
        BeaconWithPosition("33333333-3333-3333-3333-333333333333", Coordinate(37.58656, -4.64189), 0.0)
    )
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Obtener una instancia de SQLiteHelper
        val dbHelper = SQLiteHelper.getInstance(this)
        dbHelper.openDatabase()

        // Obtener la lista de dispositivos con uuid y distancia
        deviceList = dbHelper.getAllDevicesUuidAndDistance()

        // Configurar el agente de usuario
        val userAgentValue = "BeaconDetection"
        Configuration.getInstance().userAgentValue = userAgentValue

        // Configurar la ubicación inicial
        val initialPosition = Position(37.586621804773564, -4.641860098344363, 0.0)

        // Mostrar la posición inicial en el mapa
        showPositionOnMap(initialPosition)

        // Cerrar la base de datos cuando hayas terminado
        //dbHelper.closeDatabase()
    }

    // Función para mostrar la posición en el mapa
    private fun showPositionOnMap(position: Position) {
        val mapView = findViewById<MapView>(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val mapController = mapView.controller
        val startPoint = GeoPoint(position.latitude, position.longitude)
        mapController.setCenter(startPoint)
        mapController.setZoom(20.0)

        val marker = Marker(mapView)
        marker.position = startPoint
        marker.title = "Ubicación Inicial"
        mapView.overlays.add(marker)

        // Calcular la posición relativa (si puede) y mostrarla
        if (deviceList.isNotEmpty()) {
            val estimatedPosition = calculatePosition()
            if (estimatedPosition != null) {
                val estimatedPoint = GeoPoint(estimatedPosition.latitude, estimatedPosition.longitude)
                mapController.setCenter(estimatedPoint)

                val estimatedMarker = Marker(mapView)
                estimatedMarker.position = estimatedPoint
                estimatedMarker.title = "Mi posición estimada"
                mapView.overlays.add(estimatedMarker)
            }
        }

        // Añadir marcas de las balizas
        addBeaconMarkers(mapView)
    }

    // Función para añadir marcas de las balizas
    private fun addBeaconMarkers(mapView: MapView) {
        for (beacon in beaconsWithPosition) {
            val marker = Marker(mapView)
            marker.position = GeoPoint(beacon.position.latitude, beacon.position.longitude)
            marker.icon = getMarkerIcon(this)
            mapView.overlays.add(marker)
        }
    }

    private fun getMarkerIcon(context: Context): Drawable? {
        return ContextCompat.getDrawable(context, R.drawable.ic_marker_red)
    }

    // Función para calcular la posición relativa
    private fun calculatePosition(): Position? {
        // Verificar si hay suficientes balizas para la trilateración (al menos 3)
        if (deviceList.size < 3) {
            return null
        }

        // Filtrar los datos para obtener solo las balizas con información de posición (coordenadas)
        //val beaconsWithPosition = deviceList.filterIsInstance<BeaconWithPosition>()

        // Obtener las coordenadas de las balizas
        val beaconCoordinates = beaconsWithPosition.map { it.position }

        // Obtener las distancias desde el dispositivo hasta las balizas
        val distances = beaconsWithPosition.map { it.distance }

        // Realizar la trilateración para calcular la posición relativa del dispositivo
        return trilaterate(beaconCoordinates, distances)
    }

    // Función de trilateración para calcular la posición relativa
    private fun trilaterate(beaconCoordinates: List<Coordinate>, distances: List<Double>): Position {
        val p1 = beaconCoordinates[0]
        val p2 = beaconCoordinates[1]
        val p3 = beaconCoordinates[2]

        val d1 = distances[0]
        val d2 = distances[1]
        val d3 = distances[2]

        // Calcula las diferencias entre los cuadrados de las coordenadas
        val x1 = p2.latitude - p1.latitude
        val x2 = p3.latitude - p1.latitude
        val y1 = p2.longitude - p1.longitude
        val y2 = p3.longitude - p1.longitude

        // Calcula el coeficiente para la ecuación cuadrática
        val a = ((d1 * d1) - (d2 * d2) + (x2 * x2) + (y2 * y2)) / 2
        val b = ((d1 * d1) - (d3 * d3) + (x2 * x2) + (y2 * y2)) / 2

        // Resuelve el sistema de ecuaciones
        val denominator = (x1 * y2) - (x2 * y1)

        val posX = ((a * y2) - (b * y1)) / denominator
        val posY = ((x1 * b) - (x2 * a)) / denominator

        return Position(posX + p1.latitude, posY + p1.longitude, 0.0)
    }
}

data class Coordinate(val latitude: Double, val longitude: Double)

data class Position(val latitude: Double, val longitude: Double, val z: Double)

data class BeaconWithPosition(val uuid: String, val position: Coordinate, val distance: Double)
