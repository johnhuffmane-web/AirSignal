package com.example.universalir

import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
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

        // Single-Click Full Command Byte Auto-Scanner (Address 0x00FF)
        val fullScanBtn = Button(this).apply {
            text = "⚡ Auto-Scan All Bytes for Power & Color Switch (Address 0x00FF)"
            setOnClickListener { startFullByteAutoScan() }
        }
        container.addView(fullScanBtn)

        // Verified Dimmer Controls
        val dimmerLabel = TextView(this).apply {
            text = "Verified Dimmer States (Address 0x00FF LSB):"
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 16, 0, 8)
        }
        container.addView(dimmerLabel)

        val maxBtn = Button(this).apply {
            text = "100% Max Brightness (0x18 Verified)"
            setOnClickListener {
                val raw = IrCodeDatabase.necLsbToRawPattern(0x00, 0x18)
                irRepository.transmit(38000, raw)
                Toast.makeText(this@ControlActivity, "Sent 100% Max Brightness", Toast.LENGTH_SHORT).show()
            }
        }
        container.addView(maxBtn)

        val medBtn = Button(this).apply {
            text = "50% Medium Brightness (0x1C Verified)"
            setOnClickListener {
                val raw = IrCodeDatabase.necLsbToRawPattern(0x00, 0x1C)
                irRepository.transmit(38000, raw)
                Toast.makeText(this@ControlActivity, "Sent 50% Medium Brightness", Toast.LENGTH_SHORT).show()
            }
        }
        container.addView(medBtn)

        val lowBtn = Button(this).apply {
            text = "5% Low / Eco Mode (0x15 Verified)"
            setOnClickListener {
                val raw = IrCodeDatabase.necLsbToRawPattern(0x00, 0x15)
                irRepository.transmit(38000, raw)
                Toast.makeText(this@ControlActivity, "Sent 5% Low Brightness", Toast.LENGTH_SHORT).show()
            }
        }
        container.addView(lowBtn)

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

        // Multi-Address Header Test Suite
        val autoScanLabel = TextView(this).apply {
            text = "Find Power ON/OFF & Color Switch across Secondary Addresses:"
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 20, 0, 8)
        }
        container.addView(autoScanLabel)

        val autoScanLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val scanColorBtn = Button(this).apply {
            text = "Scan Secondary Color Switch"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 4 }
            setOnClickListener { startAutoScanForCommand("color_temp_cycle", 0x1C) }
        }
        val scanPowerBtn = Button(this).apply {
            text = "Scan Secondary Power OFF"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { startAutoScanForCommand("power_toggle", 0x00) }
        }
        autoScanLayout.addView(scanColorBtn)
        autoScanLayout.addView(scanPowerBtn)
        container.addView(autoScanLayout)
    }

    /**
     * Auto-scans all command bytes (0x00 to 0x3F) on Address 0x00FF LSB
     */
    private fun startFullByteAutoScan() {
        val byteList = listOf(
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            0x10, 0x11, 0x12, 0x13, 0x14, 0x16, 0x17, 0x19, 0x1A, 0x1B, 0x1D, 0x1E, 0x1F,
            0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x28, 0x2C, 0x30, 0x38, 0x3C, 0x40, 0x44, 0x48, 0x50, 0x60, 0x80, 0xA0, 0xB0, 0xC0, 0xD0, 0xE0, 0xF0
        )
        var idx = 0
        val handler = Handler(Looper.getMainLooper())

        val dialogView = layoutInflater.inflate(R.layout.dialog_scanning_progress, null)
        val titleText = dialogView.findViewById<TextView>(R.id.scanDeviceTypeTextView)
        val progressText = dialogView.findViewById<TextView>(R.id.scanProgressText)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.scanProgressBar)
        val instructionText = dialogView.findViewById<TextView>(R.id.scanInstructionText)
        val stopBtn = dialogView.findViewById<Button>(R.id.stopScanButton)
        val matchBtn = dialogView.findViewById<Button>(R.id.deviceReactedButton)

        titleText.text = "Auto-Scanning Command Bytes (Address 0x00FF)..."
        progressBar.max = byteList.size
        progressBar.progress = 1
        matchBtn.text = "Device Reacted! (Save)"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        var isScanning = true
        val runnable = object : Runnable {
            override fun run() {
                if (!isScanning || isFinishing || isDestroyed) return
                if (idx < byteList.size) {
                    val cmdByte = byteList[idx]
                    val raw = IrCodeDatabase.necLsbToRawPattern(0x00, cmdByte)
                    irRepository.transmit(38000, raw)

                    progressBar.progress = idx + 1
                    progressText.text = "Testing Byte ${idx + 1} of ${byteList.size} (0x${Integer.toHexString(cmdByte).uppercase()})"
                    instructionText.text = "Watch your BAIERDI lamp! Did it turn OFF/ON or switch light colors?"

                    idx++
                    handler.postDelayed(this, 1800)
                } else {
                    instructionText.text = "Finished testing command bytes."
                }
            }
        }

        stopBtn.setOnClickListener {
            isScanning = false
            handler.removeCallbacksAndMessages(null)
            dialog.dismiss()
        }

        matchBtn.setOnClickListener {
            isScanning = false
            handler.removeCallbacksAndMessages(null)
            dialog.dismiss()

            val matchedByte = if (idx > 0) byteList[idx - 1] else byteList[0]
            val matchedHex = "0x${Integer.toHexString(matchedByte).uppercase()}"

            val saveOptions = arrayOf("Save as Power ON/OFF Code", "Save as 3-Color Kelvin Switch Code", "Save as Warm White (3000K)")
            AlertDialog.Builder(this@ControlActivity)
                .setTitle("Match Found: Byte $matchedHex")
                .setItems(saveOptions) { _, choice ->
                    val updated = appliance.commandMap.toMutableMap()
                    val key = when (choice) {
                        0 -> "power_toggle"
                        1 -> "color_temp_cycle"
                        2 -> "warm_white"
                        else -> "power_toggle"
                    }
                    updated[key] = IrCodeDatabase.necLsbToRawPattern(0x00, matchedByte)
                    appliance = appliance.copy(commandMap = updated)
                    updateDeviceStorage(appliance)
                    Toast.makeText(this@ControlActivity, "Saved Byte $matchedHex as $key!", Toast.LENGTH_LONG).show()
                }
                .show()
        }

        dialog.show()
        handler.post(runnable)
    }

    private fun startAutoScanForCommand(actionKey: String, cmdByte: Int) {
        val addressList = listOf(0x00EF, 0x00BF, 0x0000, 0x708F, 0x00F7, 0x007F, 0x00DF, 0x10EF)
        var idx = 0
        val handler = Handler(Looper.getMainLooper())

        val dialogView = layoutInflater.inflate(R.layout.dialog_scanning_progress, null)
        val titleText = dialogView.findViewById<TextView>(R.id.scanDeviceTypeTextView)
        val progressText = dialogView.findViewById<TextView>(R.id.scanProgressText)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.scanProgressBar)
        val instructionText = dialogView.findViewById<TextView>(R.id.scanInstructionText)
        val stopBtn = dialogView.findViewById<Button>(R.id.stopScanButton)
        val matchBtn = dialogView.findViewById<Button>(R.id.deviceReactedButton)

        titleText.text = "Scanning Secondary Address Headers..."
        progressBar.max = addressList.size
        progressBar.progress = 1
        matchBtn.text = "Found Reaction! (Save)"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        var isScanning = true
        val runnable = object : Runnable {
            override fun run() {
                if (!isScanning || isFinishing || isDestroyed) return
                if (idx < addressList.size) {
                    val addr = addressList[idx]
                    val raw = IrCodeDatabase.necLsbToRawPattern(addr, cmdByte)
                    irRepository.transmit(38000, raw)

                    progressBar.progress = idx + 1
                    progressText.text = "Testing Address ${idx + 1} of ${addressList.size} (0x${Integer.toHexString(addr).uppercase()})"
                    instructionText.text = if (actionKey == "power_toggle") "Watch if lamp turns OFF or ON!" else "Watch if light color changes (Warm/White/Cool)!"

                    idx++
                    handler.postDelayed(this, 2000)
                } else {
                    instructionText.text = "Finished testing all secondary addresses."
                }
            }
        }

        stopBtn.setOnClickListener {
            isScanning = false
            handler.removeCallbacksAndMessages(null)
            dialog.dismiss()
        }

        matchBtn.setOnClickListener {
            isScanning = false
            handler.removeCallbacksAndMessages(null)
            dialog.dismiss()

            val matchedAddr = if (idx > 0) addressList[idx - 1] else addressList[0]
            val updated = appliance.commandMap.toMutableMap()
            updated[actionKey] = IrCodeDatabase.necLsbToRawPattern(matchedAddr, cmdByte)
            appliance = appliance.copy(commandMap = updated)
            updateDeviceStorage(appliance)
            Toast.makeText(this, "Saved Address 0x${Integer.toHexString(matchedAddr).uppercase()} for $actionKey!", Toast.LENGTH_LONG).show()
        }

        dialog.show()
        handler.post(runnable)
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
