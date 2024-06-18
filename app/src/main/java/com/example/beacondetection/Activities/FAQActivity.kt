package com.example.beacondetection.Activities

import android.os.Bundle
import android.widget.ExpandableListView
import androidx.appcompat.app.AppCompatActivity
import com.example.beacondetection.Adapters.FAQAdapter
import com.example.beacondetection.R

class FAQActivity : AppCompatActivity() {

    private lateinit var expandableListView: ExpandableListView
    private lateinit var faqAdapter: FAQAdapter
    private lateinit var questionList: List<String>
    private lateinit var answerList: HashMap<String, List<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)

        expandableListView = findViewById(R.id.faqListView)
        expandableListView.setSmoothScrollbarEnabled(true)
        expandableListView.setScrollingCacheEnabled(false)
        initData()
        faqAdapter = FAQAdapter(this, questionList, answerList, expandableListView)
        expandableListView.setAdapter(faqAdapter)
    }

    private fun initData() {
        questionList = listOf(
            "¿Cómo funciona esta aplicación?",
            "¿Qué son los beacons?",
            "¿Cómo puedo escanear beacons?",
            "¿Cómo acceder al mapa?",
            "¿Cómo mejorar la precisión del escaneo?",
            "¿Puedo agregar más beacons?"
        )

        answerList = HashMap()
        answerList[questionList[0]] = listOf("Esta aplicación permite escanear beacons y mostrar tu ubicación aproximada en un mapa en tiempo real.")
        answerList[questionList[2]] = listOf("Para escanear beacons, dirígete a la sección de escaneo desde el menú principal.")
        answerList[questionList[3]] = listOf("Puedes acceder al mapa desde el menú principal y ver tu ubicación en función a los beacons detectados.")
        answerList[questionList[1]] = listOf("Los beacons son dispositivos que emiten señales Bluetooth de baja energía.")
        answerList[questionList[4]] = listOf("Para mejorar la precisión, asegúrate de estar cerca del mayor número de beacons y evitar obstáculos.")
        answerList[questionList[5]] = listOf("Por ahora esta función no está disponible, pero más adelante será posible definir tus propias beacons.")
    }
}
