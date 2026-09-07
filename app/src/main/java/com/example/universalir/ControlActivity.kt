package com.example.universalir

import android.graphics.Typeface
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
            text = "3-Color Kelvin & Dimmer Controls"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        container.addView(label)

        // Power Toggle Button
        val powerBtn = Button(this).apply {
            text = "Power On / Off"
            setOnClickListener { sendCommand("power_toggle") }
        }
        container.addView(powerBtn)

        // Color Temperature (3-Color Kelvin Switch: Warm / Natural / Cool)
        val kelvinBtn = Button(this).apply {
            text = "Switch Light Color (3000K / 4000K / 6500K)"
            setOnClickListener { sendCommand("color_temp_cycle") }
        }
        container.addView(kelvinBtn)

        // Brightness Controls Row
        val brightLabel = TextView(this).apply {
            text = "Brightness Adjustment:"
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 16, 0, 8)
        }
        container.addView(brightLabel)

        val brightLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val brightUpBtn = Button(this).apply {
            text = "Brightness +"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            }
            setOnClickListener { sendCommand("brightness_up") }
        }
        val brightDownBtn = Button(this).apply {
            text = "Brightness -"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { sendCommand("brightness_down") }
        }
        brightLayout.addView(brightUpBtn)
        brightLayout.addView(brightDownBtn)
        container.addView(brightLayout)

        // Color Temperature Shift Row (Warm / Cool)
        val tempLabel = TextView(this).apply {
            text = "Kelvin Warm / Cool Shift:"
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 16, 0, 8)
        }
        container.addView(tempLabel)

        val tempLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val warmBtn = Button(this).apply {
            text = "Warmer (3000K)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            }
            setOnClickListener { sendCommand("warm_white") }
        }
        val coolBtn = Button(this).apply {
            text = "Cooler (6500K)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { sendCommand("cool_white") }
        }
        tempLayout.addView(warmBtn)
        tempLayout.addView(coolBtn)
        container.addView(tempLayout)

        // Night Light Mode Button
        val nightBtn = Button(this).apply {
            text = "Night Light / Eco Mode"
            setOnClickListener { sendCommand("night_light") }
        }
        container.addView(nightBtn)
    }

    private fun renderAcControls(container: LinearLayout) {
        val label = TextView(this).apply {
            text = "Air Conditioner Controls"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        container.addView(label)

        val powerBtn = Button(this).apply {
            text = "AC Power On / Off"
            setOnClickListener { sendCommand("power_toggle") }
        }
        container.addView(powerBtn)

        val tempLabel = TextView(this).apply {
            text = "Temperature Adjustment:"
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 16, 0, 8)
        }
        container.addView(tempLabel)

        val tempLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val tempUpBtn = Button(this).apply {
            text = "Temp +"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            }
            setOnClickListener { sendCommand("temp_up") }
        }
        val tempDownBtn = Button(this).apply {
            text = "Temp -"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { sendCommand("temp_down") }
        }
        tempLayout.addView(tempUpBtn)
        tempLayout.addView(tempDownBtn)
        container.addView(tempLayout)
    }

    private fun renderStandardPowerControls(container: LinearLayout) {
        val label = TextView(this).apply {
            text = "Device Controls"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        container.addView(label)

        val powerBtn = Button(this).apply {
            text = "Power On / Off"
            setOnClickListener { sendCommand("power_toggle") }
        }
        container.addView(powerBtn)

        val volLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }
        val volUpBtn = Button(this).apply {
            text = "Volume +"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            }
            setOnClickListener { sendCommand("vol_up") }
        }
        val volDownBtn = Button(this).apply {
            text = "Volume -"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { sendCommand("vol_down") }
        }
        volLayout.addView(volUpBtn)
        volLayout.addView(volDownBtn)
        container.addView(volLayout)
    }

    private fun sendCommand(actionKey: String) {
        val pattern = appliance.commandMap[actionKey]
            ?: appliance.commandMap["power_toggle"]
            ?: intArrayOf(9000, 4500, 560, 560)
            
        irRepository.transmit(38000, pattern)
        Toast.makeText(this, "Sent ${actionKey.replace('_', ' ')} command", Toast.LENGTH_SHORT).show()
    }
}
