package dev.enzotraini.tvlauncher

import android.os.Bundle
import dev.enzotraini.tvlauncher.databinding.ActivityDetailBinding
import dev.enzotraini.tvlauncher.model.Kind
import dev.enzotraini.tvlauncher.model.RecommendationCatalog
import dev.enzotraini.tvlauncher.model.metadata
import dev.enzotraini.tvlauncher.ui.Artwork
import dev.enzotraini.tvlauncher.ui.applyRoundOutline
import kotlin.math.roundToInt

class DetailActivity : TvActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = RecommendationCatalog.find(intent.getStringExtra(EXTRA_TITLE_ID))
        if (title == null) {
            finish()
            return
        }
        val binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val kind = getString(if (title.kind == Kind.MOVIE) R.string.kind_movie else R.string.kind_series)
        binding.detailKind.text = title.metadata(kind)
        binding.detailTitle.text = title.name
        binding.detailBlurb.text = title.blurb
        val artwork = Artwork.resId(title.artwork)
        if (artwork != 0) binding.detailArt.setImageResource(artwork)
        val params = binding.detailFrame.layoutParams
        params.width = dp(if (title.hero) 560 else 300)
        params.height = dp(if (title.hero) 315 else 450)
        binding.detailFrame.layoutParams = params
        binding.detailFrame.applyRoundOutline(resources.getDimension(R.dimen.poster_radius), clip = true)
        binding.detailRoot.requestFocus()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

    companion object {
        const val EXTRA_TITLE_ID = "titleId"
    }
}
