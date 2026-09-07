package com.example.universalir

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.universalir.hardware.IrRepository
import com.example.universalir.model.Appliance
import com.example.universalir.model.ApplianceType
import com.google.gson.Gson

class ControlActivity : AppCompatActivity() {

    private lateinit var irRepository: IrRepository
    private lateinit var appliance: Appliance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        irRepository = IrRepository(this)

        // Receive the serialized appliance object passed from the main dashboard
        val applianceJson = intent.getStringExtra("appliance_extra")
        if (applianceJson == null) {
            Toast.makeText(this, "Error loading device profile", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        appliance = Gson().fromJson(applianceJson, Appliance::class.java)

        val titleTextView = findViewById<TextView>(R.id.deviceTitleTextView)
        val containerLayout = findViewById<LinearLayout>(R.id.controlsContainerLayout)

        titleTextView.text = appliance.name

        // Render controls dynamically based on the appliance type
        when (appliance.type) {
            ApplianceType.LAMP -> renderLampControls(containerLayout)
            ApplianceType.AC -> renderAcControls(containerLayout)
            ApplianceType.TV, ApplianceType.SOUNDBAR, ApplianceType.MEDIA_PLAYER -> renderStandardPowerControls(containerLayout)
            else -> renderStandardPowerControls(containerLayout)
        }
    }

    private fun renderLampControls(container: LinearLayout) {
        val label = TextView(this).apply {
            text = "Dimmable & Kelvin Light Controls"
            textSize = 16f
            setPadding(0, 0, 0, 16)
        }
        container.addView(label)

        // Power Toggle Button
        val powerBtn = Button(this).apply {
            text = "Power Toggle"
            setOnClickListener { sendCommand("power_toggle") }
        }
        container.addView(powerBtn)

        // Brightness Slider label
        val sliderLabel = TextView(this).apply { text = "Brightness Level" }
        container.addView(sliderLabel)

        // Brightness Slider
        val seekBar = SeekBar(this).apply {
            max = 100
            progress = 50
            setPadding(0, 20, 0, 40)
        }
        container.addView(seekBar)

        val applyBrightnessBtn = Button(this).apply {
            text = "Apply Brightness Level"
            setOnClickListener {
                Toast.makeText(this@ControlActivity, "Set brightness to ${seekBar.progress}%", Toast.LENGTH_SHORT).show()
                sendCommand("power_toggle") // Maps to specific PWM/IR payload in production
            }
        }
        container.addView(applyBrightnessBtn)
    }

    private fun renderAcControls(container: LinearLayout) {
        val label = TextView(this).apply {
            text = "Air Conditioner State Panel"
            textSize = 16f
            setPadding(0, 0, 0, 16)
        }
        container.addView(label)

        val powerBtn = Button(this).apply {
            text = "AC Power On/Off"
            setOnClickListener { sendCommand("power_toggle") }
        }
        container.addView(powerBtn)

        val tempLabel = TextView(this).apply { text = "Target Temperature: 24°C" }
        container.addView(tempLabel)

        val tempUpBtn = Button(this).apply {
            text = "Temp + (Up)"
            setOnClickListener { Toast.makeText(this@ControlActivity, "Temp Increased", Toast.LENGTH_SHORT).show() }
        }
        container.addView(tempUpBtn)

        val tempDownBtn = Button(this).apply {
            text = "Temp - (Down)"
            setOnClickListener { Toast.makeText(this@ControlActivity, "Temp Decreased", Toast.LENGTH_SHORT).show() }
        }
        container.addView(tempDownBtn)
    }

    private fun renderStandardPowerControls(container: LinearLayout) {
        val powerBtn = Button(this).apply {
            text = "Power Toggle / Action"
            setOnClickListener { sendCommand("power_toggle") }
        }
        container.addView(powerBtn)
    }

    private fun sendCommand(actionKey: String) {
        val pattern = appliance.commandMap[actionKey] ?: intArrayOf(9000, 4500, 560, 560)
        irRepository.transmit(38000, pattern)
        Toast.makeText(this, "Sent command for ${appliance.name}!", Toast.LENGTH_SHORT).show()
    }
}