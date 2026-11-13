package dev.enzotraini.tvlauncher

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import dev.enzotraini.tvlauncher.apps.InstalledApps
import dev.enzotraini.tvlauncher.databinding.ActivityAppsBinding
import dev.enzotraini.tvlauncher.databinding.ItemAppBinding
import dev.enzotraini.tvlauncher.model.gridPackages
import dev.enzotraini.tvlauncher.ui.Dpad

class AppsActivity : TvActivity() {
    private lateinit var binding: ActivityAppsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val config = LauncherConfig.from(this)
        val loader = InstalledApps(this)
        val installed = loader.load()
        val visible = gridPackages(
            installed = installed.map { it.packageName },
            dockPackages = config.dockEntries.map { it.packageName }.toSet(),
            hidden = config.hidden,
        ).toSet()
        val apps = installed.filter { it.packageName in visible }
        binding.appsCount.text = resources.getQuantityString(R.plurals.apps_count, apps.size, apps.size)
        if (apps.isEmpty()) {
            binding.appsEmpty.visibility = View.VISIBLE
            return
        }
        val columns = config.columns
        val rows = mutableListOf<List<View>>()
        apps.chunked(columns).forEach { chunk ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
                clipToPadding = false
            }
            val focusables = mutableListOf<View>()
            for (app in chunk) {
                val cell = ItemAppBinding.inflate(layoutInflater, row, false)
                val root = cell.appRoot
                root.id = View.generateViewId()
                cell.appFrame.foreground = getDrawable(R.drawable.focus_ring)?.mutate()
                cell.appIcon.setImageDrawable(app.icon)
                cell.appLabel.text = app.label
                root.contentDescription = app.label
                root.setOnClickListener { loader.open(app.component) }
                root.setOnFocusChangeListener { _, hasFocus ->
                    cell.appLabel.isSelected = hasFocus
                }
                row.addView(
                    root,
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
                )
                focusables += root
            }
            repeat(columns - chunk.size) {
                row.addView(
                    View(this),
                    LinearLayout.LayoutParams(0, 1, 1f),
                )
            }
            binding.appsGrid.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            rows += focusables
        }
        Dpad.stack(rows)
        rows.firstOrNull()?.firstOrNull()?.let { first ->
            binding.appsGrid.post { first.requestFocus() }
        }
    }
}
