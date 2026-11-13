package dev.enzotraini.tvlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.Toast
import dev.enzotraini.tvlauncher.apps.InstalledApps
import dev.enzotraini.tvlauncher.apps.LaunchableApp
import dev.enzotraini.tvlauncher.databinding.ActivityHomeBinding
import dev.enzotraini.tvlauncher.databinding.ItemDockBinding
import dev.enzotraini.tvlauncher.databinding.ItemPosterBinding
import dev.enzotraini.tvlauncher.databinding.ShelfRowBinding
import dev.enzotraini.tvlauncher.model.Kind
import dev.enzotraini.tvlauncher.model.RecommendationCatalog
import dev.enzotraini.tvlauncher.model.Title
import dev.enzotraini.tvlauncher.model.monogram
import dev.enzotraini.tvlauncher.model.planDock
import dev.enzotraini.tvlauncher.ui.Dpad
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class HomeActivity : TvActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var config: LauncherConfig
    private lateinit var installedApps: InstalledApps
    private val shelfFocus = mutableListOf<List<View>>()
    private val dockFocus = mutableListOf<View>()
    private var appsSignature: String? = null
    private var lastFocusKey: String? = null
    private var timeRegistered = false
    private var packagesRegistered = false

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateClock()
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            appsSignature = null
            refreshDock()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        config = LauncherConfig.from(this)
        installedApps = InstalledApps(this)
        lastFocusKey = savedInstanceState?.getString(STATE_FOCUS)
        bindShelves()
        watchFocus(binding.entradasButton, KEY_ENTRADAS, 1.04f)
        binding.entradasButton.setOnClickListener { openInputs() }
        refreshDock()
        registerPackages()
    }

    override fun onResume() {
        super.onResume()
        registerTime()
        updateClock()
        refreshDock()
        binding.root.post {
            if (currentFocus == null) restoreFocus(lastFocusKey)
        }
    }

    override fun onPause() {
        if (timeRegistered) {
            unregisterReceiver(timeReceiver)
            timeRegistered = false
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (packagesRegistered) unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_FOCUS, lastFocusKey)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        binding.shelvesScroll.smoothScrollTo(0, 0)
        lastFocusKey = null
        restoreFocus(null)
    }

    @Deprecated("O Voltar não fecha a home.")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (binding.shelvesScroll.scrollY > 0) {
            binding.shelvesScroll.smoothScrollTo(0, 0)
        }
        lastFocusKey = null
        restoreFocus(null)
    }

    private fun bindShelves() {
        binding.shelves.removeAllViews()
        shelfFocus.clear()
        for (shelf in RecommendationCatalog.current().shelves()) {
            val row = ShelfRowBinding.inflate(layoutInflater, binding.shelves, true)
            row.shelfTitle.text = shelf.title
            val focusables = mutableListOf<View>()
            for (item in shelf.items) {
                val poster = bindPoster(item, row.shelfItems)
                row.shelfItems.addView(poster.root)
                focusables += poster.root
            }
            shelfFocus += focusables
        }
    }

    private fun bindPoster(title: Title, parent: android.view.ViewGroup): ItemPosterBinding {
        val poster = ItemPosterBinding.inflate(layoutInflater, parent, false)
        val root = poster.posterRoot
        root.id = View.generateViewId()
        poster.posterFrame.foreground = getDrawable(R.drawable.focus_ring)?.mutate()
        poster.posterArt.accent = title.accent
        poster.posterArt.monogram = monogram(title.name)
        poster.posterArt.kindLabel = getString(
            if (title.kind == Kind.MOVIE) R.string.kind_movie else R.string.kind_series,
        )
        poster.posterArt.progress = title.progress
        poster.posterTitle.text = title.name
        poster.posterMeta.text = meta(title)
        root.contentDescription = getString(R.string.poster_cd, title.name, poster.posterMeta.text)
        root.setOnClickListener {
            startActivity(
                Intent(this, DetailActivity::class.java)
                    .putExtra(DetailActivity.EXTRA_TITLE_ID, title.id),
            )
        }
        watchFocus(root, "title:${title.id}")
        return poster
    }

    private fun meta(title: Title): String {
        val progress = title.progress
        if (progress != null) {
            return getString(R.string.progress_watched, (progress * 100f).roundToInt())
        }
        val kind = getString(if (title.kind == Kind.MOVIE) R.string.kind_movie else R.string.kind_series)
        return getString(R.string.title_meta, kind, title.year)
    }

    private fun refreshDock() {
        val apps = installedApps.load()
        val signature = apps.joinToString(",") { it.packageName }
        if (signature == appsSignature && binding.dockRow.childCount > 0) return
        val key = lastFocusKey
        appsSignature = signature
        bindDock(apps)
        wireFocus()
        binding.root.post { restoreFocus(key) }
    }

    private fun bindDock(apps: List<LaunchableApp>) {
        val byPackage = apps.associateBy { it.packageName }
        val slots = planDock(config.dockEntries, byPackage.keys)
        binding.dockRow.removeAllViews()
        dockFocus.clear()
        for (slot in slots) {
            val item = ItemDockBinding.inflate(layoutInflater, binding.dockRow, false)
            val root = item.dockRoot
            root.id = View.generateViewId()
            item.dockFrame.foreground = getDrawable(R.drawable.focus_ring)?.mutate()
            item.dockLabel.text = slot.label
            val installed = byPackage[slot.packageName]
            if (installed != null && slot.installed) {
                item.dockIcon.setImageDrawable(installed.icon)
                item.dockIcon.visibility = View.VISIBLE
                item.dockMonogram.visibility = View.GONE
                root.contentDescription = slot.label
                root.setOnClickListener { installedApps.open(installed.component) }
            } else {
                item.dockIcon.visibility = View.GONE
                item.dockMonogram.visibility = View.VISIBLE
                item.dockMonogram.text = monogram(slot.label)
                root.contentDescription = getString(R.string.dock_missing_cd, slot.label)
                root.setOnClickListener {
                    Toast.makeText(this, getString(R.string.app_missing, slot.label), Toast.LENGTH_SHORT).show()
                }
            }
            watchFocus(root, "dock:${slot.packageName}", 1.08f) { focused ->
                item.dockLabel.isSelected = focused
                item.dockLabel.setTextColor(
                    getColor(if (focused || slot.installed) R.color.ink else R.color.muted),
                )
            }
            if (!slot.installed) {
                item.dockLabel.setTextColor(getColor(R.color.muted))
            }
            binding.dockRow.addView(root)
            dockFocus += root
        }

        val divider = View(this).apply {
            setBackgroundColor(getColor(R.color.line))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        binding.dockRow.addView(
            divider,
            LinearLayout.LayoutParams(dp(1), dp(36)).apply {
                marginStart = dp(10)
                marginEnd = dp(16)
                gravity = Gravity.CENTER_VERTICAL
            },
        )

        val all = ItemDockBinding.inflate(layoutInflater, binding.dockRow, false)
        val allRoot = all.dockRoot
        allRoot.id = View.generateViewId()
        all.dockFrame.foreground = getDrawable(R.drawable.focus_ring)?.mutate()
        all.dockFrame.setPadding(dp(14), dp(14), dp(14), dp(14))
        all.dockIcon.setImageResource(R.drawable.ic_apps)
        all.dockIcon.visibility = View.VISIBLE
        all.dockMonogram.visibility = View.GONE
        all.dockLabel.text = getString(R.string.todos)
        allRoot.contentDescription = getString(R.string.todos_cd)
        allRoot.setOnClickListener { startActivity(Intent(this, AppsActivity::class.java)) }
        watchFocus(allRoot, KEY_ALL_APPS, 1.08f) { focused ->
            all.dockLabel.isSelected = focused
        }
        binding.dockRow.addView(allRoot)
        dockFocus += allRoot
    }

    private fun wireFocus() {
        val rows = buildList {
            add(listOf(binding.entradasButton))
            addAll(shelfFocus)
            if (dockFocus.isNotEmpty()) add(dockFocus.toList())
        }
        Dpad.stack(rows)
    }

    private fun watchFocus(
        view: View,
        key: String,
        scale: Float = 1.06f,
        onFocus: (Boolean) -> Unit = {},
    ) {
        view.tag = key
        view.setOnFocusChangeListener { target, hasFocus ->
            target.animate().cancel()
            target.animate()
                .scaleX(if (hasFocus) scale else 1f)
                .scaleY(if (hasFocus) scale else 1f)
                .setDuration(if (hasFocus) 160 else 120)
                .setInterpolator(DecelerateInterpolator())
                .start()
            target.elevation = if (hasFocus) resources.getDimension(R.dimen.focus_elevation) else 0f
            if (hasFocus) {
                lastFocusKey = key
                target.requestRectangleOnScreen(Rect(0, 0, target.width, target.height), false)
            }
            onFocus(hasFocus)
        }
    }

    private fun restoreFocus(key: String?) {
        val tagged = key?.let { binding.root.findViewWithTag<View>(it) }
        val target = tagged
            ?: shelfFocus.firstOrNull()?.firstOrNull()
            ?: dockFocus.firstOrNull()
            ?: binding.entradasButton
        target.requestFocus()
    }

    private fun updateClock() {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        binding.greeting.setText(
            when {
                hour < 5 -> R.string.greeting_night
                hour < 12 -> R.string.greeting_morning
                hour < 18 -> R.string.greeting_afternoon
                else -> R.string.greeting_night
            },
        )
        val locale = Locale("pt", "BR")
        val pattern = if (android.text.format.DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm"
        binding.clock.text = SimpleDateFormat(pattern, locale).format(now.time)
        val date = SimpleDateFormat("EEEE, d 'de' MMMM", locale).format(now.time)
        binding.dateLabel.text = date.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
    }

    private fun registerTime() {
        if (timeRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        try {
            registerSystem(timeReceiver, filter)
            timeRegistered = true
        } catch (_: RuntimeException) {
            timeRegistered = false
        }
    }

    private fun registerPackages() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        try {
            registerSystem(packageReceiver, filter)
            packagesRegistered = true
        } catch (_: RuntimeException) {
            // A home não pode cair se o sistema recusar o receiver.
            // onResume relê os pacotes instalados de qualquer forma.
            packagesRegistered = false
        }
    }

    private fun registerSystem(receiver: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

    companion object {
        private const val STATE_FOCUS = "focus"
        private const val KEY_ENTRADAS = "entradas"
        private const val KEY_ALL_APPS = "todos"
    }
}
