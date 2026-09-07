package com.example.universalir

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.universalir.hardware.IrCandidate
import com.example.universalir.hardware.IrCodeDatabase
import com.example.universalir.hardware.IrRepository
import com.example.universalir.model.Appliance
import com.example.universalir.model.ApplianceType
import com.example.universalir.model.DeviceStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var irRepository: IrRepository
    private lateinit var deviceStorage: DeviceStorage
    private lateinit var savedDevicesList: MutableList<Appliance>
    
    private lateinit var deviceListView: ListView
    private lateinit var statusTextView: TextView

    private var scanHandler: Handler? = null
    private var scanRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        irRepository = IrRepository(this)
        deviceStorage = DeviceStorage(this)
        savedDevicesList = deviceStorage.loadDevices()

        statusTextView = findViewById(R.id.statusTextView)
        deviceListView = findViewById(R.id.deviceListView)
        val scanButton = findViewById<Button>(R.id.scanButton)
        val addHexButton = findViewById<Button>(R.id.addHexButton)
        val vaultBackupButton = findViewById<Button>(R.id.vaultBackupButton)

        if (irRepository.hasEmitter) {
            statusTextView.text = "IR Hardware Ready. Saved Devices: ${savedDevicesList.size}"
        } else {
            statusTextView.text = "No IR Blaster hardware found."
            scanButton.isEnabled = false
        }

        refreshDeviceList()

        // Trigger scan & match process
        scanButton.setOnClickListener {
            startScanAndMatchFlow()
        }

        // Add custom HEX code signal directly
        addHexButton.setOnClickListener {
            showAddCustomHexDialog()
        }

        // Export/Import JSON Remote Vault Backup
        vaultBackupButton.setOnClickListener {
            showVaultBackupDialog()
        }

        // Handle clicking a saved device to open its dynamic control screen
        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val selectedAppliance = savedDevicesList[position]
            val intent = Intent(this, ControlActivity::class.java).apply {
                putExtra("appliance_extra", Gson().toJson(selectedAppliance))
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh list when returning from controls
        savedDevicesList = deviceStorage.loadDevices()
        refreshDeviceList()
    }

    override fun onDestroy() {
        stopScanningLoop()
        super.onDestroy()
    }

    private fun refreshDeviceList() {
        val displayNames = savedDevicesList.map { "${it.name} (${it.type})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayNames)
        deviceListView.adapter = adapter
        if (irRepository.hasEmitter) {
            statusTextView.text = "IR Hardware Ready. Saved Devices: ${savedDevicesList.size}"
        }
    }

    /**
     * Step 1: Prompt the user to select a target device type to narrow down candidate scan codes.
     */
    private fun startScanAndMatchFlow() {
        val typesList = mutableListOf("All Device Types")
        typesList.addAll(ApplianceType.values().map { it.name })

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Device Type to Scan")
        builder.setItems(typesList.toTypedArray()) { dialog, which ->
            dialog.dismiss()
            val selectedType = if (which == 0) null else ApplianceType.values()[which - 1]
            startScanningForType(selectedType)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    /**
     * Step 2: Show scanning progress dialog with a live status/progress bar and stop button.
     */
    private fun startScanningForType(selectedType: ApplianceType?) {
        val candidates = IrCodeDatabase.getCandidatesForType(selectedType)
        if (candidates.isEmpty()) {
            Toast.makeText(this, "No test codes found for this category.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_scanning_progress, null)
        val typeTitleText = dialogView.findViewById<TextView>(R.id.scanDeviceTypeTextView)
        val progressText = dialogView.findViewById<TextView>(R.id.scanProgressText)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.scanProgressBar)
        val instructionText = dialogView.findViewById<TextView>(R.id.scanInstructionText)
        val stopScanBtn = dialogView.findViewById<Button>(R.id.stopScanButton)
        val deviceReactedBtn = dialogView.findViewById<Button>(R.id.deviceReactedButton)

        typeTitleText.text = "Scanning for ${selectedType?.name ?: "All Devices"}..."
        progressBar.max = candidates.size
        progressBar.progress = 0

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        var currentIndex = 0
        var isScanning = true

        scanHandler = Handler(Looper.getMainLooper())
        scanRunnable = object : Runnable {
            override fun run() {
                if (!isScanning || isFinishing || isDestroyed) return

                if (currentIndex < candidates.size) {
                    val candidate = candidates[currentIndex]
                    
                    // Transmit current test code
                    irRepository.transmit(candidate.frequency, candidate.pattern)

                    // Update Progress UI
                    progressBar.progress = currentIndex + 1
                    progressText.text = "Testing Code ${currentIndex + 1} of ${candidates.size}\n(${candidate.name} @ ${candidate.frequency} Hz)"
                    instructionText.text = "Watch your device! Did it turn on, off, or react?"

                    currentIndex++

                    // Schedule next test code in 2.5 seconds
                    scanHandler?.postDelayed(this, 2500)
                } else {
                    instructionText.text = "Scan complete. Didn't react? Try selecting another category or 'All Device Types'."
                    progressText.text = "Completed ${candidates.size} of ${candidates.size} test codes."
                }
            }
        }

        stopScanBtn.setOnClickListener {
            isScanning = false
            stopScanningLoop()
            dialog.dismiss()
            Toast.makeText(this, "Scanning stopped.", Toast.LENGTH_SHORT).show()
        }

        deviceReactedBtn.setOnClickListener {
            isScanning = false
            stopScanningLoop()
            dialog.dismiss()

            val matchedCandidate = if (currentIndex > 0) candidates[currentIndex - 1] else candidates[0]
            showSaveDialog(matchedCandidate)
        }

        dialog.show()

        // Start scanning loop immediately
        scanHandler?.post(scanRunnable!!)
    }

    private fun stopScanningLoop() {
        scanRunnable?.let { scanHandler?.removeCallbacks(it) }
        scanHandler = null
        scanRunnable = null
    }

    /**
     * Manual HEX Code / Signal Generator Dialog
     */
    private fun showAddCustomHexDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val nameInput = EditText(this).apply { hint = "Appliance Name (e.g. Living Room Lamp)" }
        val hexInput = EditText(this).apply { hint = "HEX Code (e.g. 0x00FF02FD or 0x00EF1CE3)" }
        val spinner = Spinner(this)
        val types = ApplianceType.values().map { it.name }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        layout.addView(TextView(this).apply { text = "Add Device by Custom HEX Code:" })
        layout.addView(nameInput)
        layout.addView(hexInput)
        layout.addView(TextView(this).apply { text = "Select Category:" })
        layout.addView(spinner)

        AlertDialog.Builder(this)
            .setTitle("Add Custom HEX Signal")
            .setView(layout)
            .setPositiveButton("Save Signal") { dialog, _ ->
                val name = nameInput.text.toString().trim()
                val hexStr = hexInput.text.toString().trim().replace("0x", "").replace("0X", "")
                if (name.isEmpty() || hexStr.isEmpty()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                try {
                    val hexVal = hexStr.toLong(16)
                    val rawPattern = IrCodeDatabase.necToRawPattern(hexVal)
                    val selectedType = ApplianceType.valueOf(spinner.selectedItem.toString())
                    val commandMap = createCommandMapForType(selectedType, rawPattern)

                    val appliance = Appliance(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = selectedType,
                        commandMap = commandMap
                    )

                    savedDevicesList.add(appliance)
                    deviceStorage.saveDevices(savedDevicesList)
                    refreshDeviceList()
                    Toast.makeText(this, "Successfully saved custom signal $name!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid HEX code format", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Backup & Restore Vault Dialog (Export/Import JSON)
     */
    private fun showVaultBackupDialog() {
        val options = arrayOf("Export Remotes Vault to Clipboard", "Import Remotes Vault from Clipboard")
        AlertDialog.Builder(this)
            .setTitle("Remote Backup Vault (JSON)")
            .setItems(options) { dialog, which ->
                dialog.dismiss()
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                if (which == 0) {
                    // Export
                    val json = Gson().toJson(savedDevicesList)
                    val clip = ClipData.newPlainText("AirSignal_Vault_Backup", json)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "Copied ${savedDevicesList.size} remotes to clipboard!", Toast.LENGTH_LONG).show()
                } else {
                    // Import
                    val clipData = clipboard.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val json = clipData.getItemAt(0).text.toString()
                        try {
                            val type = object : TypeToken<MutableList<Appliance>>() {}.type
                            val imported: MutableList<Appliance> = Gson().fromJson(json, type)
                            if (imported.isNotEmpty()) {
                                savedDevicesList.clear()
                                savedDevicesList.addAll(imported)
                                deviceStorage.saveDevices(savedDevicesList)
                                refreshDeviceList()
                                Toast.makeText(this, "Successfully restored ${imported.size} remotes!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Clipboard data empty or invalid", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "Failed to parse backup JSON", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    /**
     * Step 3: Prompt user to save the matched device with a custom name.
     */
    private fun showSaveDialog(matchedCandidate: IrCandidate) {
        val dialogView = layoutInflater.inflate(R.layout.dailog_save_device, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.deviceNameInput)
        val spinner = dialogView.findViewById<Spinner>(R.id.applianceTypeSpinner)

        // Pre-fill suggested name
        val defaultName = "${matchedCandidate.type.name.lowercase().replaceFirstChar { it.uppercase() }} (${matchedCandidate.name})"
        nameInput.setText(defaultName)

        val types = ApplianceType.values().map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spinner.adapter = adapter
        
        // Select matched candidate's type in the spinner
        val selectedIndex = ApplianceType.values().indexOf(matchedCandidate.type)
        if (selectedIndex >= 0) {
            spinner.setSelection(selectedIndex)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.saveDeviceButton).setOnClickListener {
            val customName = nameInput.text.toString().trim()
            if (customName.isEmpty()) {
                Toast.makeText(this, "Please enter a device name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedType = ApplianceType.valueOf(spinner.selectedItem.toString())
            val commandMap = createCommandMapForType(selectedType, matchedCandidate.pattern)

            val newAppliance = Appliance(
                id = UUID.randomUUID().toString(),
                name = customName,
                type = selectedType,
                commandMap = commandMap
            )

            savedDevicesList.add(newAppliance)
            deviceStorage.saveDevices(savedDevicesList)
            refreshDeviceList()

            Toast.makeText(this, "Successfully saved $customName!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createCommandMapForType(type: ApplianceType, matchedPattern: IntArray): Map<String, IntArray> {
        val map = mutableMapOf<String, IntArray>()
        map["power_toggle"] = matchedPattern

        when (type) {
            ApplianceType.LAMP -> {
                map["color_temp_cycle"] = IrCodeDatabase.necToRawPattern(0x00FFE01FL)
                map["brightness_up"] = IrCodeDatabase.necToRawPattern(0x00FF629DL)
                map["brightness_down"] = IrCodeDatabase.necToRawPattern(0x00FFA25DL)
                map["warm_white"] = IrCodeDatabase.necToRawPattern(0x00FF22DDL)
                map["cool_white"] = IrCodeDatabase.necToRawPattern(0x00FFC23DL)
                map["night_light"] = IrCodeDatabase.necToRawPattern(0x00FFA857L)
            }
            ApplianceType.AC -> {
                map["temp_up"] = IrCodeDatabase.necToRawPattern(0x00FF609FL)
                map["temp_down"] = IrCodeDatabase.necToRawPattern(0x00FFE01FL)
            }
            ApplianceType.TV, ApplianceType.SOUNDBAR, ApplianceType.MEDIA_PLAYER -> {
                map["vol_up"] = IrCodeDatabase.necToRawPattern(0x00FFB04FL)
                map["vol_down"] = IrCodeDatabase.necToRawPattern(0x00FFF00FL)
                map["ch_up"] = IrCodeDatabase.necToRawPattern(0x00FF08F7L)
                map["ch_down"] = IrCodeDatabase.necToRawPattern(0x00FF8877L)
            }
            else -> {}
        }
        return map
    }
}
