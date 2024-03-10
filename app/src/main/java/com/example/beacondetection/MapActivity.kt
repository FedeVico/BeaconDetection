package com.example.beacondetection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapActivity : AppCompatActivity() {

    private lateinit var deviceList: ArrayList<Any>
    private lateinit var estimatedPosition: Position

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Obtener la lista de dispositivos del intent
        deviceList = intent.getSerializableExtra("deviceList") as? ArrayList<Any> ?: arrayListOf()

        // Configurar el agente de usuario
        val userAgentValue = "BeaconDetection"
        Configuration.getInstance().userAgentValue = userAgentValue

        // Configurar la ubicación inicial
        val initialPosition = Position(37.586621804773564, -4.641860098344363, 0.0)

        // Mostrar la posición inicial en el mapa
        showPositionOnMap(initialPosition)
    }

    // Función para mostrar la posición en el mapa
    private fun showPositionOnMap(position: Position) {
        val mapView = findViewById<MapView>(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK) // Utiliza el proveedor de mapas MAPNIK (OpenStreetMap)
        mapView.setMultiTouchControls(true) // Habilita los controles de zoom multitáctiles

        val mapController = mapView.controller
        val startPoint = GeoPoint(position.latitude, position.longitude)
        mapController.setCenter(startPoint)
        mapController.setZoom(15.0)

        val marker = Marker(mapView)
        marker.position = startPoint
        marker.title = "Ubicación Inicial"
        mapView.overlays.add(marker)

        // Si se ha calculado la posición relativa, muestra también esa posición
        if (deviceList.isNotEmpty()) {
            // Calcular la posición relativa
            val estimatedPosition = calculatePosition()
            if (estimatedPosition != null) {
                val estimatedPoint = GeoPoint(estimatedPosition.latitude, estimatedPosition.longitude)
                mapController.setCenter(estimatedPoint)

                // Agregar un marcador en la posición estimada
                val estimatedMarker = Marker(mapView)
                estimatedMarker.position = estimatedPoint
                estimatedMarker.title = "Mi posición estimada"
                mapView.overlays.add(estimatedMarker)
            }
        }
    }

    // Función para calcular la posición relativa
    private fun calculatePosition(): Position? {
        // Verificar si hay suficientes balizas para la trilateración (al menos 3)
        if (deviceList.size < 3) {
            return null
        }

        // Filtrar los datos para obtener solo las balizas con información de posición (coordenadas)
        val beaconsWithPosition = deviceList.filterIsInstance<BeaconWithPosition>()

        // Verificar si hay suficientes balizas con información de posición (al menos 3)
        if (beaconsWithPosition.size < 3) {
            return null
        }

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

    data class BeaconWithPosition(val position: Coordinate, val distance: Double)

