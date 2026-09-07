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
            text = "Night Light / Eco Mode (Verified)"
            setOnClickListener { sendCommand("night_light") }
        }
        container.addView(nightBtn)

        // Brightness + Button (Verified Working!)
        val brightMaxBtn = Button(this).apply {
            text = "Max Brightness + (Verified)"
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

        // Interactive Signal Fine-Tuner Matrix
        val matrixLabel = TextView(this).apply {
            text = "Lazada CCT Signal Fine-Tuner Matrix (LSB Official):"
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 20, 0, 8)
        }
        container.addView(matrixLabel)

        // Color Switch Code Matrix
        val colorMatrixLabel = TextView(this).apply {
            text = "Test 3-Color Kelvin Switch Codes:"
            setPadding(0, 8, 0, 4)
        }
        container.addView(colorMatrixLabel)

        val colorCodes = listOf(
            "Color Code 1 (0xB0)" to 0xB0,
            "Color Code 2 (0x1C)" to 0x1C,
            "Color Code 3 (0x0C)" to 0x0C,
            "Color Code 4 (0x20)" to 0x20,
            "Color Code 5 (0x48)" to 0x48,
            "Color Code 6 (0x68)" to 0x68,
            "Color Code 7 (0x88)" to 0x88,
            "Color Code 8 (0xE8)" to 0xE8,
            "Color Code 9 (0x28)" to 0x28,
            "Color Code 10 (0x14)" to 0x14,
            "Color Code 11 (0x04)" to 0x04
        )

        for ((name, cmdByte) in colorCodes) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val testBtn = Button(this).apply {
                text = name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f).apply { marginEnd = 8 }
                setOnClickListener {
                    val raw = IrCodeDatabase.necLsbToRawPattern(0x00, cmdByte)
                    irRepository.transmit(38000, raw)
                    Toast.makeText(this@ControlActivity, "Transmitted $name (LSB)", Toast.LENGTH_SHORT).show()
                }
            }
            val setBtn = Button(this).apply {
                text = "Save as Color Switch"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    val updated = appliance.commandMap.toMutableMap()
                    updated["color_temp_cycle"] = IrCodeDatabase.necLsbToRawPattern(0x00, cmdByte)
                    appliance = appliance.copy(commandMap = updated)
                    updateDeviceStorage(appliance)
                    Toast.makeText(this@ControlActivity, "Saved $name as Color Switch!", Toast.LENGTH_SHORT).show()
                }
            }
            row.addView(testBtn)
            row.addView(setBtn)
            container.addView(row)
        }

        // Power Code Matrix
        val powerMatrixLabel = TextView(this).apply {
            text = "Test Power On/Off Codes:"
            setPadding(0, 16, 0, 4)
        }
        container.addView(powerMatrixLabel)

        val powerCodes = listOf(
            "Power Code 1 (0x02)" to 0x02,
            "Power Code 2 (0x12)" to 0x12,
            "Power Code 3 (0x00)" to 0x00,
            "Power Code 4 (0x0A)" to 0x0A,
            "Power Code 5 (0x01)" to 0x01,
            "Power Code 6 (0x03)" to 0x03,
            "Power Code 7 (0x1A)" to 0x1A,
            "Power Code 8 (0x1E)" to 0x1E,
            "Power Code 9 (0x40)" to 0x40
        )

        for ((name, cmdByte) in powerCodes) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val testBtn = Button(this).apply {
                text = name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f).apply { marginEnd = 8 }
                setOnClickListener {
                    val raw = IrCodeDatabase.necLsbToRawPattern(0x00, cmdByte)
                    irRepository.transmit(38000, raw)
                    Toast.makeText(this@ControlActivity, "Transmitted $name (LSB)", Toast.LENGTH_SHORT).show()
                }
            }
            val setBtn = Button(this).apply {
                text = "Save as Power Code"
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
            row.addView(setBtn)
            container.addView(row)
        }

        // Dimmer / Brightness Down Matrix
        val dimMatrixLabel = TextView(this).apply {
            text = "Test Brightness - (Dimmer) Codes:"
            setPadding(0, 16, 0, 4)
        }
        container.addView(dimMatrixLabel)

        val dimCodes = listOf(
            "Dimmer Code 1 (0x10)" to 0x10,
            "Dimmer Code 2 (0x08)" to 0x08,
            "Dimmer Code 3 (0xA0)" to 0xA0,
            "Dimmer Code 4 (0x80)" to 0x80
        )

        for ((name, cmdByte) in dimCodes) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val testBtn = Button(this).apply {
                text = name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f).apply { marginEnd = 8 }
                setOnClickListener {
                    val raw = IrCodeDatabase.necLsbToRawPattern(0x00, cmdByte)
                    irRepository.transmit(38000, raw)
                    Toast.makeText(this@ControlActivity, "Transmitted $name (LSB)", Toast.LENGTH_SHORT).show()
                }
            }
            val setBtn = Button(this).apply {
                text = "Save as Dimmer"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    val updated = appliance.commandMap.toMutableMap()
                    updated["brightness_down"] = IrCodeDatabase.necLsbToRawPattern(0x00, cmdByte)
                    appliance = appliance.copy(commandMap = updated)
                    updateDeviceStorage(appliance)
                    Toast.makeText(this@ControlActivity, "Saved $name as Dimmer!", Toast.LENGTH_SHORT).show()
                }
            }
            row.addView(testBtn)
            row.addView(setBtn)
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
