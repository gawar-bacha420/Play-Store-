package com.ultimatecloner.safe.Feedback

import android.content.Context
import android.os.Build
import com.ultimatecloner.safe.Utils.EncryptionUtils
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class FeedbackManager(private val context: Context) {
    
    // CHANGE THIS to your server URL
    private val API_URL = "https://your-server.com/api/v1/feedback"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    fun submitFeedback(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val payload = JSONObject().apply {
                    put("message", message)
                    put("device", Build.MODEL)
                    put("android", Build.VERSION.RELEASE)
                    put("app_version", context.packageManager.getPackageInfo(
                        context.packageName, 0
                    ).versionName)
                    put("timestamp", System.currentTimeMillis())
                }
                
                val request = Request.Builder()
                    .url(API_URL)
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // Save for retry
                        val dir = java.io.File(context.filesDir, "feedback_queue")
                        dir.mkdirs()
                        java.io.File(dir, "fb_${System.currentTimeMillis()}.json")
                            .writeText(payload.toString())
                    }
                    override fun onResponse(call: Call, response: Response) {
                        response.close()
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
