package dev.enzotraini.tvlauncher.model

enum class SlotKind { HDMI, ANTENNA }

enum class ConnectionState { CONNECTED, STANDBY, DISCONNECTED }

data class InputSlot(
    val id: String,
    val fallbackName: String,
    val kind: SlotKind,
)

data class ReportedPort(
    val inputId: String,
    val label: String,
    val state: ConnectionState?,
)

data class BoundSlot(
    val slot: InputSlot,
    val name: String,
    val port: ReportedPort?,
)

data class SystemShortcut(
    val id: String,
    val actions: List<String>,
    val components: List<Pair<String, String>>,
)

fun defaultSlots(): List<InputSlot> = listOf(
    InputSlot("hdmi-1", "HDMI 1", SlotKind.HDMI),
    InputSlot("hdmi-2", "HDMI 2", SlotKind.HDMI),
    InputSlot("hdmi-3", "HDMI 3", SlotKind.HDMI),
    InputSlot("hdmi-4", "HDMI 4", SlotKind.HDMI),
    InputSlot("antenna", "TV ao vivo", SlotKind.ANTENNA),
)

fun hdmiNumber(label: String, inputId: String): Int? {
    val source = "$label $inputId"
    val match = Regex("(?i)hdmi[^0-9]{0,3}([1-4])\\b").find(source)
        ?: Regex("(?i)(?:^|[^a-z0-9])hw([1-4])\\b").find(source)
    return match?.groupValues?.get(1)?.toIntOrNull()
}

/** 0, 1 e 2 são os estados de [android.media.tv.TvInputManager]. Outro valor não vira status. */
fun connectionFromTvInputState(state: Int): ConnectionState? = when (state) {
    0 -> ConnectionState.CONNECTED
    1 -> ConnectionState.STANDBY
    2 -> ConnectionState.DISCONNECTED
    else -> null
}

fun connectionLabel(state: ConnectionState?): String? = when (state) {
    ConnectionState.CONNECTED -> "conectado"
    ConnectionState.STANDBY -> "em espera"
    ConnectionState.DISCONNECTED -> "sem sinal"
    null -> null
}

fun bindSlots(reported: List<ReportedPort>, names: Map<String, String>): List<BoundSlot> {
    val used = HashSet<String>()
    return defaultSlots().map { slot ->
        val port = if (slot.kind == SlotKind.HDMI) {
            val number = slot.id.removePrefix("hdmi-").toInt()
            reported.firstOrNull { port ->
                port.inputId !in used && hdmiNumber(port.label, port.inputId) == number
            }?.also { used += it.inputId }
        } else {
            null
        }
        val custom = names[slot.id]?.trim().orEmpty()
        BoundSlot(slot, custom.ifEmpty { slot.fallbackName }, port)
    }
}

fun inputSubtitle(bound: BoundSlot): String? {
    if (bound.slot.kind == SlotKind.ANTENNA) return "Antena"
    val system = bound.port?.label?.trim().orEmpty()
    if (system.isNotEmpty() && !system.equals(bound.name, ignoreCase = true)) return system
    if (bound.slot.fallbackName != bound.name) return bound.slot.fallbackName
    return null
}

fun systemShortcuts(): List<SystemShortcut> = listOf(
    SystemShortcut(
        id = "picture",
        actions = listOf("android.settings.DISPLAY_SETTINGS"),
        components = listOf(
            "com.android.tv.settings" to "com.android.tv.settings.device.displaysound.DisplaySoundActivity",
        ),
    ),
    SystemShortcut(
        id = "sound",
        actions = listOf("android.settings.SOUND_SETTINGS"),
        components = listOf(
            "com.android.tv.settings" to "com.android.tv.settings.device.sound.SoundActivity",
        ),
    ),
    SystemShortcut(
        id = "wifi",
        actions = listOf("android.settings.WIFI_SETTINGS"),
        components = emptyList(),
    ),
    SystemShortcut(
        id = "bluetooth",
        actions = listOf("android.settings.BLUETOOTH_SETTINGS"),
        components = emptyList(),
    ),
    SystemShortcut(
        id = "settings",
        actions = listOf("android.settings.SETTINGS"),
        components = listOf(
            "com.android.tv.settings" to "com.android.tv.settings.MainSettings",
        ),
    ),
)
