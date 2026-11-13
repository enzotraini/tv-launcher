package dev.enzotraini.tvlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.Toast
import dev.enzotraini.tvlauncher.apps.InstalledApps
import dev.enzotraini.tvlauncher.apps.LaunchableApp
import dev.enzotraini.tvlauncher.databinding.ActivityHomeBinding
import dev.enzotraini.tvlauncher.databinding.ItemDockBinding
import dev.enzotraini.tvlauncher.databinding.ItemFeatureBinding
import dev.enzotraini.tvlauncher.databinding.ViewTabBinding
import dev.enzotraini.tvlauncher.model.HomeTab
import dev.enzotraini.tvlauncher.model.Kind
import dev.enzotraini.tvlauncher.model.RecommendationCatalog
import dev.enzotraini.tvlauncher.model.featured
import dev.enzotraini.tvlauncher.model.metadata
import dev.enzotraini.tvlauncher.model.planDock
import dev.enzotraini.tvlauncher.ui.Artwork
import dev.enzotraini.tvlauncher.ui.DockBrands
import dev.enzotraini.tvlauncher.ui.Dpad
import dev.enzotraini.tvlauncher.ui.applyRoundOutline
import kotlin.math.roundToInt

class HomeActivity : TvActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var config: LauncherConfig
    private lateinit var installedApps: InstalledApps
    private val tabViews = mutableListOf<TabSlot>()
    private val featuredFocus = mutableListOf<View>()
    private val dockFocus = mutableListOf<View>()
    private var currentTab = HomeTab.FILMES
    private var appsSignature: String? = null
    private var lastFocusKey: String? = null
    private var packagesRegistered = false

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
        currentTab = HomeTab.fromId(savedInstanceState?.getString(STATE_TAB))
        bindTabs()
        bindIconCluster()
        selectTab(currentTab, force = true)
        refreshDock()
        registerPackages()
        if (Build.VERSION.SDK_INT >= 28) {
            binding.dock.outlineSpotShadowColor = 0x38000000
            binding.dock.outlineAmbientShadowColor = 0x14000000
        }
        // O primeiro foco tem de ser Filmes. Sem isso o sistema foca Explorar
        // e a aba troca antes da home aparecer.
        restoreFocus(lastFocusKey)
    }

    override fun onResume() {
        super.onResume()
        refreshDock()
        binding.root.post {
            if (currentFocus == null) restoreFocus(lastFocusKey)
        }
    }

    override fun onDestroy() {
        if (packagesRegistered) unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_FOCUS, lastFocusKey)
        outState.putString(STATE_TAB, currentTab.shelfId)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lastFocusKey = null
        selectTab(HomeTab.FILMES, force = true)
        binding.root.post { restoreFocus(null) }
    }

    @Deprecated("O Voltar não fecha a home.")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        lastFocusKey = null
        restoreFocus(null)
    }

    private fun bindTabs() {
        binding.tabRow.removeAllViews()
        tabViews.clear()
        val tabs = HomeTab.entries
        for (tab in tabs) {
            val tabBinding = ViewTabBinding.inflate(layoutInflater, binding.tabRow, true)
            val label = tabBinding.tabLabel
            label.id = View.generateViewId()
            label.setText(tabLabel(tab))
            label.contentDescription = getString(tabLabel(tab))
            if (tab == tabs.last()) {
                (label.layoutParams as LinearLayout.LayoutParams).marginEnd = 0
            }
            label.setOnClickListener { selectTab(tab) }
            watchFocus(label, "tab:${tab.shelfId}", scale = 1f) { focused ->
                if (focused) selectTab(tab)
            }
            tabViews += TabSlot(tab, tabBinding)
        }
    }

    private fun bindIconCluster() {
        watchFocus(binding.castButton, KEY_CAST, scale = 1.05f)
        watchFocus(binding.editButton, KEY_EDIT, scale = 1.05f)
        watchFocus(binding.settingsButton, KEY_ENTRADAS, scale = 1.05f)
        binding.castButton.setOnClickListener { openCast() }
        binding.editButton.setOnClickListener { openAppSettings() }
        binding.settingsButton.setOnClickListener { openInputs() }
    }

    private fun selectTab(tab: HomeTab, force: Boolean = false) {
        val same = tab == currentTab && binding.featuredRow.childCount > 0
        currentTab = tab
        renderTabs()
        if (same && !force) return
        bindFeatured()
        wireFocus()
    }

    private fun renderTabs() {
        for (slot in tabViews) {
            val selected = slot.tab == currentTab
            val label = slot.binding.tabLabel
            label.setTextColor(getColor(if (selected) R.color.ink else R.color.tab_inactive))
            label.typeface = resources.getFont(
                if (selected) R.font.inter_semibold else R.font.inter_regular,
            )
            label.background = if (selected) {
                getDrawable(R.drawable.tab_selected_underline)?.constantState?.newDrawable()?.mutate()
            } else {
                null
            }
        }
    }

    private fun bindFeatured() {
        val items = RecommendationCatalog.current().shelves()
            .firstOrNull { it.id == currentTab.shelfId }
            ?.featured()
            .orEmpty()
        binding.featuredRow.removeAllViews()
        featuredFocus.clear()
        val radius = resources.getDimension(R.dimen.poster_radius)
        items.forEachIndexed { index, title ->
            val poster = ItemFeatureBinding.inflate(layoutInflater, binding.featuredRow, false)
            val root = poster.featureRoot
            root.id = View.generateViewId()
            val params = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                if (index == 0) 2.55f else 1f,
            )
            if (index != items.lastIndex) params.marginEnd = dp(16)
            root.layoutParams = params
            val artwork = Artwork.resId(title.artwork)
            if (artwork != 0) poster.featureArt.setImageResource(artwork)
            poster.featureArt.applyRoundOutline(radius, clip = true)
            poster.featureFrame.applyRoundOutline(radius, clip = false)
            if (Build.VERSION.SDK_INT >= 28) {
                poster.featureFrame.outlineSpotShadowColor = 0x40000000
                poster.featureFrame.outlineAmbientShadowColor = 0x18000000
            }
            val kind = getString(if (title.kind == Kind.MOVIE) R.string.kind_movie else R.string.kind_series)
            poster.featureTitle.text = title.name
            poster.featureMeta.text = title.metadata(kind)
            root.contentDescription = getString(R.string.poster_cd, title.name, poster.featureMeta.text)
            root.setOnClickListener {
                startActivity(
                    Intent(this, DetailActivity::class.java)
                        .putExtra(DetailActivity.EXTRA_TITLE_ID, title.id),
                )
            }
            watchFocus(root, "title:${title.id}", scale = 1.035f)
            binding.featuredRow.addView(root)
            featuredFocus += root
        }
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
            val installedApp = byPackage[slot.packageName]
            val installed = installedApp != null && slot.installed
            val brand = DockBrands.forPackage(slot.packageName, slot.label)
            addDockTile(
                widthDp = brand.widthDp,
                backgroundRes = brand.backgroundRes,
                word = brand.word,
                wordColorRes = brand.wordColorRes,
                textSizeSp = brand.textSizeSp,
                letterSpacing = brand.letterSpacing,
                fontRes = brand.fontRes,
                markRes = brand.markRes,
                markSizeDp = if (brand.markRes != 0) 18 else 0,
                contentDescription = if (installed) {
                    slot.label
                } else {
                    getString(R.string.dock_missing_cd, slot.label)
                },
                focusKey = "dock:${slot.packageName}",
            ) {
                if (installedApp != null && slot.installed) {
                    installedApps.open(installedApp.component)
                } else {
                    Toast.makeText(this, getString(R.string.app_missing, slot.label), Toast.LENGTH_SHORT).show()
                }
            }
        }
        addDockTile(
            widthDp = 50,
            backgroundRes = R.drawable.dock_todos,
            word = null,
            wordColorRes = R.color.ink,
            textSizeSp = 14f,
            letterSpacing = 0f,
            fontRes = R.font.inter_bold,
            markRes = R.drawable.ic_apps,
            markSizeDp = 22,
            contentDescription = getString(R.string.todos_cd),
            focusKey = KEY_ALL_APPS,
        ) {
            startActivity(Intent(this, AppsActivity::class.java))
        }
        val last = binding.dockRow.getChildAt(binding.dockRow.childCount - 1)
        (last?.layoutParams as? LinearLayout.LayoutParams)?.marginEnd = 0
    }

    private fun addDockTile(
        widthDp: Int,
        backgroundRes: Int,
        word: String?,
        wordColorRes: Int,
        textSizeSp: Float,
        letterSpacing: Float,
        fontRes: Int,
        markRes: Int,
        markSizeDp: Int,
        contentDescription: String,
        focusKey: String,
        onClick: () -> Unit,
    ) {
        val item = ItemDockBinding.inflate(layoutInflater, binding.dockRow, false)
        val root = item.dockRoot
        root.id = View.generateViewId()
        val frameParams = item.dockFrame.layoutParams
        frameParams.width = dp(widthDp)
        frameParams.height = dp(50)
        item.dockFrame.layoutParams = frameParams
        item.dockFrame.background = getDrawable(backgroundRes)?.mutate()
        item.dockFrame.applyRoundOutline(resources.getDimension(R.dimen.dock_tile_radius), clip = true)
        if (word.isNullOrEmpty()) {
            item.dockWord.visibility = View.GONE
        } else {
            item.dockWord.visibility = View.VISIBLE
            item.dockWord.text = word
            item.dockWord.setTextColor(getColor(wordColorRes))
            item.dockWord.textSize = textSizeSp
            item.dockWord.letterSpacing = letterSpacing
            item.dockWord.typeface = resources.getFont(fontRes)
        }
        if (markRes != 0) {
            item.dockMark.setImageResource(markRes)
            item.dockMark.visibility = View.VISIBLE
            val markParams = item.dockMark.layoutParams as LinearLayout.LayoutParams
            markParams.width = dp(markSizeDp)
            markParams.height = dp(markSizeDp)
            markParams.marginEnd = if (word.isNullOrEmpty()) 0 else dp(6)
            item.dockMark.layoutParams = markParams
        } else {
            item.dockMark.visibility = View.GONE
        }
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        params.marginEnd = dp(6)
        root.layoutParams = params
        root.contentDescription = contentDescription
        root.setOnClickListener { onClick() }
        watchFocus(root, focusKey, 1.06f)
        binding.dockRow.addView(root)
        dockFocus += root
    }

    private fun wireFocus() {
        val top = buildList {
            tabViews.mapTo(this) { it.binding.tabLabel }
            add(binding.castButton)
            add(binding.editButton)
            add(binding.settingsButton)
        }
        val rows = buildList {
            if (top.isNotEmpty()) add(top)
            if (featuredFocus.isNotEmpty()) add(featuredFocus.toList())
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
        view.isFocusableInTouchMode = true
        view.setOnFocusChangeListener { target, hasFocus ->
            target.animate().cancel()
            target.animate()
                .scaleX(if (hasFocus) scale else 1f)
                .scaleY(if (hasFocus) scale else 1f)
                .setDuration(if (hasFocus) 160 else 120)
                .setInterpolator(DecelerateInterpolator())
                .start()
            target.translationZ = if (hasFocus) resources.getDimension(R.dimen.focus_elevation) else 0f
            if (hasFocus) {
                lastFocusKey = key
                target.requestRectangleOnScreen(Rect(0, 0, target.width, target.height), false)
            }
            onFocus(hasFocus)
        }
    }

    private fun restoreFocus(key: String?) {
        val tagged = key?.let { binding.root.findViewWithTag<View>(it) }
        val filmes = tabViews.firstOrNull { it.tab == HomeTab.FILMES }?.binding?.tabLabel
        val target = tagged
            ?: filmes
            ?: featuredFocus.firstOrNull()
            ?: dockFocus.firstOrNull()
            ?: binding.settingsButton
        target.requestFocus()
    }

    private fun openCast() {
        val actions = listOf(
            "android.settings.CAST_SETTINGS",
            "android.settings.WIFI_DISPLAY_SETTINGS",
        )
        for (action in actions) {
            val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val resolved = try {
                intent.resolveActivity(packageManager) != null
            } catch (_: RuntimeException) {
                false
            }
            if (!resolved) continue
            try {
                startActivity(intent)
                return
            } catch (_: RuntimeException) {
                continue
            }
        }
        Toast.makeText(this, R.string.cast_unavailable, Toast.LENGTH_SHORT).show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resolved = try {
            intent.resolveActivity(packageManager) != null
        } catch (_: RuntimeException) {
            false
        }
        if (!resolved) {
            Toast.makeText(this, R.string.edit_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(intent)
        } catch (_: RuntimeException) {
            Toast.makeText(this, R.string.edit_unavailable, Toast.LENGTH_SHORT).show()
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

    private fun tabLabel(tab: HomeTab): Int {
        return when (tab) {
            HomeTab.EXPLORAR -> R.string.tab_explorar
            HomeTab.FILMES -> R.string.tab_filmes
            HomeTab.SERIES -> R.string.tab_series
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

    private data class TabSlot(val tab: HomeTab, val binding: ViewTabBinding)

    companion object {
        private const val STATE_FOCUS = "focus"
        private const val STATE_TAB = "tab"
        private const val KEY_ENTRADAS = "entradas"
        private const val KEY_CAST = "cast"
        private const val KEY_EDIT = "edit"
        private const val KEY_ALL_APPS = "todos"
    }
}
