package com.example.universalir

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.universalir.hardware.IrCodeDatabase
import com.example.universalir.hardware.IrRepository
import com.example.universalir.model.Appliance
import com.example.universalir.model.ApplianceType
import com.example.universalir.model.DeviceStorage
import com.google.gson.Gson

class ControlActivity : AppCompatActivity() {

    private lateinit var irRepository: IrRepository
    private lateinit var appliance: Appliance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Device Controls"

        irRepository = IrRepository(this)

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

        // Top "Back to Dashboard" button
        val backBtn = Button(this).apply {
            text = "← Back to Main Dashboard"
            setOnClickListener { finish() }
        }
        containerLayout.addView(backBtn)

        when (appliance.type) {
            ApplianceType.LAMP -> renderLampControls(containerLayout)
            ApplianceType.AC -> renderAcControls(containerLayout)
            ApplianceType.TV, ApplianceType.SOUNDBAR, ApplianceType.MEDIA_PLAYER -> renderStandardPowerControls(containerLayout)
            else -> renderStandardPowerControls(containerLayout)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun renderLampControls(container: LinearLayout) {
        val label = TextView(this).apply {
            text = "3-Color Kelvin & Dimmer Controls"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 16, 0, 12)
        }
        container.addView(label)

        // Protocol / Address Selector
        val protocolLabel = TextView(this).apply {
            text = "Fine-Tune Protocol / IR Address Variant:"
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 4)
        }
        container.addView(protocolLabel)

        val protocolSpinner = Spinner(this)
        val protocols = listOf(
            "Protocol 2B: Lazada CCT Lamp (0x00FF - LSB Official)",
            "Protocol 2A: Standard 24-Key RGB (0x00FF)",
            "Protocol 2C: CCT 21-Key Controller (0x00FF)",
            "Protocol 2D: CCT Driver Pro (0x00FF)",
            "Protocol 1: Lazada Nordic Lamp (0x00EF)",
            "Protocol 3: CCT 3-Color Driver (0x0000)",
            "Protocol 4: Mini LED Controller (0x00BF)",
            "Protocol 5: 44-Key LED Controller (0x00F7)",
            "Protocol 6: Tuya Smart IR (0x708F)"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, protocols)
        protocolSpinner.adapter = adapter
        container.addView(protocolSpinner)

        protocolSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val updatedMap = when (position) {
                    0 -> IrCodeDatabase.getLampProtocolMapForVariant(0x00FF0000L, 1)
                    1 -> IrCodeDatabase.getLampProtocolMapForVariant(0x00FF0000L, 0)
                    2 -> IrCodeDatabase.getLampProtocolMapForVariant(0x00FF0000L, 2)
                    3 -> IrCodeDatabase.getLampProtocolMapForVariant(0x00FF0000L, 3)
                    4 -> IrCodeDatabase.getLampProtocolMapForVariant(0x00EF0000L, 0)
                    5 -> IrCodeDatabase.getLampProtocolMapForVariant(0x00000000L, 0)
                    6 -> IrCodeDatabase.getLampProtocolMapForVariant(0x00BF0000L, 0)
                    7 -> IrCodeDatabase.getLampProtocolMapForVariant(0x00F70000L, 0)
                    8 -> IrCodeDatabase.getLampProtocolMapForVariant(0x708F0000L, 0)
                    else -> IrCodeDatabase.getLampProtocolMapForVariant(0x00FF0000L, 1)
                }
                appliance = appliance.copy(commandMap = updatedMap)
                updateDeviceStorage(appliance)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Night Light Mode Button (Verified Working!)
        val nightBtn = Button(this).apply {
            text = "Night Light / Eco Mode (0x1C Verified)"
            setOnClickListener { sendCommand("night_light") }
        }
        container.addView(nightBtn)

        // Brightness + Button (Verified Working!)
        val brightMaxBtn = Button(this).apply {
            text = "Max Brightness + (0x18 Verified)"
            setOnClickListener { sendCommand("brightness_up") }
        }
        container.addView(brightMaxBtn)

        // Power Toggle Button
        val powerBtn = Button(this).apply {
            text = "Power On / Off"
            setOnClickListener { sendCommand("power_toggle") }
        }
        container.addView(powerBtn)

        // Color Temperature Switch
        val kelvinBtn = Button(this).apply {
            text = "Switch Light Color (3000K / 4000K / 6500K)"
            setOnClickListener { sendCommand("color_temp_cycle") }
        }
        container.addView(kelvinBtn)

        // BAIERDI 0x10–0x1F Block Test Suite
        val baierdiMatrixLabel = TextView(this).apply {
            text = "BAIERDI 0x10–0x1F Command Suite (0x00FF LSB):"
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 20, 0, 8)
        }
        container.addView(baierdiMatrixLabel)

        val baierdiBlock = listOf(
            "0x10 (Dimmer -)" to 0x10,
            "0x11 (Warm White 3000K)" to 0x11,
            "0x12 (Power / CCT Switch)" to 0x12,
            "0x13 (Cool White 6500K)" to 0x13,
            "0x14 (Power Toggle / ON)" to 0x14,
            "0x15 (Power OFF)" to 0x15,
            "0x16 (Brightness +)" to 0x16,
            "0x17 (Color Temp Cycle)" to 0x17,
            "0x18 (Max Brightness - Verified)" to 0x18,
            "0x19 (50% Brightness)" to 0x19,
            "0x1A (30% Warm Night)" to 0x1A,
            "0x1B (Warmer Shift)" to 0x1B,
            "0x1C (Night Light - Verified)" to 0x1C,
            "0x1D (Cooler Shift)" to 0x1D,
            "0x1E (30m Timer)" to 0x1E,
            "0x1F (60m Timer)" to 0x1F
        )

        for ((name, cmdByte) in baierdiBlock) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val testBtn = Button(this).apply {
                text = name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f).apply { marginEnd = 8 }
                setOnClickListener {
                    val raw = IrCodeDatabase.necLsbToRawPattern(0x00, cmdByte)
                    irRepository.transmit(38000, raw)
                    Toast.makeText(this@ControlActivity, "Sent $name", Toast.LENGTH_SHORT).show()
                }
            }
            val saveColorBtn = Button(this).apply {
                text = "Save as Color"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 4 }
                setOnClickListener {
                    val updated = appliance.commandMap.toMutableMap()
                    updated["color_temp_cycle"] = IrCodeDatabase.necLsbToRawPattern(0x00, cmdByte)
                    appliance = appliance.copy(commandMap = updated)
                    updateDeviceStorage(appliance)
                    Toast.makeText(this@ControlActivity, "Saved $name as Color Switch!", Toast.LENGTH_SHORT).show()
                }
            }
            val savePowerBtn = Button(this).apply {
                text = "Save Power"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    val updated = appliance.commandMap.toMutableMap()
                    updated["power_toggle"] = IrCodeDatabase.necLsbToRawPattern(0x00, cmdByte)
                    appliance = appliance.copy(commandMap = updated)
                    updateDeviceStorage(appliance)
                    Toast.makeText(this@ControlActivity, "Saved $name as Power Code!", Toast.LENGTH_SHORT).show()
                }
            }
            row.addView(testBtn)
            row.addView(saveColorBtn)
            row.addView(savePowerBtn)
            container.addView(row)
        }
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

    private fun updateDeviceStorage(updatedAppliance: Appliance) {
        val deviceStorage = DeviceStorage(this)
        val devices = deviceStorage.loadDevices()
        val index = devices.indexOfFirst { it.id == updatedAppliance.id }
        if (index >= 0) {
            devices[index] = updatedAppliance
            deviceStorage.saveDevices(devices)
        }
    }

    private fun sendCommand(actionKey: String) {
        val pattern = appliance.commandMap[actionKey]
            ?: appliance.commandMap["power_toggle"]
            ?: intArrayOf(9000, 4500, 560, 560)
            
        irRepository.transmit(38000, pattern)
        Toast.makeText(this, "Sent ${actionKey.replace('_', ' ')} command", Toast.LENGTH_SHORT).show()
    }
}
