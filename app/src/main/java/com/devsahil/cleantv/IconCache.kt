package com.devsahil.cleantv

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap

/** In-memory cache. Icons are decoded lazily, off the main thread, at 160px square. */
object IconCache {
    private val cache = LruCache<String, Bitmap>(64)

    fun get(ctx: Context, pkg: String): Bitmap? {
        cache.get(pkg)?.let { return it }
        return try {
            val bmp = ctx.packageManager.getApplicationIcon(pkg).toBitmap(160, 160)
            cache.put(pkg, bmp)
            bmp
        } catch (e: Exception) {
            null
        }
    }
}
