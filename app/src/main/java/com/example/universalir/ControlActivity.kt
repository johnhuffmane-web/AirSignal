package com.example.universalir

import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.universalir.hardware.IrCodeDatabase
import com.example.universalir.hardware.IrRepository
import com.example.universalir.model.Appliance
import com.example.universalir.model.DeviceStorage
import com.google.gson.Gson

class ControlActivity : AppCompatActivity() {

    private lateinit var irRepository: IrRepository
    private lateinit var appliance: Appliance
    private lateinit var containerLayout: LinearLayout

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
        containerLayout = findViewById(R.id.controlsContainerLayout)

        titleTextView.text = appliance.name

        refreshRemoteUI()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun refreshRemoteUI() {
        containerLayout.removeAllViews()

        // Top "Back to Dashboard" button
        val backBtn = Button(this).apply {
            text = "← Back to Main Dashboard"
            setOnClickListener { finish() }
        }
        containerLayout.addView(backBtn)

        // Title and Rename Device
        val deviceHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 12)
        }
        val deviceTitle = TextView(this).apply {
            text = "${appliance.name} (${appliance.type})"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val renameDeviceBtn = Button(this, null, android.R.attr.borderlessButtonStyle).apply {
            text = "✎ Rename Device"
            setOnClickListener { showRenameDeviceDialog() }
        }
        deviceHeader.addView(deviceTitle)
        deviceHeader.addView(renameDeviceBtn)
        containerLayout.addView(deviceHeader)

        // Action Toolbar: Auto-Scan & Add Custom Signal
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }
        val scanBtn = Button(this).apply {
            text = "⚡ Auto-Scan Bytes"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 4 }
            setOnClickListener { startFullByteAutoScan() }
        }
        val addCustomBtn = Button(this).apply {
            text = "+ Add Custom Code"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showAddCustomButtonDialog() }
        }
        toolbar.addView(scanBtn)
        toolbar.addView(addCustomBtn)
        containerLayout.addView(toolbar)

        // Standard 24-Key Power Test Section
        val standard24KeyLabel = TextView(this).apply {
            text = "Test Standard 24-Key CCT Power Codes (Address 0x00FF LSB):"
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 12, 0, 8)
        }
        containerLayout.addView(standard24KeyLabel)

        val powerRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val testPowerOff0x08 = Button(this).apply {
            text = "Test Power OFF (0x08)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f).apply { marginEnd = 4 }
            setOnClickListener {
                val raw = IrCodeDatabase.necLsbToRawPattern(0x00, 0x08)
                irRepository.transmit(38000, raw)
                Toast.makeText(this@ControlActivity, "Sent Power OFF (0x08)", Toast.LENGTH_SHORT).show()
            }
        }
        val savePowerOff0x08 = Button(this).apply {
            text = "Save Power OFF"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val updated = appliance.commandMap.toMutableMap()
                updated["Power OFF"] = IrCodeDatabase.necLsbToRawPattern(0x00, 0x08)
                appliance = appliance.copy(commandMap = updated)
                updateDeviceStorage(appliance)
                refreshRemoteUI()
                Toast.makeText(this@ControlActivity, "Saved 0x08 as Power OFF!", Toast.LENGTH_SHORT).show()
            }
        }
        powerRow.addView(testPowerOff0x08)
        powerRow.addView(savePowerOff0x08)
        containerLayout.addView(powerRow)

        val powerOnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val testPowerOn0x88 = Button(this).apply {
            text = "Test Power ON (0x88)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f).apply { marginEnd = 4 }
            setOnClickListener {
                val raw = IrCodeDatabase.necLsbToRawPattern(0x00, 0x88)
                irRepository.transmit(38000, raw)
                Toast.makeText(this@ControlActivity, "Sent Power ON (0x88)", Toast.LENGTH_SHORT).show()
            }
        }
        val savePowerOn0x88 = Button(this).apply {
            text = "Save Power ON"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val updated = appliance.commandMap.toMutableMap()
                updated["Power ON"] = IrCodeDatabase.necLsbToRawPattern(0x00, 0x88)
                appliance = appliance.copy(commandMap = updated)
                updateDeviceStorage(appliance)
                refreshRemoteUI()
                Toast.makeText(this@ControlActivity, "Saved 0x88 as Power ON!", Toast.LENGTH_SHORT).show()
            }
        }
        powerOnRow.addView(testPowerOn0x88)
        powerOnRow.addView(savePowerOn0x88)
        containerLayout.addView(powerOnRow)

        // Custom Universal Remote Canvas
        val canvasLabel = TextView(this).apply {
            text = "Your Configured Remote Buttons (${appliance.commandMap.size}):"
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 16, 0, 8)
        }
        containerLayout.addView(canvasLabel)

        if (appliance.commandMap.isEmpty()) {
            val emptyNotice = TextView(this).apply {
                text = "No buttons saved yet. Tap 'Auto-Scan Bytes' or '+ Add Custom Code' to start building your remote!"
                setPadding(0, 16, 0, 16)
            }
            containerLayout.addView(emptyNotice)
        } else {
            for ((key, pattern) in appliance.commandMap) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 4, 0, 4)
                }

                // Main Action Button
                val actionBtn = Button(this).apply {
                    text = key.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.8f).apply { marginEnd = 4 }
                    setOnClickListener {
                        val lowerKey = key.lowercase()
                        // Lock Sequence: If triggering Yellow Light (0x07), send double-lock sequence (0x09 -> 0x07 -> 0x07)
                        if (lowerKey.contains("0x07") || lowerKey.contains("yellow") || lowerKey.contains("warm")) {
                            sendMacroSequence(0x09, 0x07, 0x07)
                            Toast.makeText(this@ControlActivity, "Sent Locked Yellow Sequence", Toast.LENGTH_SHORT).show()
                        } else {
                            irRepository.transmit(38000, pattern)
                            Toast.makeText(this@ControlActivity, "Sent $key", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Rename Button
                val renameBtn = Button(this, null, android.R.attr.borderlessButtonStyle).apply {
                    text = "✎"
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 4 }
                    setOnClickListener { showRenameButtonDialog(key) }
                }

                // Delete Button
                val deleteBtn = Button(this, null, android.R.attr.borderlessButtonStyle).apply {
                    text = "🗑️"
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    setOnClickListener { showDeleteButtonDialog(key) }
                }

                row.addView(actionBtn)
                row.addView(renameBtn)
                row.addView(deleteBtn)
                containerLayout.addView(row)
            }
        }
    }

    private fun sendMacroSequence(vararg bytes: Int) {
        val handler = Handler(Looper.getMainLooper())
        for (i in bytes.indices) {
            handler.postDelayed({
                val raw = IrCodeDatabase.necLsbToRawPattern(0x00, bytes[i])
                irRepository.transmit(38000, raw)
            }, i * 200L)
        }
    }

    private fun showRenameDeviceDialog() {
        val input = EditText(this).apply { setText(appliance.name) }
        AlertDialog.Builder(this)
            .setTitle("Rename Appliance")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    appliance = appliance.copy(name = newName)
                    updateDeviceStorage(appliance)
                    refreshRemoteUI()
                    Toast.makeText(this, "Renamed device to $newName", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameButtonDialog(oldKey: String) {
        val input = EditText(this).apply { setText(oldKey) }
        AlertDialog.Builder(this)
            .setTitle("Rename Button")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newKey = input.text.toString().trim()
                if (newKey.isNotEmpty() && newKey != oldKey) {
                    val pattern = appliance.commandMap[oldKey]
                    if (pattern != null) {
                        val updated = appliance.commandMap.toMutableMap()
                        updated.remove(oldKey)
                        updated[newKey] = pattern
                        appliance = appliance.copy(commandMap = updated)
                        updateDeviceStorage(appliance)
                        refreshRemoteUI()
                        Toast.makeText(this, "Renamed button to $newKey!", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteButtonDialog(key: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Button")
            .setMessage("Are you sure you want to delete '$key'?")
            .setPositiveButton("Delete") { dialog, _ ->
                val updated = appliance.commandMap.toMutableMap()
                updated.remove(key)
                appliance = appliance.copy(commandMap = updated)
                updateDeviceStorage(appliance)
                refreshRemoteUI()
                Toast.makeText(this, "Deleted '$key'", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddCustomButtonDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }
        val nameInput = EditText(this).apply { hint = "Button Name (e.g. Yellow Light)" }
        val hexInput = EditText(this).apply { hint = "HEX Code (e.g. 0x07 or 0x09 or 0x0C)" }

        layout.addView(TextView(this).apply { text = "Add Custom Button Code:" })
        layout.addView(nameInput)
        layout.addView(hexInput)

        AlertDialog.Builder(this)
            .setTitle("Add Custom Button")
            .setView(layout)
            .setPositiveButton("Add Button") { dialog, _ ->
                val name = nameInput.text.toString().trim()
                val hexStr = hexInput.text.toString().trim().replace("0x", "").replace("0X", "")
                if (name.isNotEmpty() && hexStr.isNotEmpty()) {
                    try {
                        val byteVal = hexStr.toInt(16)
                        val pattern = IrCodeDatabase.necLsbToRawPattern(0x00, byteVal)
                        val updated = appliance.commandMap.toMutableMap()
                        updated[name] = pattern
                        appliance = appliance.copy(commandMap = updated)
                        updateDeviceStorage(appliance)
                        refreshRemoteUI()
                        Toast.makeText(this, "Added custom button '$name'!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Invalid HEX code", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Auto-scans command bytes (0x00 to 0x3F) with Pause/Resume and custom naming
     */
    private fun startFullByteAutoScan() {
        val byteList = listOf(
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            0x10, 0x11, 0x12, 0x13, 0x14, 0x16, 0x17, 0x19, 0x1A, 0x1B, 0x1D, 0x1E, 0x1F,
            0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x28, 0x2C, 0x30, 0x38, 0x3C, 0x40, 0x44, 0x48, 0x50, 0x60, 0x80, 0xA0, 0xB0, 0xC0, 0xD0, 0xE0, 0xF0
        )
        var idx = 0
        var isPaused = false
        var isScanning = true
        val handler = Handler(Looper.getMainLooper())

        val dialogView = layoutInflater.inflate(R.layout.dialog_scanning_progress, null)
        val titleText = dialogView.findViewById<TextView>(R.id.scanDeviceTypeTextView)
        val progressText = dialogView.findViewById<TextView>(R.id.scanProgressText)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.scanProgressBar)
        val instructionText = dialogView.findViewById<TextView>(R.id.scanInstructionText)
        val stopBtn = dialogView.findViewById<Button>(R.id.stopScanButton)
        val pauseBtn = dialogView.findViewById<Button>(R.id.pauseScanButton)
        val matchBtn = dialogView.findViewById<Button>(R.id.deviceReactedButton)

        titleText.text = "Auto-Scanning Command Bytes (Address 0x00FF)..."
        progressBar.max = byteList.size
        progressBar.progress = 1
        matchBtn.text = "Reacted! (Save)"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val runnable = object : Runnable {
            override fun run() {
                if (!isScanning || isPaused || isFinishing || isDestroyed) return
                if (idx < byteList.size) {
                    val cmdByte = byteList[idx]
                    val raw = IrCodeDatabase.necLsbToRawPattern(0x00, cmdByte)
                    irRepository.transmit(38000, raw)

                    progressBar.progress = idx + 1
                    val hexStr = "0x${Integer.toHexString(cmdByte).uppercase()}"
                    progressText.text = "Testing Byte ${idx + 1} of ${byteList.size} ($hexStr)"
                    instructionText.text = "Watch your lamp! Did it turn OFF/ON or change colors?"

                    idx++
                    handler.postDelayed(this, 2200)
                } else {
                    instructionText.text = "Finished testing all command bytes."
                }
            }
        }

        pauseBtn.setOnClickListener {
            if (isPaused) {
                isPaused = false
                pauseBtn.text = "Pause"
                val currentHex = if (idx > 0) "0x${Integer.toHexString(byteList[idx - 1]).uppercase()}" else "0x00"
                instructionText.text = "Resuming scan from $currentHex..."
                handler.post(runnable)
            } else {
                isPaused = true
                handler.removeCallbacksAndMessages(null)
                pauseBtn.text = "Resume"
                val currentHex = if (idx > 0) "0x${Integer.toHexString(byteList[idx - 1]).uppercase()}" else "0x00"
                instructionText.text = "⏸️ SCAN PAUSED at Byte $currentHex\n(Type your notes now. Tap Resume to continue!)"
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

            // Quick Name Entry Dialog
            val nameInput = EditText(this@ControlActivity).apply {
                setText("Signal $matchedHex")
            }
            AlertDialog.Builder(this@ControlActivity)
                .setTitle("Save Signal $matchedHex")
                .setMessage("Type any custom name for this remote button:")
                .setView(nameInput)
                .setPositiveButton("Save Button") { _, _ ->
                    val customBtnName = nameInput.text.toString().trim()
                    val validName = if (customBtnName.isNotEmpty()) customBtnName else "Signal $matchedHex"

                    val updated = appliance.commandMap.toMutableMap()
                    updated[validName] = IrCodeDatabase.necLsbToRawPattern(0x00, matchedByte)
                    appliance = appliance.copy(commandMap = updated)
                    updateDeviceStorage(appliance)
                    refreshRemoteUI()
                    Toast.makeText(this@ControlActivity, "Saved '$validName' into remote!", Toast.LENGTH_LONG).show()
                }
                .show()
        }

        dialog.show()
        handler.post(runnable)
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
}
