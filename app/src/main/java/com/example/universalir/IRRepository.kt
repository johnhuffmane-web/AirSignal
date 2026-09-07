package com.example.universalir.hardware

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log

class IrRepository(context: Context) {

    // Retrieve the system's Consumer IR Service
    private val irManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    /**
     * Checks if the device actually has a physical IR emitter.
     */
    val hasEmitter: Boolean
        get() = irManager?.hasIrEmitter() == true

    /**
     * Transmits a raw IR signal pattern.
     * @param frequency Carrier frequency in Hertz (typically 38000 for home appliances)
     * @param pattern Alternating microsecond array [on, off, on, off...]
     */
    fun transmit(frequency: Int, pattern: IntArray) {
        if (!hasEmitter) {
            Log.e("IrRepository", "Error: This device does not have an IR blaster hardware module.")
            return
        }

        try {
            irManager?.transmit(frequency, pattern)
            Log.d("IrRepository", "IR Signal successfully transmitted at ${frequency}Hz")
        } catch (e: Exception) {
            Log.e("IrRepository", "Failed to transmit IR signal", e)
        }
    }
}