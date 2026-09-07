package com.example.universalir.model

enum class ApplianceType {
    TV,            // Added for Televisions
    AC,            // Added for Air Conditioners
    LAMP,          // Added for Lamps
    FAN,           // Added for Fans
    SOUNDBAR,      // Added for Home audio systems
    MEDIA_PLAYER,  // Added for Streaming boxes / cable boxes
    PROJECTOR      // Added for Home theater setups
}

data class Appliance(
    val id: String,
    val name: String,
    val type: ApplianceType,
    val commandMap: Map<String, IntArray>
)