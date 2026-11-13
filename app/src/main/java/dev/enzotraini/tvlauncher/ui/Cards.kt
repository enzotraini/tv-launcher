package dev.enzotraini.tvlauncher.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import dev.enzotraini.tvlauncher.R
import dev.enzotraini.tvlauncher.model.monogram

class HeroBanner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val art: ImageView
    private val title: TextView
    private val meta: TextView
    private val kicker: TextView
    private val chip: TextView
    private val shimmer: ShimmerView
    val play: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_hero, this, true)
        art = findViewById(R.id.hero_art)
        title = findViewById(R.id.hero_title)
        meta = findViewById(R.id.hero_meta)
        kicker = findViewById(R.id.hero_kicker)
        chip = findViewById(R.id.hero_chip)
        shimmer = findViewById(R.id.hero_shimmer)
        play = findViewById(R.id.hero_play)
        val radius = resources.getDimension(R.dimen.radius_20)
        applyRoundOutline(radius, clip = true)
        setBackgroundColor(0xFF1C1917.toInt())
    }

    fun showLoading() {
        shimmer.setRunning(true)
        play.visibility = GONE
        kicker.visibility = GONE
        chip.visibility = GONE
        title.text = ""
        meta.text = ""
    }

    fun bind(name: String, metaText: String, kickerText: String?, contentDescription: String) {
        shimmer.setRunning(false)
        title.text = name
        meta.text = metaText
        play.visibility = VISIBLE
        play.contentDescription = contentDescription
        if (kickerText.isNullOrBlank()) {
            kicker.visibility = GONE
        } else {
            kicker.visibility = VISIBLE
            kicker.text = kickerText
        }
    }

    fun setAvailable(label: String?) {
        if (label.isNullOrBlank()) {
            chip.visibility = GONE
        } else {
            chip.visibility = VISIBLE
            chip.text = context.getString(R.string.available_on, label)
        }
    }

    fun setArt(bitmap: Bitmap?) {
        if (bitmap == null) {
            art.setImageDrawable(null)
            return
        }
        art.alpha = 0f
        art.setImageBitmap(bitmap)
        art.animate().alpha(1f).setDuration(200L).start()
    }
}

class PosterCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val art: ImageView
    private val letters: TextView
    private val title: TextView
    private val badge: TextView
    private val shimmer: ShimmerView

    init {
        LayoutInflater.from(context).inflate(R.layout.item_poster, this, true)
        art = findViewById(R.id.poster_art)
        letters = findViewById(R.id.poster_letters)
        title = findViewById(R.id.poster_title)
        badge = findViewById(R.id.poster_badge)
        shimmer = findViewById(R.id.poster_shimmer)
        foreground = context.getDrawable(R.drawable.focus_poster)
        val radius = resources.getDimension(R.dimen.radius_12)
        applyRoundOutline(radius, clip = true)
        setBackgroundColor(0xFFE3DDD4.toInt())
        isClickable = true
        isFocusable = true
    }

    fun showLoading() {
        shimmer.setRunning(true)
        title.text = ""
        badge.visibility = GONE
        letters.visibility = GONE
        isFocusable = false
    }

    fun bind(name: String, description: String) {
        shimmer.setRunning(false)
        title.text = name
        contentDescription = description
        letters.text = monogram(name)
        letters.visibility = VISIBLE
        isFocusable = true
        isClickable = true
    }

    fun setArt(bitmap: Bitmap?) {
        if (bitmap == null) return
        letters.visibility = GONE
        art.alpha = 0f
        art.setImageBitmap(bitmap)
        art.animate().alpha(1f).setDuration(200L).start()
    }

    fun setBadge(text: String?) {
        if (text.isNullOrBlank()) {
            badge.visibility = GONE
        } else {
            badge.visibility = VISIBLE
            badge.text = text
        }
    }
}

class ChannelCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val nameView: TextView
    private val hintView: TextView
    private val live: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.item_channel, this, true)
        nameView = findViewById(R.id.channel_name)
        hintView = findViewById(R.id.channel_hint)
        live = findViewById(R.id.channel_live)
        foreground = context.getDrawable(R.drawable.focus_card)
        applyRoundOutline(resources.getDimension(R.dimen.radius_20), clip = true)
        isClickable = true
        isFocusable = true
    }

    fun bind(name: String, hint: String, dark: Boolean, showLive: Boolean) {
        nameView.text = name
        hintView.text = hint
        contentDescription = if (hint.isBlank()) name else "$name, $hint"
        live.visibility = if (showLive) VISIBLE else GONE
        setBackgroundResource(if (dark) R.drawable.bg_channel else R.drawable.bg_card)
        val ink = context.getColor(if (dark) R.color.white else R.color.ink)
        val muted = if (dark) 0xFFD6D1C8.toInt() else context.getColor(R.color.muted)
        nameView.setTextColor(ink)
        hintView.setTextColor(muted)
    }
}

class InputCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val nameView: TextView
    private val portView: TextView
    private val statusView: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.item_input_card, this, true)
        nameView = findViewById(R.id.input_name)
        portView = findViewById(R.id.input_port)
        statusView = findViewById(R.id.input_status)
        setBackgroundResource(R.drawable.bg_card)
        foreground = context.getDrawable(R.drawable.focus_card)
        applyRoundOutline(resources.getDimension(R.dimen.radius_20), clip = true)
        isClickable = true
        isFocusable = true
    }

    fun bind(name: String, subtitle: String?, status: String?) {
        nameView.text = name
        if (subtitle.isNullOrBlank()) {
            portView.visibility = GONE
        } else {
            portView.visibility = VISIBLE
            portView.text = subtitle
        }
        if (status.isNullOrBlank()) {
            statusView.visibility = GONE
        } else {
            statusView.visibility = VISIBLE
            statusView.text = status
            statusView.setTextColor(
                context.getColor(if (status == "conectado") R.color.accent else R.color.muted),
            )
        }
        contentDescription = listOfNotNull(name, subtitle, status).joinToString(", ")
    }
}

class ShortcutTile @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val icon: ImageView
    private val label: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.item_shortcut, this, true)
        icon = findViewById(R.id.shortcut_icon)
        label = findViewById(R.id.shortcut_label)
        setBackgroundResource(R.drawable.bg_card)
        foreground = context.getDrawable(R.drawable.focus_card)
        applyRoundOutline(resources.getDimension(R.dimen.radius_20), clip = true)
        isClickable = true
        isFocusable = true
    }

    fun bind(iconRes: Int, text: String) {
        icon.setImageResource(iconRes)
        label.text = text
        contentDescription = text
    }
}
