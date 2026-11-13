package dev.enzotraini.tvlauncher

import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import dev.enzotraini.tvlauncher.apps.InstalledApps
import dev.enzotraini.tvlauncher.databinding.ActivityInputsBinding
import dev.enzotraini.tvlauncher.databinding.ItemInputBinding

class InputsActivity : TvActivity() {
    private lateinit var binding: ActivityInputsBinding
    private lateinit var config: LauncherConfig
    private lateinit var installedApps: InstalledApps

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInputsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        config = LauncherConfig.from(this)
        installedApps = InstalledApps(this)
        val first = bindRows()
        binding.inputList.post { first?.requestFocus() }
    }

    override fun onMenuKey() {
        finish()
    }

    private fun bindRows(): View? {
        binding.inputList.removeAllViews()
        var first: View? = null
        val inputs = passthroughInputs()
        if (inputs.isEmpty()) {
            binding.inputList.addView(note(getString(R.string.no_hdmi)))
        }
        for (input in inputs) {
            val row = addRow(input.label, getString(R.string.hint_hdmi)) {
                openPassthrough(input.id)
            }
            if (first == null) first = row
        }
        val live = addRow(getString(R.string.live_tv), getString(R.string.hint_app)) {
            openLiveTv()
        }
        if (first == null) first = live
        if (installedApps.isInstalled(config.inputPickerPackage)) {
            addRow(getString(R.string.all_sources), getString(R.string.hint_app)) {
                if (!installedApps.openPackage(config.inputPickerPackage)) {
                    toast(R.string.input_open_failed)
                }
            }
        }
        addRow(getString(R.string.settings), getString(R.string.hint_system)) {
            openSettings(Settings.ACTION_SETTINGS)
        }
        addRow(getString(R.string.wifi), getString(R.string.hint_system)) {
            openSettings(Settings.ACTION_WIFI_SETTINGS)
        }
        wire(binding.inputList)
        return first
    }

    private fun addRow(title: String, hint: String, action: () -> Unit): View {
        val row = ItemInputBinding.inflate(layoutInflater, binding.inputList, false)
        val root = row.inputRoot
        root.id = View.generateViewId()
        root.background = getDrawable(R.drawable.row_bg)?.mutate()
        row.inputTitle.text = title
        row.inputHint.text = hint
        root.contentDescription = title
        root.setOnClickListener { action() }
        binding.inputList.addView(root)
        return root
    }

    private fun note(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(getColor(R.color.muted))
            textSize = 16f
            setPadding(0, 0, 0, dp(12))
        }
    }

    private fun wire(list: android.widget.LinearLayout) {
        val rows = mutableListOf<View>()
        for (index in 0 until list.childCount) {
            val child = list.getChildAt(index)
            if (child.isFocusable) rows += child
        }
        rows.forEachIndexed { index, view ->
            view.nextFocusUpId = rows[if (index == 0) 0 else index - 1].id
            view.nextFocusDownId = rows[if (index == rows.lastIndex) index else index + 1].id
            view.nextFocusLeftId = view.id
            view.nextFocusRightId = view.id
        }
    }

    private fun passthroughInputs(): List<HdmiInput> {
        val manager = getSystemService(TV_INPUT_SERVICE) as? TvInputManager ?: return emptyList()
        return try {
            manager.tvInputList
                .filter { it.isPassthroughInput && !it.isHidden(this) }
                .map { info ->
                    val custom = info.loadCustomLabel(this)
                    val label = if (custom.isNullOrBlank()) info.loadLabel(this) else custom
                    HdmiInput(info.id, label?.toString().orEmpty().ifBlank { getString(R.string.hint_hdmi) })
                }
                .sortedBy { it.label.lowercase() }
        } catch (_: RuntimeException) {
            emptyList()
        }
    }

    private fun openPassthrough(inputId: String) {
        val uri = TvContract.buildChannelUriForPassthroughInput(inputId)
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!startSafely(intent) && !installedApps.openPackage(config.inputPickerPackage)) {
            toast(R.string.input_open_failed)
        }
    }

    private fun openLiveTv() {
        val candidates = listOf(
            config.liveTvPackage,
            "com.tcl.tv",
            "com.google.android.tv",
            "com.mediatek.wwtv.tvcenter",
        ).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val installed = candidates.firstOrNull { installedApps.isInstalled(it) }
        if (installed == null || !installedApps.openPackage(installed)) {
            toast(R.string.live_tv_missing)
        }
    }

    private fun openSettings(action: String) {
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!startSafely(intent)) toast(R.string.input_open_failed)
    }

    private fun startSafely(intent: Intent): Boolean {
        return try {
            startActivity(intent)
            true
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun toast(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private data class HdmiInput(val id: String, val label: String)
}
