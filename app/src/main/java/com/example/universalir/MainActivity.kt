package com.example.universalir

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
            val newAppliance = Appliance(
                id = UUID.randomUUID().toString(),
                name = customName,
                type = selectedType,
                commandMap = mapOf("power_toggle" to matchedCandidate.pattern)
            )

            savedDevicesList.add(newAppliance)
            deviceStorage.saveDevices(savedDevicesList)
            refreshDeviceList()

            Toast.makeText(this, "Successfully saved $customName!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}
