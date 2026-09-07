package com.example.universalir.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DeviceStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ir_devices_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveDevices(devices: List<Appliance>) {
        val json = gson.toJson(devices)
        prefs.edit().putString("saved_appliances", json).apply()
    }

    fun loadDevices(): MutableList<Appliance> {
        val json = prefs.getString("saved_appliances", null) ?: return mutableCopyOf()
        val type = object : TypeToken<MutableList<Appliance>>() {}.type
        return gson.fromJson(json, type) ?: mutableCopyOf()
    }

    private fun mutableCopyOf(): MutableList<Appliance> = mutableListOf()
}