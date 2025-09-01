package com.example.storemanagerassitent.data.api

/**
 * Simple in-memory cache for barcode -> (name, spec) to avoid repeated API calls
 * for the same barcode when the product is not stored locally yet.
 */
object BarcodeCache {
    data class Entry(val name: String, val spec: String, val timestampMs: Long)

    private const val MAX_ENTRIES: Int = 200
    private const val TTL_MS: Long = 5 * 60 * 1000L // 5 minutes

    // LRU by access order
    private val cache: MutableMap<String, Entry> = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean {
            return size > MAX_ENTRIES
        }
    }

    @Synchronized
    fun get(barcode: String): Entry? {
        val e = cache[barcode] ?: return null
        return if (System.currentTimeMillis() - e.timestampMs <= TTL_MS) e else {
            cache.remove(barcode)
            null
        }
    }

    @Synchronized
    fun put(barcode: String, name: String, spec: String) {
        cache[barcode] = Entry(name = name, spec = spec, timestampMs = System.currentTimeMillis())
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }
}




