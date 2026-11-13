package dev.enzotraini.tvlauncher

import android.app.AlertDialog
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import dev.enzotraini.tvlauncher.apps.InstalledApps
import dev.enzotraini.tvlauncher.databinding.ActivityInputsBinding
import dev.enzotraini.tvlauncher.model.BoundSlot
import dev.enzotraini.tvlauncher.model.ConnectionState
import dev.enzotraini.tvlauncher.model.ReportedPort
import dev.enzotraini.tvlauncher.model.SlotKind
import dev.enzotraini.tvlauncher.model.bindSlots
import dev.enzotraini.tvlauncher.model.clockLabel
import dev.enzotraini.tvlauncher.model.connectionFromTvInputState
import dev.enzotraini.tvlauncher.model.connectionLabel
import dev.enzotraini.tvlauncher.model.inputSubtitle
import dev.enzotraini.tvlauncher.model.systemShortcuts
import dev.enzotraini.tvlauncher.store.DeviceStores
import dev.enzotraini.tvlauncher.ui.Dpad
import dev.enzotraini.tvlauncher.ui.InputCard
import dev.enzotraini.tvlauncher.ui.ShortcutTile
import dev.enzotraini.tvlauncher.ui.dp
import dev.enzotraini.tvlauncher.ui.openShortcut
import dev.enzotraini.tvlauncher.ui.tryStart
import dev.enzotraini.tvlauncher.ui.tvFocus
import java.time.LocalTime

class InputsActivity : TvActivity() {
    private lateinit var binding: ActivityInputsBinding
    private lateinit var config: LauncherConfig
    private lateinit var installedApps: InstalledApps
    private val handler = Handler(Looper.getMainLooper())
    private val cards = mutableListOf<View>()
    private val shortcuts = mutableListOf<View>()
    private var slots: List<BoundSlot> = emptyList()
    private var focusedSlot: BoundSlot? = null

    private val clockTick = object : Runnable {
        override fun run() {
            if (::binding.isInitialized) binding.inputsClock.text = clockLabel(LocalTime.now())
            handler.postDelayed(this, 15_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInputsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        config = LauncherConfig.from(this)
        installedApps = InstalledApps(this)
        bind()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(clockTick)
        clockTick.run()
    }

    override fun onPause() {
        handler.removeCallbacks(clockTick)
        super.onPause()
    }

    override fun onMenuKey() {
        finish()
    }

    private fun bind() {
        slots = bindSlots(readPorts(), readNames())
        binding.inputRow.removeAllViews()
        binding.shortcutRow.removeAllViews()
        cards.clear()
        shortcuts.clear()
        slots.forEachIndexed { index, slot ->
            val card = InputCard(this)
            val status = connectionLabel(slot.port?.state)
            card.bind(slot.name, inputSubtitle(slot), status)
            card.id = View.generateViewId()
            card.tvFocus(1.04f) { focused ->
                if (focused) focusedSlot = slot
            }
            card.setOnClickListener { openSlot(slot) }
            card.setOnLongClickListener {
                focusedSlot = slot
                rename(slot)
                true
            }
            val params = LinearLayout.LayoutParams(dp(156), LinearLayout.LayoutParams.MATCH_PARENT)
            params.marginEnd = dp(16)
            binding.inputRow.addView(card, params)
            cards += card
            if (index == 0) card.post { card.requestFocus() }
        }
        addShortcut(R.drawable.ic_picture, getString(R.string.picture)) {
            if (!openShortcut(systemShortcuts().first { it.id == "picture" })) toast(R.string.shortcut_missing)
        }
        addShortcut(R.drawable.ic_sound, getString(R.string.sound)) {
            if (!openShortcut(systemShortcuts().first { it.id == "sound" })) toast(R.string.shortcut_missing)
        }
        addShortcut(R.drawable.ic_wifi, getString(R.string.wifi)) {
            if (!openShortcut(systemShortcuts().first { it.id == "wifi" })) toast(R.string.shortcut_missing)
        }
        addShortcut(R.drawable.ic_bluetooth, getString(R.string.bluetooth)) {
            if (!openShortcut(systemShortcuts().first { it.id == "bluetooth" })) toast(R.string.shortcut_missing)
        }
        addShortcut(R.drawable.ic_settings, getString(R.string.settings)) {
            if (!openShortcut(systemShortcuts().first { it.id == "settings" })) toast(R.string.shortcut_missing)
        }
        addShortcut(R.drawable.ic_frames, getString(R.string.wallpapers)) {
            startActivity(Intent(this, WallpapersActivity::class.java))
        }
        addShortcut(R.drawable.ic_edit, getString(R.string.rename)) {
            val slot = focusedSlot
            if (slot == null) toast(R.string.rename_pick) else rename(slot)
        }
        val rows = buildList {
            if (cards.isNotEmpty()) add(cards.toList())
            if (shortcuts.isNotEmpty()) add(shortcuts.toList())
        }
        Dpad.stack(rows)
        focusedSlot = slots.firstOrNull()
    }

    private fun addShortcut(icon: Int, label: String, onClick: () -> Unit) {
        val tile = ShortcutTile(this)
        tile.bind(icon, label)
        tile.id = View.generateViewId()
        tile.tvFocus(1.06f)
        tile.setOnClickListener { onClick() }
        val params = LinearLayout.LayoutParams(dp(108), LinearLayout.LayoutParams.MATCH_PARENT)
        params.marginEnd = dp(12)
        binding.shortcutRow.addView(tile, params)
        shortcuts += tile
    }

    private fun openSlot(slot: BoundSlot) {
        if (slot.slot.kind == SlotKind.ANTENNA) {
            openTuner()
            return
        }
        val port = slot.port
        if (port == null) {
            toast(R.string.input_missing)
            return
        }
        val uri = TvContract.buildChannelUriForPassthroughInput(port.inputId)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (!tryStart(intent) && !installedApps.openPackage(config.inputPickerPackage)) {
            toast(R.string.input_open_failed)
        }
    }

    private fun openTuner() {
        val candidates = listOf(config.liveTvPackage, "com.tcl.tv", "com.google.android.tv")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val installed = candidates.firstOrNull { installedApps.isInstalled(it) }
        if (installed == null || !installedApps.openPackage(installed)) toast(R.string.live_tv_missing)
    }

    private fun rename(slot: BoundSlot) {
        val input = EditText(this).apply {
            setText(slot.name)
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.rename_title)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val typed = input.text?.toString()?.trim().orEmpty()
                val prefs = DeviceStores.inputNames(this)
                if (typed.isEmpty() || typed == slot.slot.fallbackName) {
                    prefs.edit().remove(slot.slot.id).apply()
                } else {
                    prefs.edit().putString(slot.slot.id, typed).apply()
                }
                bind()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun readNames(): Map<String, String> {
        return DeviceStores.inputNames(this).all.mapNotNull { (key, value) ->
            (value as? String)?.let { key to it }
        }.toMap()
    }

    private fun readPorts(): List<ReportedPort> {
        val manager = getSystemService(TV_INPUT_SERVICE) as? TvInputManager ?: return emptyList()
        return try {
            manager.tvInputList
                .filter { it.isPassthroughInput && !it.isHidden(this) }
                .map { info ->
                    val custom = info.loadCustomLabel(this)?.toString().orEmpty()
                    val label = custom.ifBlank { info.loadLabel(this)?.toString().orEmpty() }
                    val state = try {
                        connectionFromTvInputState(manager.getInputState(info.id))
                    } catch (_: RuntimeException) {
                        null
                    }
                    ReportedPort(info.id, label, state)
                }
        } catch (_: RuntimeException) {
            emptyList()
        }
    }

    private fun toast(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
