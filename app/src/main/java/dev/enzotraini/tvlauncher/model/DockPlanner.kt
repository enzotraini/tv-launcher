package dev.enzotraini.tvlauncher.model

data class DockEntry(
    val packageName: String,
    val label: String,
)

data class DockSlot(
    val packageName: String,
    val label: String,
    val installed: Boolean,
)

/** Mantém a ordem do config.xml. App ausente continua na lista, marcado como não instalado. */
fun planDock(entries: List<DockEntry>, installedPackages: Set<String>): List<DockSlot> {
    return entries.map { entry ->
        DockSlot(
            packageName = entry.packageName,
            label = entry.label,
            installed = entry.packageName in installedPackages,
        )
    }
}

/** Grade "Todos": o que está instalado, fora do dock e fora da lista oculta. */
fun gridPackages(
    installed: List<String>,
    dockPackages: Set<String>,
    hidden: Set<String>,
): List<String> {
    return installed.filter { it !in dockPackages && it !in hidden }
}
