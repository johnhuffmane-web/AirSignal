package com.example.universalir.hardware

import com.example.universalir.model.ApplianceType

data class IrCandidate(
    val name: String,
    val type: ApplianceType,
    val frequency: Int,
    val pattern: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as IrCandidate
        return name == other.name && type == other.type && frequency == other.frequency && pattern.contentEquals(other.pattern)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + frequency.hashCode()
        result = 31 * result + pattern.contentHashCode()
        return result
    }
}

object IrCodeDatabase {

    /**
     * Converts a 32-bit NEC protocol hex code into raw microsecond mark/space array (MSB-first).
     */
    fun necToRawPattern(hexCode: Long): IntArray {
        val list = mutableListOf<Int>()
        // Leader mark and space
        list.add(9000)
        list.add(4500)
        // 32 bits (MSB first)
        for (i in 31 downTo 0) {
            val bit = (hexCode shr i) and 1L
            list.add(560)
            if (bit == 1L) {
                list.add(1690)
            } else {
                list.add(560)
            }
        }
        // Final stop mark
        list.add(560)
        return list.toIntArray()
    }

    /**
     * Official LSB-first NEC Protocol raw pattern generator.
     * Transmits Address, Address Inverse, Command, and Command Inverse (8 bits each, LSB-first).
     */
    fun necLsbToRawPattern(address: Int, command: Int): IntArray {
        val list = mutableListOf<Int>()
        list.add(9000)
        list.add(4500)

        fun addByteLsb(b: Int) {
            for (bitIdx in 0..7) {
                val bit = (b shr bitIdx) and 1
                list.add(560)
                if (bit == 1) {
                    list.add(1690)
                } else {
                    list.add(560)
                }
            }
        }

        val addrLow = address and 0xFF
        val addrHigh = (address shr 8) and 0xFF
        val cmd = command and 0xFF

        if ((address shr 8) == 0) {
            addByteLsb(addrLow)
            addByteLsb(addrLow xor 0xFF)
        } else {
            addByteLsb(addrLow)
            addByteLsb(addrHigh)
        }

        addByteLsb(cmd)
        addByteLsb(cmd xor 0xFF)

        list.add(560)
        return list.toIntArray()
    }

    /**
     * Generates a complete command map for CCT 3-Color Dimmable Lamps based on address header & variant.
     */
    fun getLampProtocolMapForVariant(addressHeader: Long, variantIndex: Int): Map<String, IntArray> {
        val map = mutableMapOf<String, IntArray>()

        when (variantIndex) {
            1 -> { // Variant B: Lazada CCT Lamp (BAIERDI Verified)
                map["power_toggle"] = necLsbToRawPattern(0x00, 0x12)
                map["color_temp_cycle"] = necLsbToRawPattern(0x00, 0x17)
                map["brightness_up"] = necLsbToRawPattern(0x00, 0x18) // 100% MAX BRIGHTNESS (VERIFIED)
                map["brightness_down"] = necLsbToRawPattern(0x00, 0x1C) // 50% MEDIUM BRIGHTNESS (VERIFIED)
                map["warm_white"] = necLsbToRawPattern(0x00, 0x11)
                map["cool_white"] = necLsbToRawPattern(0x00, 0x13)
                map["night_light"] = necLsbToRawPattern(0x00, 0x15) // 5% LOW BRIGHTNESS (VERIFIED)
            }
            2 -> { // Variant C: CCT 21-Key Controller
                map["power_toggle"] = necLsbToRawPattern(0x00, 0xB2)
                map["color_temp_cycle"] = necLsbToRawPattern(0x00, 0x30)
                map["brightness_up"] = necLsbToRawPattern(0x00, 0x18)
                map["brightness_down"] = necLsbToRawPattern(0x00, 0x1C)
                map["warm_white"] = necLsbToRawPattern(0x00, 0x50)
                map["cool_white"] = necLsbToRawPattern(0x00, 0xD0)
                map["night_light"] = necLsbToRawPattern(0x00, 0x15)
            }
            3 -> { // Variant D: CCT Driver Pro
                map["power_toggle"] = necLsbToRawPattern(0x00, 0x0A)
                map["color_temp_cycle"] = necLsbToRawPattern(0x00, 0x0C)
                map["brightness_up"] = necLsbToRawPattern(0x00, 0x18)
                map["brightness_down"] = necLsbToRawPattern(0x00, 0x1C)
                map["warm_white"] = necLsbToRawPattern(0x00, 0x48)
                map["cool_white"] = necLsbToRawPattern(0x00, 0x68)
                map["night_light"] = necLsbToRawPattern(0x00, 0x15)
            }
            else -> { // Variant A: Standard 24-Key CCT
                map["power_toggle"] = necLsbToRawPattern(0x00, 0x02)
                map["color_temp_cycle"] = necLsbToRawPattern(0x00, 0xB0)
                map["brightness_up"] = necLsbToRawPattern(0x00, 0x18)
                map["brightness_down"] = necLsbToRawPattern(0x00, 0x1C)
                map["warm_white"] = necLsbToRawPattern(0x00, 0x22)
                map["cool_white"] = necLsbToRawPattern(0x00, 0xC2)
                map["night_light"] = necLsbToRawPattern(0x00, 0x15)
            }
        }
        return map
    }

    val allCandidates: List<IrCandidate> = listOf(
        // Lamp / Lighting (Includes common Lazada/Shopee Nordic Standing & Corner Floor Lamp IR remotes)
        IrCandidate("Nordic Floor Lamp 24-Key Power ON (0x00FF02FD)", ApplianceType.LAMP, 38000, necToRawPattern(0x00FF02FDL)),
        IrCandidate("Nordic Floor Lamp 24-Key Power OFF (0x00FF00FF)", ApplianceType.LAMP, 38000, necToRawPattern(0x00FF00FFL)),
        IrCandidate("Nordic Corner Lamp Power Toggle (0x00FFB04F)", ApplianceType.LAMP, 38000, necToRawPattern(0x00FFB04FL)),
        IrCandidate("Lazada RGB Standing Lamp Code A (0x00FF629D)", ApplianceType.LAMP, 38000, necToRawPattern(0x00FF629DL)),
        IrCandidate("Lazada RGB Standing Lamp Code B (0x00FFA25D)", ApplianceType.LAMP, 38000, necToRawPattern(0x00FFA25DL)),
        IrCandidate("Nordic CCT Dimmable Lamp (0x00FFE01F)", ApplianceType.LAMP, 38000, necToRawPattern(0x00FFE01FL)),
        IrCandidate("21-Key Mini Lamp Remote (0x00FFF00F)", ApplianceType.LAMP, 38000, necToRawPattern(0x00FFF00FL)),
        IrCandidate("RGB Lamp Code 1", ApplianceType.LAMP, 38000, intArrayOf(9000, 4500, 560, 560, 560, 1690, 560, 560, 560, 1690)),
        IrCandidate("Generic Lamp Code 2", ApplianceType.LAMP, 38000, intArrayOf(8900, 4450, 550, 1650, 550, 550, 550, 1650)),
        IrCandidate("Smart Bulb Code 3", ApplianceType.LAMP, 36000, intArrayOf(8800, 4400, 500, 1500, 500, 500, 500, 1500)),
        IrCandidate("Light Panel Code 4", ApplianceType.LAMP, 38000, intArrayOf(9000, 4500, 600, 1200, 600, 600, 600, 1200)),

        // TV
        IrCandidate("Samsung TV Power", ApplianceType.TV, 38000, intArrayOf(4500, 4500, 560, 1690, 560, 560, 560, 1690)),
        IrCandidate("LG TV Power", ApplianceType.TV, 38000, intArrayOf(9000, 4500, 560, 560, 560, 1690, 560, 1690)),
        IrCandidate("Sony SIRC TV Power", ApplianceType.TV, 40000, intArrayOf(2400, 600, 1200, 600, 600, 600, 1200, 600)),
        IrCandidate("Panasonic TV Power", ApplianceType.TV, 36700, intArrayOf(3500, 1750, 450, 1300, 450, 450, 450, 1300)),
        IrCandidate("TCL / Roku TV Power", ApplianceType.TV, 38000, intArrayOf(9000, 4500, 560, 1690, 560, 1690, 560, 560)),

        // Air Conditioner
        IrCandidate("Daikin AC Power", ApplianceType.AC, 38000, intArrayOf(3500, 1750, 450, 1350, 450, 450, 450, 1350)),
        IrCandidate("Panasonic AC Power", ApplianceType.AC, 36700, intArrayOf(3500, 1750, 450, 450, 450, 1350, 450, 1350)),
        IrCandidate("Midea / Carrier AC Power", ApplianceType.AC, 38000, intArrayOf(4400, 4400, 540, 1620, 540, 540, 540, 1620)),
        IrCandidate("LG AC Power", ApplianceType.AC, 38000, intArrayOf(3200, 9800, 500, 1500, 500, 500, 500, 1500)),

        // Fan
        IrCandidate("Dyson Fan Power", ApplianceType.FAN, 38000, intArrayOf(2200, 760, 500, 1200, 500, 500, 500, 1200)),
        IrCandidate("Generic Fan Power 1", ApplianceType.FAN, 38000, intArrayOf(9000, 4500, 560, 560, 560, 1690)),
        IrCandidate("Lasko Fan Power", ApplianceType.FAN, 38000, intArrayOf(8000, 4000, 500, 1500, 500, 500)),

        // Soundbar / Audio
        IrCandidate("Bose Soundbar Power", ApplianceType.SOUNDBAR, 38000, intArrayOf(9000, 4500, 560, 1690, 560, 560)),
        IrCandidate("Sonos / Yamaha Power", ApplianceType.SOUNDBAR, 38000, intArrayOf(4500, 4500, 560, 560, 560, 1690)),
        IrCandidate("Sony Audio Power", ApplianceType.SOUNDBAR, 40000, intArrayOf(2400, 600, 1200, 600, 600, 600)),

        // Media Player
        IrCandidate("Apple TV Power", ApplianceType.MEDIA_PLAYER, 38000, intArrayOf(8000, 4000, 500, 1500, 500, 500)),
        IrCandidate("Roku Media Box Power", ApplianceType.MEDIA_PLAYER, 38000, intArrayOf(9000, 4500, 560, 1690, 560, 1690)),

        // Projector
        IrCandidate("Epson Projector Power", ApplianceType.PROJECTOR, 38000, intArrayOf(9000, 4500, 560, 1690, 560, 560)),
        IrCandidate("BenQ Projector Power", ApplianceType.PROJECTOR, 38000, intArrayOf(8800, 4400, 550, 1650, 550, 550))
    )

    fun getCandidatesForType(type: ApplianceType?): List<IrCandidate> {
        if (type == null) return allCandidates
        val filtered = allCandidates.filter { it.type == type }
        return filtered.ifEmpty { allCandidates }
    }
}
