package dev.enzotraini.tvlauncher.net

import dev.enzotraini.tvlauncher.model.HttpGet
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object UrlHttp : HttpGet {
    override fun get(url: String): String {
        if (!url.startsWith("https://")) throw IOException("esquema recusado")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "ParaVoce/0.2")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val landed = connection.url?.toString().orEmpty()
            if (landed.startsWith("http://")) throw IOException("redirecionou para http")
            if (code !in 200..299) throw IOException("HTTP $code")
            return body
        } finally {
            connection.disconnect()
        }
    }
}
