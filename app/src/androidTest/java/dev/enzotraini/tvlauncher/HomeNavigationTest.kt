package dev.enzotraini.tvlauncher

import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import dev.enzotraini.tvlauncher.model.HttpGet
import dev.enzotraini.tvlauncher.model.MemoryTextCache
import dev.enzotraini.tvlauncher.model.TmdbRepository
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class HomeNavigationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)

    @Before
    fun fixture() {
        val context = instrumentation.targetContext
        File(context.filesDir, "wallpaper-pref.txt").writeText("idle=180\n")
        val app = context.applicationContext as App
        app.tmdbForTest = TmdbRepository(
            apiKey = "fixture",
            http = FixtureHttp,
            cache = MemoryTextCache(),
            now = { 0L },
        )
    }

    @After
    fun close() {
        val app = instrumentation.targetContext.applicationContext as App
        app.tmdbForTest = null
    }

    @Test
    fun dpadCrossesHeroPostersDockAndRestoresThePoster() {
        val launch = Intent(instrumentation.targetContext, HomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        instrumentation.startActivitySync(launch)
        assertTrue("hero Alfa; tela='${screenText()}'", waitFor("Titulo Alfa"))
        assertTrue("foco no hero, era '${focused()}'", focused().contains("Alfa"))
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertTrue("direita troca o título, era '${focused()}'", waitFor("Titulo Beta") && focused().contains("Beta"))
        shot("nav-02-hero-direita")
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        pause()
        assertTrue("esquerda volta ao título, era '${focused()}'", focused().contains("Alfa"))
        shot("nav-01-hero")

        key(KeyEvent.KEYCODE_DPAD_DOWN)
        assertTrue("baixo vai ao poster, era '${focused()}'", focused().contains("Gama"))
        shot("nav-03-poster")

        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertTrue("direita no poster, era '${focused()}'", focused().contains("Delta"))
        shot("nav-04-poster-direita")

        key(KeyEvent.KEYCODE_DPAD_DOWN)
        assertTrue("baixo chega ao dock, era '${focused()}'", !focused().contains("Titulo"))
        shot("nav-05-dock")

        key(KeyEvent.KEYCODE_DPAD_UP)
        assertTrue("cima volta ao poster lembrado, era '${focused()}'", focused().contains("Delta"))
        shot("nav-06-volta-poster")

        key(KeyEvent.KEYCODE_DPAD_CENTER)
        val opened = waitFor("Resumo delta")
        assertTrue("ok abre o detalhe; tela='${screenText()}'", opened)
        shot("nav-07-detalhe")

        key(KeyEvent.KEYCODE_BACK)
        assertTrue("voltar restaura o poster, era '${focused()}'", focused().contains("Delta"))
        shot("nav-08-volta-do-detalhe")

        key(KeyEvent.KEYCODE_DPAD_UP)
        key(KeyEvent.KEYCODE_DPAD_UP)
        assertTrue("cima chega na aba, era '${focused()}'", focused().contains("Explorar"))
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertTrue("aba Filmes; tela='${screenText()}'", waitFor("Titulo Gama"))
        shot("nav-09-filmes")

        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertTrue("aba Ao vivo; tela='${screenText()}'", waitFor("Globo"))
        shot("nav-10-aovivo")
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        shot("nav-11-canal")

        repeat(4) {
            key(KeyEvent.KEYCODE_DPAD_DOWN)
        }
        moveUntil("Todos os aplicativos", right = true)
        shot("nav-12-dock-todos")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        val apps = waitFor("Todos os aplicativos")
        assertTrue("todos abre a grade; tela='${screenText()}'", apps)
        shot("nav-13-apps")
    }

    private fun moveUntil(label: String, right: Boolean) {
        repeat(8) {
            if (focused().contains(label)) return
            key(if (right) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT)
        }
        assertTrue(label, focused().contains(label))
    }

    private fun screenText(): String {
        return try {
            device.findObjects(By.enabled(true))
                .take(12)
                .joinToString(" | ") { view ->
                    try {
                        view.text?.takeIf { it.isNotBlank() } ?: view.contentDescription.orEmpty()
                    } catch (_: RuntimeException) {
                        ""
                    }
                }
        } catch (_: RuntimeException) {
            ""
        }
    }

    private fun focused(): String {
        return try {
            val view = device.findObject(By.focused(true)) ?: return ""
            "${view.text.orEmpty()} ${view.contentDescription.orEmpty()}"
        } catch (_: RuntimeException) {
            ""
        }
    }

    private fun waitFor(text: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + 20_000L
        while (SystemClock.uptimeMillis() < deadline) {
            if (device.hasObject(By.textContains(text))) return true
            SystemClock.sleep(200)
        }
        return false
    }

    private fun key(code: Int) {
        instrumentation.sendKeyDownUpSync(code)
        pause()
    }

    private fun pause() {
        SystemClock.sleep(900)
    }

    private fun shot(name: String) {
        val dir = instrumentation.targetContext.getExternalFilesDir(null) ?: return
        device.takeScreenshot(File(dir, "$name.png"))
    }

    private object FixtureHttp : HttpGet {
        override fun get(url: String): String = when {
            url.contains("/movie/101") -> detail("101", "Titulo Alfa", "Resumo alfa", movie = true)
            url.contains("/movie/103") -> detail("103", "Titulo Gama", "Resumo gama", movie = true)
            url.contains("/tv/102") -> detail("102", "Titulo Beta", "Resumo beta", movie = false)
            url.contains("/tv/104") -> detail("104", "Titulo Delta", "Resumo delta", movie = false)
            url.contains("/trending/") -> list(
                item("101", "Titulo Alfa", movie = true, popularity = 30),
                item("102", "Titulo Beta", movie = false, popularity = 20),
            )
            url.contains("/discover/movie") -> list(item("103", "Titulo Gama", movie = true, popularity = 12))
            url.contains("/discover/tv") -> list(item("104", "Titulo Delta", movie = false, popularity = 9))
            url.contains("/movie/popular") -> list(item("103", "Titulo Gama", movie = true, popularity = 12))
            url.contains("/tv/popular") -> list(item("104", "Titulo Delta", movie = false, popularity = 9))
            else -> """{"results":[]}"""
        }

        private fun list(vararg rows: String) = """{"results":[${rows.joinToString(",")}]}"""

        private fun item(id: String, title: String, movie: Boolean, popularity: Int): String {
            val named = if (movie) """"title":"$title","release_date":"2026-11-10"""" else """"name":"$title","first_air_date":"2026-11-11""""
            val type = if (movie) "movie" else "tv"
            return """{"id":$id,"media_type":"$type",$named,"vote_average":7.1,"overview":"Resumo","popularity":$popularity}"""
        }

        private fun detail(id: String, title: String, overview: String, movie: Boolean): String {
            val named = if (movie) """"title":"$title","release_date":"2026-11-10"""" else """"name":"$title","first_air_date":"2026-11-11""""
            return """{"id":$id,$named,"overview":"$overview","vote_average":7.1}"""
        }
    }
}
