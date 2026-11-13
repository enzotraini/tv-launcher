package dev.enzotraini.tvlauncher

import android.content.Context
import dev.enzotraini.tvlauncher.model.DockEntry

data class LauncherConfig(
    val dockEntries: List<DockEntry>,
    val hidden: Set<String>,
    val liveTvPackage: String,
    val inputPickerPackage: String,
    val columns: Int,
    val weatherCity: String,
    val weatherLatitude: Double,
    val weatherLongitude: Double,
    val idleMinutes: Int,
    val heroSeconds: Int,
) {
    companion object {
        fun from(context: Context): LauncherConfig {
            val resources = context.resources
            val packages = resources.getStringArray(R.array.dock_apps)
            val labels = resources.getStringArray(R.array.dock_labels)
            val entries = packages.mapIndexed { index, packageName ->
                val label = labels.getOrElse(index) { packageName.substringAfterLast('.') }
                DockEntry(packageName.trim(), label.trim())
            }.filter { it.packageName.isNotEmpty() }
            val hidden = resources.getStringArray(R.array.hidden_apps)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
            return LauncherConfig(
                dockEntries = entries,
                hidden = hidden + context.packageName,
                liveTvPackage = resources.getString(R.string.live_tv_package).trim(),
                inputPickerPackage = resources.getString(R.string.input_picker_package).trim(),
                columns = resources.getInteger(R.integer.app_columns).coerceAtLeast(1),
                weatherCity = resources.getString(R.string.weather_city).trim().ifBlank { "São Paulo" },
                weatherLatitude = resources.getString(R.string.weather_latitude).toDoubleOrNull() ?: -23.5505,
                weatherLongitude = resources.getString(R.string.weather_longitude).toDoubleOrNull() ?: -46.6333,
                idleMinutes = resources.getInteger(R.integer.idle_minutes).coerceIn(1, 180),
                heroSeconds = resources.getInteger(R.integer.hero_seconds).coerceIn(4, 30),
            )
        }
    }
}
