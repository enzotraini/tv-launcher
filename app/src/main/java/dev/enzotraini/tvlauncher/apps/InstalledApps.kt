package dev.enzotraini.tvlauncher.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import java.text.Collator
import java.util.Locale

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val component: ComponentName,
    val icon: Drawable,
)

class InstalledApps(private val context: Context) {
    private val packageManager: PackageManager
        get() = context.packageManager

    fun load(): List<LaunchableApp> {
        val found = LinkedHashMap<String, LaunchableApp>()
        collect(found, Intent.CATEGORY_LEANBACK_LAUNCHER)
        collect(found, Intent.CATEGORY_LAUNCHER)
        val collator = Collator.getInstance(Locale("pt", "BR")).apply {
            strength = Collator.PRIMARY
        }
        return found.values.sortedWith { left, right -> collator.compare(left.label, right.label) }
    }

    fun open(component: ComponentName): Boolean = openPackage(component.packageName)

    fun openPackage(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        val leanback = packageManager.getLeanbackLaunchIntentForPackage(packageName)
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        return firstWorkingLaunch(
            leanback = leanback?.let { { start(it.addFlags(flags)) } },
            launcher = launch?.let { { start(it.addFlags(flags)) } },
        )
    }

    fun isInstalled(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun collect(into: LinkedHashMap<String, LaunchableApp>, category: String) {
        val query = Intent(Intent.ACTION_MAIN).addCategory(category)
        val resolved = try {
            packageManager.queryIntentActivities(query, 0)
        } catch (_: RuntimeException) {
            emptyList()
        }
        for (info in resolved) {
            val activity = info.activityInfo ?: continue
            val packageName = activity.packageName ?: continue
            if (into.containsKey(packageName)) continue
            val label = info.loadLabel(packageManager)?.toString().orEmpty().ifBlank { packageName }
            val icon = try {
                info.loadIcon(packageManager)
            } catch (_: RuntimeException) {
                null
            } ?: ColorDrawable(0xFFEFEBE4.toInt())
            into[packageName] = LaunchableApp(
                packageName = packageName,
                label = label,
                component = ComponentName(packageName, activity.name),
                icon = icon,
            )
        }
    }

    private fun start(intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: RuntimeException) {
            false
        }
    }
}

/** Leanback primeiro. Se essa abertura falhar, tenta o launcher comum. */
internal fun firstWorkingLaunch(leanback: (() -> Boolean)?, launcher: (() -> Boolean)?): Boolean {
    if (leanback?.invoke() == true) return true
    return launcher?.invoke() == true
}
