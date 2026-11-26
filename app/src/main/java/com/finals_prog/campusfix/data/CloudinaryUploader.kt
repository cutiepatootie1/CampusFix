package com.finals_prog.campusfix.data

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Shared Cloudinary upload (unsigned preset, no API secret in the app). */
object CloudinaryUploader {

    // Cloudinary dashboard -> "Cloud name".
    private const val CLOUD_NAME = "kbqtj3ps"

    // Settings -> Upload -> Upload presets. Signing mode must be "Unsigned".
    private const val UPLOAD_PRESET = "kotlin_uploads"

    data class Result(val url: String, val publicId: String)

    /** Uploads [source] as an image into [folder] with the given [publicId]. Safe to call from any dispatcher. */
    suspend fun upload(
        resolver: ContentResolver,
        source: Uri,
        publicId: String,
        folder: String
    ): Result = withContext(Dispatchers.IO) {
        val boundary = "----CampusFix${UUID.randomUUID()}"
        val endpoint = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"
        val mime = resolver.getType(source) ?: "image/jpeg"

        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 60_000
            setChunkedStreamingMode(0) // stream the file instead of buffering it in memory
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        try {
            DataOutputStream(conn.outputStream).use { out ->
                fun field(name: String, value: String) {
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    out.write(value.toByteArray(Charsets.UTF_8))
                    out.writeBytes("\r\n")
                }

                field("upload_preset", UPLOAD_PRESET)
                field("folder", folder)
                field("public_id", publicId)

                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$publicId.jpg\"\r\n")
                out.writeBytes("Content-Type: $mime\r\n\r\n")
                resolver.openInputStream(source)?.use { it.copyTo(out) }
                    ?: throw IOException("Cannot read selected photo")
                out.writeBytes("\r\n--$boundary--\r\n")
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                val msg = runCatching {
                    JSONObject(body).getJSONObject("error").getString("message")
                }.getOrDefault("HTTP $code")
                throw IOException("Photo upload failed: $msg")
            }

            val json = JSONObject(body)
            Result(url = json.getString("secure_url"), publicId = json.getString("public_id"))
        } finally {
            conn.disconnect()
        }
    }
}