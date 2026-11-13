package dev.enzotraini.tvlauncher.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import dev.enzotraini.tvlauncher.model.SystemShortcut

fun Activity.tryStart(intent: Intent): Boolean {
    return try {
        val resolved = intent.resolveActivity(packageManager) != null
        if (!resolved) return false
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: RuntimeException) {
        false
    }
}

fun Activity.openShortcut(shortcut: SystemShortcut): Boolean {
    for (action in shortcut.actions) {
        if (tryStart(Intent(action))) return true
    }
    for ((packageName, className) in shortcut.components) {
        val intent = Intent().setComponent(ComponentName(packageName, className))
        if (tryStart(intent)) return true
    }
    return false
}

fun Activity.viewResolves(uri: String, packageName: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(packageName)
    return try {
        intent.resolveActivity(packageManager) != null
    } catch (_: RuntimeException) {
        false
    }
}
