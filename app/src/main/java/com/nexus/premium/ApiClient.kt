package com.nexus.premium

import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val SYSTEM_PROMPT = """
        Bạn là NEXUS PRIME. Giải CHÍNH XÁC TUYỆT ĐỐI.
        ÁP DỤNG 10 KỸ NĂNG: OODA, /deepthink, L99, CHAINLOGIC, /blindspots, OVERTHINK, /unpack, INVERT, /layered, XRAY.
        TRẢ VỀ JSON: {"1": "A", "2": "a-Đúng, b-Sai", "3": "Đúng"}
        KHÔNG markdown, KHÔNG giải thích.
    """.trimIndent()

    suspend fun callBatch(apiKeys: List<String>, batchText: String, model: String): String? = withContext(Dispatchers.IO) {
        var keyIndex = 0
        var retry = 0
        while (retry < 3) {
            val currentKey = apiKeys[keyIndex % apiKeys.size]
            try {
                val jsonBody = """
                    {
                      "model": "$model",
                      "temperature": 0.0,
                      "top_p": 0.1,
                      "max_tokens": 4096,
                      "messages": [
                        {"role": "system", "content": "${SYSTEM_PROMPT.replace("\"", "\\\"").replace("\n", "\\n")}"},
                        {"role": "user", "content": "${batchText.replace("\"", "\\\"").replace("\n", "\\n")}"}
                      ]
                    }
                """.trimIndent()

                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $currentKey")
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val resBody = response.body?.string()
                    val json = JSONObject(resBody!!)
                    var content = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                    return@withContext content.replace("```json", "").replace("```", "").trim()
                } else if (response.code == 429) {
                    keyIndex++; delay(4000); retry++
                } else { break }
            } catch (e: Exception) { keyIndex++; delay(2000); retry++ }
        }
        return@withContext null
    }
}
