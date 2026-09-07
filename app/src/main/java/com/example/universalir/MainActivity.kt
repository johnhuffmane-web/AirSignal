package com.example.universalir

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

    private fun refreshDeviceList() {
        val displayNames = savedDevicesList.map { "${it.name} (${it.type})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayNames)
        deviceListView.adapter = adapter
        if (irRepository.hasEmitter) {
            statusTextView.text = "IR Hardware Ready. Saved Devices: ${savedDevicesList.size}"
        }
    }

    private fun startScanAndMatchFlow() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Scanning for Device...")
        builder.setMessage("Transmitting test code sequences. Did your appliance just react or turn off?")

        builder.setPositiveButton("Found It! (Save Device)") { _, _ ->
            showSaveDialog(intArrayOf(9000, 4500, 560, 1690))
        }
        builder.setNegativeButton("Keep Scanning", null)
        
        irRepository.transmit(38000, intArrayOf(9000, 4500, 560, 560, 560, 1690))
        builder.show()
    }

    private fun showSaveDialog(workingPattern: IntArray) {
        val dialogView = layoutInflater.inflate(R.layout.dailog_save_device, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.deviceNameInput)
        val spinner = dialogView.findViewById<Spinner>(R.id.applianceTypeSpinner)

        val types = ApplianceType.values().map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spinner.adapter = adapter

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
                commandMap = mapOf("power_toggle" to workingPattern)
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