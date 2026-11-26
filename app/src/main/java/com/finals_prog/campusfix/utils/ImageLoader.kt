package com.finals_prog.campusfix.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Tiny image loader with an in-memory cache, safe for RecyclerView rows.
 * Cloudinary URLs are resized server-side. Glide/Coil can replace this if you add either library.
 */
object ImageLoader {

    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun load(view: ImageView, url: String?, widthPx: Int = 300) {
        if (url.isNullOrBlank()) {
            view.tag = null
            view.setImageDrawable(null)
            return
        }

        val sized = if (url.contains("res.cloudinary.com") && url.contains("/upload/")) {
            url.replace("/upload/", "/upload/w_$widthPx,c_fill,q_auto/")
        } else url

        view.tag = sized // lets us ignore results for rows that were recycled meanwhile
        cache.get(sized)?.let {
            view.setImageBitmap(it)
            return
        }

        view.setImageDrawable(null)
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching { URL(sized).openStream().use { BitmapFactory.decodeStream(it) } }.getOrNull()
            }
            if (bitmap != null) {
                cache.put(sized, bitmap)
                if (view.tag == sized) view.setImageBitmap(bitmap)
            }
        }
    }
}