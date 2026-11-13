package dev.enzotraini.tvlauncher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import dev.enzotraini.tvlauncher.apps.InstalledApps
import dev.enzotraini.tvlauncher.databinding.ActivityDetailBinding
import dev.enzotraini.tvlauncher.model.Load
import dev.enzotraini.tvlauncher.model.MediaKind
import dev.enzotraini.tvlauncher.model.OfferKind
import dev.enzotraini.tvlauncher.model.PlayAction
import dev.enzotraini.tvlauncher.model.WatchOffer
import dev.enzotraini.tvlauncher.model.metaLine
import dev.enzotraini.tvlauncher.model.planPlay
import dev.enzotraini.tvlauncher.model.tmdbImage
import dev.enzotraini.tvlauncher.model.watchButton
import dev.enzotraini.tvlauncher.ui.Dpad
import dev.enzotraini.tvlauncher.ui.dp
import dev.enzotraini.tvlauncher.ui.tryStart
import dev.enzotraini.tvlauncher.ui.tvFocus
import dev.enzotraini.tvlauncher.ui.viewResolves

class DetailActivity : TvActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val kind = if (intent.getStringExtra(EXTRA_KIND) == "series") MediaKind.SERIES else MediaKind.MOVIE
        val id = intent.getIntExtra(EXTRA_ID, -1)
        if (id <= 0) {
            finish()
            return
        }
        binding.detailRoot.isFocusable = false
        val fixture = BuildConfig.DEBUG && app().tmdbForTest != null
        if (BuildConfig.TMDB_API_KEY.isBlank() && !fixture) {
            binding.detailStatus.text = getString(R.string.empty_key)
            return
        }
        app().io.execute {
            val loaded = app().tmdb().details(kind, id)
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                when (loaded) {
                    is Load.Ready -> show(binding, loaded.value.item.title, loaded.value.item, loaded.value.offers)
                    else -> binding.detailStatus.text = getString(R.string.detail_failed)
                }
            }
        }
    }

    private fun show(
        binding: ActivityDetailBinding,
        title: String,
        item: dev.enzotraini.tvlauncher.model.MediaItem,
        offers: List<WatchOffer>,
    ) {
        binding.detailStatus.visibility = View.GONE
        binding.detailTitle.text = item.title
        val meta = listOf(kindName(item.kind), metaLine(item)).filter { it.isNotBlank() }.joinToString("  ·  ")
        binding.detailMeta.text = meta
        binding.detailOverview.text = item.overview
        val installed = InstalledApps(this)
        val actions = offers.map { offer ->
            offer to planPlay(offer, item.title, installedSet(installed, offers)) { uri, packageName ->
                viewResolves(uri, packageName)
            }
        }
        val playable = actions.filter { it.second !is PlayAction.NotInstalled }
        val missing = actions.filter { it.second is PlayAction.NotInstalled }.map { watchButton(it.first) }
        binding.detailActions.removeAllViews()
        val buttons = playable.map { (offer, action) ->
            button(watchButton(offer)) {
                when (action) {
                    is PlayAction.View -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.uri)).setPackage(action.packageName)
                        if (!tryStart(intent)) installed.openPackage(action.packageName)
                    }
                    is PlayAction.OpenApp -> {
                        if (!installed.openPackage(action.packageName)) {
                            Toast.makeText(this, getString(R.string.app_missing, offer.providerName), Toast.LENGTH_SHORT).show()
                        }
                    }
                    PlayAction.NotInstalled -> Unit
                }
            }
        }
        buttons.forEach { binding.detailActions.addView(it) }
        if (buttons.isNotEmpty()) {
            Dpad.stack(listOf(buttons))
            buttons.first().requestFocus()
        } else {
            binding.detailRoot.requestFocus()
        }
        binding.detailNote.text = when {
            offers.isEmpty() -> getString(R.string.no_provider)
            playable.isEmpty() -> getString(R.string.none_installed) + " " +
                offers.joinToString(", ") { it.providerName }
            missing.isEmpty() -> getString(R.string.detail_footnote)
            else -> getString(R.string.also_on, missing.joinToString(", ") { it.removePrefix("Assistir na ").removePrefix("Alugar na ").removePrefix("Comprar na ") })
        }
        val art = tmdbImage(item.backdropPath ?: item.posterPath, "w1280")
        app().images.load(art, binding.detailArt)
        binding.detailRoot.contentDescription = "$title. $meta"
    }

    private fun button(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            id = View.generateViewId()
            text = label
            setTextColor(getColor(R.color.ink))
            textSize = 18f
            typeface = resources.getFont(R.font.inter_semibold)
            background = getDrawable(R.drawable.bg_play)
            foreground = getDrawable(R.drawable.focus_pill)
            setPadding(dp(22), dp(10), dp(22), dp(10))
            isClickable = true
            isFocusable = true
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            params.marginEnd = dp(12)
            layoutParams = params
            tvFocus(1.04f)
            setOnClickListener { onClick() }
        }
    }

    private fun installedSet(apps: InstalledApps, offers: List<WatchOffer>): Set<String> {
        val packages = offers.flatMap { offer ->
            dev.enzotraini.tvlauncher.model.matchProvider(offer.providerId, offer.providerName)?.packages.orEmpty()
        }.distinct()
        return packages.filter { apps.isInstalled(it) }.toSet()
    }

    private fun kindName(kind: MediaKind): String {
        return getString(if (kind == MediaKind.MOVIE) R.string.kind_movie else R.string.kind_series)
    }

    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_ID = "tmdbId"
    }
}
