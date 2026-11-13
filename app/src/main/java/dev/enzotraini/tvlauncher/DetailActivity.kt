package dev.enzotraini.tvlauncher

import android.os.Bundle
import android.view.View
import dev.enzotraini.tvlauncher.databinding.ActivityDetailBinding
import dev.enzotraini.tvlauncher.model.Kind
import dev.enzotraini.tvlauncher.model.RecommendationCatalog
import dev.enzotraini.tvlauncher.model.monogram
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
        binding.detailKind.text = getString(R.string.title_meta, kind, title.year)
        binding.detailTitle.text = title.name
        binding.detailBlurb.text = title.blurb
        binding.detailArt.accent = title.accent
        binding.detailArt.monogram = monogram(title.name)
        binding.detailArt.kindLabel = kind
        binding.detailArt.progress = title.progress
        val progress = title.progress
        if (progress != null) {
            binding.detailProgress.visibility = View.VISIBLE
            binding.detailProgress.text = getString(
                R.string.progress_watched,
                (progress * 100f).roundToInt(),
            )
        }
        binding.detailRoot.requestFocus()
    }

    companion object {
        const val EXTRA_TITLE_ID = "titleId"
    }
}
