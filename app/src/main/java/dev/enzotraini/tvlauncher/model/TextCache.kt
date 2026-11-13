package dev.enzotraini.tvlauncher.model

import java.io.File

data class CacheEntry(
    val body: String,
    val savedAtMillis: Long,
)

interface TextCache {
    fun read(key: String): CacheEntry?

    fun write(key: String, body: String, savedAtMillis: Long)
}

class MemoryTextCache : TextCache {
    val entries = LinkedHashMap<String, CacheEntry>()

    override fun read(key: String): CacheEntry? = entries[key]

    override fun write(key: String, body: String, savedAtMillis: Long) {
        entries[key] = CacheEntry(body, savedAtMillis)
    }
}

/** Arquivos em disco. A chave da API não entra no nome do arquivo. */
class DiskTextCache(private val root: File) : TextCache {
    override fun read(key: String): CacheEntry? {
        val body = File(dir(key), "body.txt")
        val saved = File(dir(key), "saved-at.txt")
        if (!body.isFile || !saved.isFile) return null
        val at = saved.readText().trim().toLongOrNull() ?: return null
        return CacheEntry(body.readText(), at)
    }

    override fun write(key: String, body: String, savedAtMillis: Long) {
        val folder = dir(key)
        folder.mkdirs()
        File(folder, "body.txt").writeText(body)
        File(folder, "saved-at.txt").writeText(savedAtMillis.toString())
    }

    private fun dir(key: String): File {
        val safe = key.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(root, safe)
    }
}
