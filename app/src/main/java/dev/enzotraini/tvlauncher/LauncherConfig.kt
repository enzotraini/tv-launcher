package dev.enzotraini.tvlauncher

import android.content.Context
import dev.enzotraini.tvlauncher.model.DockEntry

data class LauncherConfig(
    val dockEntries: List<DockEntry>,
    val hidden: Set<String>,
    val liveTvPackage: String,
    val inputPickerPackage: String,
    val columns: Int,
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
            )
        }
    }
}
