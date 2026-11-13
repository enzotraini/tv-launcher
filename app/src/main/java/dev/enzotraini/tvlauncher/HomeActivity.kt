package dev.enzotraini.tvlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.palette.graphics.Palette
import dev.enzotraini.tvlauncher.apps.InstalledApps
import dev.enzotraini.tvlauncher.apps.LaunchableApp
import dev.enzotraini.tvlauncher.databinding.ActivityHomeBinding
import dev.enzotraini.tvlauncher.databinding.ItemDockBinding
import dev.enzotraini.tvlauncher.databinding.ViewTabBinding
import dev.enzotraini.tvlauncher.model.HomeTab
import dev.enzotraini.tvlauncher.model.Load
import dev.enzotraini.tvlauncher.model.MediaItem
import dev.enzotraini.tvlauncher.model.MediaKind
import dev.enzotraini.tvlauncher.model.OfferKind
import dev.enzotraini.tvlauncher.model.ShelfMemory
import dev.enzotraini.tvlauncher.model.WeatherKind
import dev.enzotraini.tvlauncher.model.broadcastWeek
import dev.enzotraini.tvlauncher.model.channelHint
import dev.enzotraini.tvlauncher.model.clockLabel
import dev.enzotraini.tvlauncher.model.matchProvider
import dev.enzotraini.tvlauncher.model.metaLine
import dev.enzotraini.tvlauncher.model.nextHeroIndex
import dev.enzotraini.tvlauncher.model.previousHeroIndex
import dev.enzotraini.tvlauncher.model.pageTint
import dev.enzotraini.tvlauncher.model.planDock
import dev.enzotraini.tvlauncher.model.resolveChannel
import dev.enzotraini.tvlauncher.model.resolveService
import dev.enzotraini.tvlauncher.model.temperatureLabel
import dev.enzotraini.tvlauncher.model.tmdbImage
import dev.enzotraini.tvlauncher.ui.ChannelCard
import dev.enzotraini.tvlauncher.ui.DockBrands
import dev.enzotraini.tvlauncher.ui.Dpad
import dev.enzotraini.tvlauncher.ui.HeroBanner
import dev.enzotraini.tvlauncher.ui.PosterCard
import dev.enzotraini.tvlauncher.ui.animatePageColor
import dev.enzotraini.tvlauncher.ui.applyRoundOutline
import dev.enzotraini.tvlauncher.ui.dp
import dev.enzotraini.tvlauncher.ui.tvFocus
import java.time.LocalDate
import java.time.LocalTime
class HomeActivity : TvActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var config: LauncherConfig
    private lateinit var installedApps: InstalledApps
    private val handler = Handler(Looper.getMainLooper())
    private val tabViews = mutableListOf<TabSlot>()
    private val memory = ShelfMemory()
    private val posters = mutableListOf<View>()
    private val posterCards = LinkedHashMap<String, PosterCard>()
    private val channels = mutableListOf<View>()
    private val freebies = mutableListOf<View>()
    private val dockViews = mutableListOf<View>()
    private var hero: HeroBanner? = null
    private var heroItems: List<MediaItem> = emptyList()
    private var heroIndex = 0
    private var heroBitmap: Bitmap? = null
    private var currentTab = HomeTab.EXPLORAR
    private var activeRow = HomeTab.EXPLORAR.shelfId
    private var appsSignature: String? = null
    private var lastFocusKey: String? = null
    private var loadGeneration = 0
    private var tintToken = 0
    private var packagesRegistered = false
    private var focusTouched = false
    private var restoring = false

    private val clockTick = object : Runnable {
        override fun run() {
            binding.clock.text = clockLabel(LocalTime.now())
            handler.postDelayed(this, 15_000L)
        }
    }

    private val heroTick = object : Runnable {
        override fun run() {
            advanceHero()
            handler.postDelayed(this, config.heroSeconds * 1_000L)
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            appsSignature = null
            refreshDock()
            if (currentTab == HomeTab.AO_VIVO) showLive()
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
        if (Build.VERSION.SDK_INT >= 28) {
            binding.dock.outlineSpotShadowColor = 0x38000000
            binding.dock.outlineAmbientShadowColor = 0x14000000
        }
        bindTabs()
        binding.settingsButton.tag = KEY_ENTRADAS
        binding.settingsButton.tvFocus(1.05f) { focused ->
            if (focused) noteFocus(KEY_ENTRADAS)
        }
        binding.settingsButton.setOnClickListener { openInputs() }
        selectTab(currentTab, fetch = false)
        refreshDock()
        registerPackages()
        binding.root.post { restoreFocus(lastFocusKey) }
    }

    override fun onResume() {
        restoring = true
        super.onResume()
        handler.removeCallbacks(clockTick)
        clockTick.run()
        loadWeather()
        refreshDock()
        if (currentTab == HomeTab.AO_VIVO) {
            if (channels.isEmpty()) showLive()
        } else if (posters.isEmpty() && heroItems.isEmpty()) {
            loadCatalog(currentTab, keepVisible = false)
        }
        binding.root.post {
            settleFocus(preferHero = false)
            restoring = false
        }
    }

    override fun onPause() {
        handler.removeCallbacks(clockTick)
        handler.removeCallbacks(heroTick)
        super.onPause()
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
        lastFocusKey = "tab:${HomeTab.EXPLORAR.shelfId}"
        focusTouched = false
        selectTab(HomeTab.EXPLORAR, fetch = true, force = true)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) focusTouched = true
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && currentFocus?.tag == KEY_HERO) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    stepHero(1)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    stepHero(-1)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("O Voltar não fecha a home.")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val focused = currentFocus
        val upId = focused?.nextFocusUpId ?: View.NO_ID
        if (focused != null && upId != View.NO_ID && upId != focused.id) {
            val next = findViewById<View>(upId)
            if (isUsableFocus(next)) {
                next?.requestFocus()
                return
            }
        }
        selectedTab()?.requestFocus()
    }

    private fun bindTabs() {
        binding.tabRow.removeAllViews()
        tabViews.clear()
        val tabs = HomeTab.entries
        for (tab in tabs) {
            val tabBinding = ViewTabBinding.inflate(layoutInflater, binding.tabRow, false)
            val label = tabBinding.tabLabel
            label.id = View.generateViewId()
            label.setText(tabLabel(tab))
            label.contentDescription = getString(tabLabel(tab))
            label.tag = "tab:${tab.shelfId}"
            label.tvFocus(1f) { focused ->
                if (focused) {
                    noteFocus("tab:${tab.shelfId}")
                    memory.rememberKey("tab", "tab:${tab.shelfId}")
                    selectTab(tab, fetch = true)
                }
            }
            label.setOnClickListener { selectTab(tab, fetch = true) }
            binding.tabRow.addView(label)
            tabViews += TabSlot(tab, label)
        }
    }

    private fun selectTab(tab: HomeTab, fetch: Boolean, force: Boolean = false) {
        val changed = force || tab != currentTab
        currentTab = tab
        renderTabs()
        if (!changed) return
        handler.removeCallbacks(heroTick)
        if (tab == HomeTab.AO_VIVO) {
            showLive()
            return
        }
        if (fetch) loadCatalog(tab, keepVisible = false)
    }

    private fun renderTabs() {
        for (slot in tabViews) {
            val selected = slot.tab == currentTab
            slot.label.setTextColor(getColor(if (selected) R.color.ink else R.color.tab_inactive))
            slot.label.typeface = resources.getFont(if (selected) R.font.inter_semibold else R.font.inter_regular)
            slot.label.background = if (selected) {
                getDrawable(R.drawable.tab_selected_underline)?.constantState?.newDrawable()?.mutate()
            } else {
                null
            }
        }
    }

    private fun loadCatalog(tab: HomeTab, keepVisible: Boolean) {
        val generation = ++loadGeneration
        binding.catalogBlock.visibility = View.VISIBLE
        binding.liveBlock.visibility = View.GONE
        val fixture = BuildConfig.DEBUG && app().tmdbForTest != null
        if (BuildConfig.TMDB_API_KEY.isBlank() && !fixture) {
            showMessage(getString(R.string.empty_key))
            return
        }
        if (!keepVisible) showSkeleton(tab)
        app().io.execute {
            val today = LocalDate.now()
            val week = broadcastWeek(today)
            val tmdb = app().tmdb()
            val heroLoad: Load<List<MediaItem>>
            val shelfLoad: Load<List<MediaItem>>
            when (tab) {
                HomeTab.EXPLORAR -> {
                    heroLoad = tmdb.trending()
                    shelfLoad = tmdb.weekly(week.first, week.second)
                }
                HomeTab.FILMES -> {
                    heroLoad = tmdb.movies()
                    shelfLoad = heroLoad
                }
                HomeTab.SERIES -> {
                    heroLoad = tmdb.series()
                    shelfLoad = heroLoad
                }
                HomeTab.AO_VIVO -> return@execute
            }
            runOnUiThread {
                if (generation != loadGeneration || currentTab != tab) return@runOnUiThread
                renderCatalog(tab, heroLoad, shelfLoad)
            }
        }
    }

    private fun renderCatalog(
        tab: HomeTab,
        heroLoad: Load<List<MediaItem>>,
        shelfLoad: Load<List<MediaItem>>,
    ) {
        val trending = (heroLoad as? Load.Ready)?.value.orEmpty()
        val shelfItems = (shelfLoad as? Load.Ready)?.value.orEmpty()
        val feature = trending.ifEmpty { shelfItems }
        if (heroLoad is Load.MissingKey || shelfLoad is Load.MissingKey) {
            showMessage(getString(R.string.empty_key))
            return
        }
        if (feature.isEmpty() && shelfItems.isEmpty()) {
            val message = if (heroLoad is Load.Unavailable || shelfLoad is Load.Unavailable) {
                getString(R.string.empty_offline)
            } else {
                getString(R.string.empty_shelf)
            }
            showMessage(message)
            return
        }
        binding.catalogBlock.visibility = View.VISIBLE
        binding.emptyMessage.visibility = View.GONE
        binding.heroHost.visibility = if (feature.isEmpty()) View.GONE else View.VISIBLE
        binding.shelfTitle.visibility = View.VISIBLE
        binding.posterScroll.visibility = View.VISIBLE
        binding.shelfTitle.text = shelfTitle(tab)
        if (shelfItems.isEmpty()) {
            binding.emptyMessage.visibility = View.VISIBLE
            binding.emptyMessage.text = if (shelfLoad is Load.Unavailable) {
                getString(R.string.empty_offline)
            } else {
                getString(R.string.empty_shelf)
            }
        }
        bindHero(tab, feature, feature === trending || tab == HomeTab.EXPLORAR && trending.isNotEmpty())
        bindPosters(tab, shelfItems)
        wireFocus()
        settleFocus(preferHero = !focusTouched)
        prefetchBadges((feature + shelfItems).distinctBy { it.kind to it.id }.take(8))
    }

    private fun showMessage(message: String) {
        heroItems = emptyList()
        hero = null
        posters.clear()
        posterCards.clear()
        binding.heroHost.removeAllViews()
        binding.posterRow.removeAllViews()
        binding.catalogBlock.visibility = View.GONE
        binding.liveBlock.visibility = View.GONE
        binding.emptyMessage.visibility = View.VISIBLE
        binding.emptyMessage.text = message
        wireFocus()
    }

    private fun showSkeleton(tab: HomeTab) {
        binding.catalogBlock.visibility = View.VISIBLE
        binding.liveBlock.visibility = View.GONE
        binding.emptyMessage.visibility = View.GONE
        binding.heroHost.visibility = View.VISIBLE
        binding.shelfTitle.visibility = View.VISIBLE
        binding.posterScroll.visibility = View.VISIBLE
        binding.shelfTitle.text = shelfTitle(tab)
        val banner = ensureHero()
        banner.showLoading()
        binding.posterRow.removeAllViews()
        posters.clear()
        posterCards.clear()
        repeat(6) {
            val card = PosterCard(this)
            card.showLoading()
            val params = LinearLayout.LayoutParams(dp(112), ViewGroup.LayoutParams.MATCH_PARENT)
            params.marginEnd = dp(12)
            binding.posterRow.addView(card, params)
        }
        wireFocus()
    }

    private fun bindHero(tab: HomeTab, items: List<MediaItem>, trending: Boolean) {
        heroItems = items
        heroIndex = 0
        val banner = ensureHero()
        if (items.isEmpty()) {
            banner.visibility = View.GONE
            return
        }
        banner.visibility = View.VISIBLE
        showHeroItem(items.first(), trending && tab == HomeTab.EXPLORAR)
        handler.removeCallbacks(heroTick)
        if (items.size > 1 && !usingFixture()) handler.postDelayed(heroTick, config.heroSeconds * 1_000L)
    }

    private fun ensureHero(): HeroBanner {
        hero?.let { return it }
        val banner = HeroBanner(this)
        banner.id = View.generateViewId()
        banner.tag = KEY_HERO
        banner.tvFocus(1.02f) { focused ->
            if (focused) {
                noteFocus(KEY_HERO)
                memory.rememberKey("band", KEY_HERO)
                heroBitmap?.let { tintFrom(it) }
            }
        }
        banner.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) {
                return@setOnKeyListener false
            }
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    stepHero(1)
                    true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    stepHero(-1)
                    true
                }
                else -> false
            }
        }
        binding.heroHost.removeAllViews()
        binding.heroHost.addView(
            banner,
            android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        hero = banner
        return banner
    }

    private fun showHeroItem(item: MediaItem, trending: Boolean) {
        val banner = hero ?: return
        val meta = metaLine(item)
        banner.bind(
            name = item.title,
            metaText = meta,
            kickerText = if (trending) getString(R.string.kicker_trending) else null,
            contentDescription = getString(R.string.poster_cd, item.title, meta),
        )
        banner.setAvailable(null)
        banner.setOnClickListener { openDetail(item) }
        val url = tmdbImage(item.backdropPath ?: item.posterPath, "w1280")
        val art = banner.findViewById<android.widget.ImageView>(R.id.hero_art)
        app().images.load(url, art) { bitmap ->
            heroBitmap = bitmap
            if (lastFocusKey?.startsWith("poster:") != true) tintFrom(bitmap)
        }
    }

    private fun advanceHero() {
        if (heroItems.size <= 1 || currentTab == HomeTab.AO_VIVO) return
        if (lastFocusKey?.startsWith("poster:") == true) return
        showHeroAt(nextHeroIndex(heroIndex, heroItems.size))
    }

    private fun stepHero(delta: Int) {
        if (heroItems.size <= 1 || currentTab == HomeTab.AO_VIVO) return
        val next = if (delta >= 0) {
            nextHeroIndex(heroIndex, heroItems.size)
        } else {
            previousHeroIndex(heroIndex, heroItems.size)
        }
        showHeroAt(next)
        handler.removeCallbacks(heroTick)
        if (!usingFixture()) handler.postDelayed(heroTick, config.heroSeconds * 1_000L)
    }

    private fun usingFixture(): Boolean = BuildConfig.DEBUG && app().tmdbForTest != null

    private fun showHeroAt(index: Int) {
        if (heroItems.isEmpty()) return
        heroIndex = index.coerceIn(0, heroItems.lastIndex)
        val item = heroItems[heroIndex]
        showHeroItem(item, currentTab == HomeTab.EXPLORAR)
        memory.rememberKey("hero:${currentTab.shelfId}", item.id.toString())
    }

    private fun bindPosters(tab: HomeTab, items: List<MediaItem>) {
        binding.posterRow.removeAllViews()
        posters.clear()
        posterCards.clear()
        activeRow = tab.shelfId
        items.forEachIndexed { index, item ->
            val card = PosterCard(this)
            val meta = metaLine(item)
            card.bind(item.title, getString(R.string.poster_cd, item.title, meta.ifBlank { kindLabel(item) }))
            card.id = View.generateViewId()
            card.tag = "poster:${tab.shelfId}:${item.id}"
            card.tvFocus(1.08f) { focused ->
                if (!focused) return@tvFocus
                noteFocus(card.tag as String)
                activeRow = tab.shelfId
                memory.remember(tab.shelfId, index)
                memory.rememberKey(tab.shelfId, card.tag as String)
                val drawn = (card.findViewById<android.widget.ImageView>(R.id.poster_art).drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (drawn != null) tintFrom(drawn)
            }
            card.setOnClickListener { openDetail(item) }
            val params = LinearLayout.LayoutParams(dp(112), ViewGroup.LayoutParams.MATCH_PARENT)
            params.marginEnd = dp(12)
            binding.posterRow.addView(card, params)
            posters += card
            posterCards[keyOf(item)] = card
            val url = tmdbImage(item.posterPath ?: item.backdropPath, "w342")
            app().images.load(url, card.findViewById(R.id.poster_art)) { bitmap ->
                card.setArt(bitmap)
                if (card.hasFocus()) tintFrom(bitmap)
            }
        }
    }

    private fun prefetchBadges(items: List<MediaItem>) {
        val generation = loadGeneration
        app().io.execute {
            val tmdb = app().tmdb()
            for (item in items) {
                if (generation != loadGeneration) return@execute
                val details = tmdb.details(item.kind, item.id) as? Load.Ready ?: continue
                val offer = details.value.offers.firstOrNull { it.kind != OfferKind.RENT && it.kind != OfferKind.BUY }
                    ?: details.value.offers.firstOrNull()
                val label = offer?.let { matchProvider(it.providerId, it.providerName)?.label ?: it.providerName }
                runOnUiThread {
                    if (generation != loadGeneration) return@runOnUiThread
                    posterCards[keyOf(item)]?.setBadge(label)
                    val featured = heroItems.getOrNull(heroIndex)
                    if (featured != null && featured.id == item.id && featured.kind == item.kind) {
                        hero?.setAvailable(label)
                    }
                }
            }
        }
    }

    private fun showLive() {
        handler.removeCallbacks(heroTick)
        loadGeneration++
        binding.catalogBlock.visibility = View.GONE
        binding.emptyMessage.visibility = View.GONE
        binding.liveBlock.visibility = View.VISIBLE
        val installed = installedPackages()
        binding.channelRow.removeAllViews()
        binding.freeRow.removeAllViews()
        channels.clear()
        freebies.clear()
        dev.enzotraini.tvlauncher.model.LiveCatalog.channels.forEachIndexed { index, channel ->
            val target = resolveChannel(channel, installed, config.liveTvPackage)
            val card = ChannelCard(this)
            val hint = channelHint(target, channel.appLabel)
            card.bind(channel.name, hint, dark = true, showLive = true)
            card.id = View.generateViewId()
            card.tag = "channel:${channel.id}"
            card.tvFocus(1.06f) { focused ->
                if (focused) {
                    noteFocus(card.tag as String)
                    activeRow = ROW_CHANNELS
                    memory.remember(ROW_CHANNELS, index)
                    memory.rememberKey(ROW_CHANNELS, card.tag as String)
                }
            }
            card.setOnClickListener { openChannel(target, channel.name) }
            addRowCard(binding.channelRow, card, 156, 12)
            channels += card
        }
        dev.enzotraini.tvlauncher.model.LiveCatalog.free.forEachIndexed { index, service ->
            val installedPackage = resolveService(service, installed)
            val card = ChannelCard(this)
            val hint = if (installedPackage == null) getString(R.string.not_installed) else getString(R.string.free_badge)
            card.bind(service.name, hint, dark = false, showLive = false)
            card.id = View.generateViewId()
            card.tag = "free:${service.id}"
            card.tvFocus(1.06f) { focused ->
                if (focused) {
                    noteFocus(card.tag as String)
                    activeRow = ROW_FREE
                    memory.remember(ROW_FREE, index)
                    memory.rememberKey(ROW_FREE, card.tag as String)
                }
            }
            card.setOnClickListener {
                if (installedPackage == null || !installedApps.openPackage(installedPackage)) {
                    Toast.makeText(this, getString(R.string.app_missing, service.name), Toast.LENGTH_SHORT).show()
                }
            }
            addRowCard(binding.freeRow, card, 156, 12)
            freebies += card
        }
        activeRow = ROW_CHANNELS
        wireFocus()
        if (!focusTouched) channels.firstOrNull()?.requestFocus()
    }

    private fun openChannel(target: dev.enzotraini.tvlauncher.model.ChannelTarget, name: String) {
        when (target) {
            is dev.enzotraini.tvlauncher.model.ChannelTarget.App -> {
                if (!installedApps.openPackage(target.packageName)) {
                    Toast.makeText(this, getString(R.string.app_missing, name), Toast.LENGTH_SHORT).show()
                }
            }
            is dev.enzotraini.tvlauncher.model.ChannelTarget.Tuner -> {
                if (!installedApps.openPackage(target.packageName)) {
                    Toast.makeText(this, R.string.live_tv_missing, Toast.LENGTH_SHORT).show()
                }
            }
            dev.enzotraini.tvlauncher.model.ChannelTarget.Unavailable -> {
                Toast.makeText(this, R.string.live_tv_missing, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadWeather() {
        val city = config.weatherCity
        binding.weatherText.contentDescription = getString(R.string.weather_cd, city)
        app().io.execute {
            val result = app().weather().current(config.weatherLatitude, config.weatherLongitude)
            runOnUiThread {
                val ready = result as? Load.Ready
                if (ready == null) {
                    binding.weatherText.text = city
                    return@runOnUiThread
                }
                binding.weatherText.text = temperatureLabel(ready.value.celsius, city)
                binding.weatherIcon.visibility = View.VISIBLE
                if (ready.value.kind == WeatherKind.UNKNOWN) {
                    binding.weatherIcon.alpha = 0.7f
                }
            }
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
        binding.root.post { if (!isUsableFocus(currentFocus)) restoreFocus(key) }
    }

    private fun bindDock(apps: List<LaunchableApp>) {
        val byPackage = apps.associateBy { it.packageName }
        val slots = planDock(config.dockEntries, byPackage.keys)
        binding.dockRow.removeAllViews()
        dockViews.clear()
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
                contentDescription = if (installed) slot.label else getString(R.string.dock_missing_cd, slot.label),
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
        frameParams.height = dp(48)
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
        root.tag = focusKey
        root.setOnClickListener { onClick() }
        root.tvFocus(1.06f) { focused ->
            if (focused) {
                noteFocus(focusKey)
                memory.rememberKey("dock", focusKey)
            }
        }
        binding.dockRow.addView(root)
        dockViews += root
    }

    private fun wireFocus() {
        applyLinks(collectRows())
        binding.root.post {
            if (isFinishing) return@post
            applyLinks(collectRows())
        }
    }

    private fun collectRows(): List<List<View>> {
        val rows = mutableListOf<List<View>>()
        val tabs = tabViews.map { it.label } + binding.settingsButton
        if (tabs.isNotEmpty()) rows += tabs
        val feature = hero?.takeIf { it.isShown && it.isFocusable && currentTab != HomeTab.AO_VIVO }
        if (feature != null) rows += listOf(feature)
        if (currentTab == HomeTab.AO_VIVO) {
            if (channels.isNotEmpty()) rows += channels.toList()
            if (freebies.isNotEmpty()) rows += freebies.toList()
        } else if (posters.isNotEmpty()) {
            rows += posters.toList()
        }
        if (dockViews.isNotEmpty()) rows += dockViews.toList()
        return rows
    }

    private fun applyLinks(rows: List<List<View>>) {
        Dpad.stack(rows)
        val tab = selectedTab() ?: return
        val feature = hero?.takeIf { it.isShown && it.isFocusable && currentTab != HomeTab.AO_VIVO }
        if (feature != null) {
            feature.nextFocusUpId = tab.id
            val poster = rememberedPoster()
            if (poster != null) {
                feature.nextFocusDownId = poster.id
                posters.forEach { it.nextFocusUpId = feature.id }
            }
            return
        }
        val firstContent = when {
            currentTab == HomeTab.AO_VIVO && channels.isNotEmpty() -> channels
            posters.isNotEmpty() -> posters
            else -> emptyList()
        }
        firstContent.forEach { it.nextFocusUpId = tab.id }
    }

    private fun rememberedPoster(): View? {
        if (posters.isEmpty() || currentTab == HomeTab.AO_VIVO) return null
        val saved = memory.recallKey(currentTab.shelfId)
        val tagged = saved?.let { binding.root.findViewWithTag<View>(it) }
        if (tagged != null && tagged in posters && isUsableFocus(tagged)) return tagged
        return posters.getOrNull(memory.recall(currentTab.shelfId, posters.size))
    }

    private fun noteFocus(key: String) {
        if (restoring) return
        lastFocusKey = key
    }

    private fun settleFocus(preferHero: Boolean) {
        val feature = hero?.takeIf { isUsableFocus(it) && currentTab != HomeTab.AO_VIVO }
        if (preferHero && feature != null && (currentFocus == null || currentFocus == selectedTab())) {
            feature.requestFocus()
            return
        }
        val saved = lastFocusKey?.let { binding.root.findViewWithTag<View>(it) }?.takeIf { isUsableFocus(it) }
        val parkedOnChrome = currentFocus == null || currentFocus == selectedTab() || currentFocus == binding.settingsButton
        if (saved != null && parkedOnChrome && saved != currentFocus) {
            saved.requestFocus()
            return
        }
        if (isUsableFocus(currentFocus)) return
        restoreFocus(lastFocusKey)
    }

    private fun restoreFocus(key: String?) {
        val tagged = key?.let { binding.root.findViewWithTag<View>(it) }?.takeIf { isUsableFocus(it) }
        val section = when (currentTab) {
            HomeTab.AO_VIVO -> memory.recallKey(activeRow)
            else -> memory.recallKey(currentTab.shelfId)
        }
        val remembered = section?.let { binding.root.findViewWithTag<View>(it) }?.takeIf { isUsableFocus(it) }
        val target = tagged
            ?: remembered
            ?: hero?.takeIf { isUsableFocus(it) && currentTab != HomeTab.AO_VIVO }
            ?: posters.firstOrNull { isUsableFocus(it) }
            ?: channels.firstOrNull { isUsableFocus(it) }
            ?: selectedTab()
            ?: dockViews.firstOrNull { isUsableFocus(it) }
            ?: binding.settingsButton
        target.requestFocus()
    }

    private fun isUsableFocus(view: View?): Boolean {
        if (view == null || !view.isShown || !view.isFocusable) return false
        if (view is android.widget.ScrollView || view is android.widget.HorizontalScrollView) return false
        return view.isClickable
    }

    private fun tintFrom(bitmap: Bitmap) {
        val token = ++tintToken
        app().io.execute {
            val color = try {
                val palette = Palette.from(bitmap).maximumColorCount(12).generate()
                palette.vibrantSwatch?.rgb ?: palette.dominantSwatch?.rgb
            } catch (_: Exception) {
                null
            } ?: return@execute
            val tint = pageTint(color)
            runOnUiThread {
                if (token != tintToken) return@runOnUiThread
                binding.homeRoot.animatePageColor(tint)
            }
        }
    }

    private fun openDetail(item: MediaItem) {
        startActivity(
            Intent(this, DetailActivity::class.java)
                .putExtra(DetailActivity.EXTRA_KIND, if (item.kind == MediaKind.MOVIE) "movie" else "series")
                .putExtra(DetailActivity.EXTRA_ID, item.id),
        )
    }

    private fun installedPackages(): Set<String> {
        val names = dev.enzotraini.tvlauncher.model.LiveCatalog.installedPackages() + config.liveTvPackage
        return names.filter { installedApps.isInstalled(it) }.toSet()
    }

    private fun registerPackages() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(packageReceiver, filter, RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(packageReceiver, filter)
            }
            packagesRegistered = true
        } catch (_: RuntimeException) {
            packagesRegistered = false
        }
    }

    private fun selectedTab(): View? = tabViews.firstOrNull { it.tab == currentTab }?.label

    private fun tabLabel(tab: HomeTab): Int = when (tab) {
        HomeTab.EXPLORAR -> R.string.tab_explorar
        HomeTab.FILMES -> R.string.tab_filmes
        HomeTab.SERIES -> R.string.tab_series
        HomeTab.AO_VIVO -> R.string.tab_ao_vivo
    }

    private fun shelfTitle(tab: HomeTab): String = when (tab) {
        HomeTab.EXPLORAR -> getString(R.string.shelf_week)
        HomeTab.FILMES -> getString(R.string.shelf_movies)
        HomeTab.SERIES -> getString(R.string.shelf_series)
        HomeTab.AO_VIVO -> getString(R.string.shelf_channels)
    }

    private fun kindLabel(item: MediaItem): String {
        return getString(if (item.kind == MediaKind.MOVIE) R.string.kind_movie else R.string.kind_series)
    }

    private fun addRowCard(row: LinearLayout, view: View, widthDp: Int, gapDp: Int) {
        val params = LinearLayout.LayoutParams(dp(widthDp), ViewGroup.LayoutParams.MATCH_PARENT)
        params.marginEnd = dp(gapDp)
        row.addView(view, params)
    }

    private fun keyOf(item: MediaItem): String = "${item.kind}:${item.id}"

    private data class TabSlot(val tab: HomeTab, val label: TextView)

    companion object {
        private const val STATE_FOCUS = "focus"
        private const val STATE_TAB = "tab"
        private const val KEY_ENTRADAS = "entradas"
        private const val KEY_HERO = "hero"
        private const val KEY_ALL_APPS = "todos"
        private const val ROW_CHANNELS = "channels"
        private const val ROW_FREE = "free"
    }
}
