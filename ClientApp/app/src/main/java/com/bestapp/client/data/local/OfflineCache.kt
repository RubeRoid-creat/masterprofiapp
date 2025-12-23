package com.bestapp.client.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

/**
 * Простой offline кэш для хранения данных
 */
class OfflineCache(private val context: Context) {
    
    private val cacheDir = File(context.cacheDir, "offline_data")
    private val json = Json { ignoreUnknownKeys = true }

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    /**
     * Сохранить данные в кэш
     */
    suspend fun <T> put(key: String, data: T, serializer: (T) -> String) = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "$key.json")
            val jsonData = serializer(data)
            file.writeText(jsonData)
            true
        } catch (e: Exception) {
            android.util.Log.e("OfflineCache", "Error saving to cache: ${e.message}")
            false
        }
    }

    /**
     * Получить данные из кэша
     */
    suspend fun <T> get(key: String, deserializer: (String) -> T): T? = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "$key.json")
            if (file.exists()) {
                val jsonData = file.readText()
                deserializer(jsonData)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("OfflineCache", "Error reading from cache: ${e.message}")
            null
        }
    }

    /**
     * Удалить данные из кэша
     */
    suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "$key.json")
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("OfflineCache", "Error removing from cache: ${e.message}")
            false
        }
    }

    /**
     * Очистить весь кэш
     */
    suspend fun clear() = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            android.util.Log.e("OfflineCache", "Error clearing cache: ${e.message}")
        }
    }

    /**
     * Проверить, есть ли данные в кэше
     */
    suspend fun has(key: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(cacheDir, "$key.json")
        file.exists()
    }

    /**
     * Получить время последнего обновления кэша
     */
    suspend fun getLastModified(key: String): Long = withContext(Dispatchers.IO) {
        val file = File(cacheDir, "$key.json")
        if (file.exists()) file.lastModified() else 0L
    }

    /**
     * Проверить, устарел ли кэш
     */
    suspend fun isExpired(key: String, maxAgeMillis: Long): Boolean = withContext(Dispatchers.IO) {
        val lastModified = getLastModified(key)
        if (lastModified == 0L) return@withContext true
        
        val now = System.currentTimeMillis()
        (now - lastModified) > maxAgeMillis
    }
}

/**
 * Extension функции для упрощения работы с кэшем
 */
suspend inline fun <reified T> OfflineCache.putJson(key: String, data: T): Boolean {
    return put(key, data) { Json.encodeToString(it) }
}

suspend inline fun <reified T> OfflineCache.getJson(key: String): T? {
    return get(key) { Json.decodeFromString<T>(it) }
}
