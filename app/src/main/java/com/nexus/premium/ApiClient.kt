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
        Bạn là NEXUS PRIME - Giáo sư chấm thi xuất sắc.
        Nhiệm vụ: Giải câu hỏi trắc nghiệm dưới đây.
        
        YÊU CẦU ĐẦU RA:
        - Chỉ trả về 1 đối tượng JSON duy nhất. KHÔNG kèm văn bản khác.
        - Định dạng: {"answer": "A"} (A, B, C hoặc D).
        - Tuyệt đối không trả về "?". Nếu không chắc, hãy chọn đáp án khả thi nhất.
        
        QUY TRÌNH TƯ DUY BẮT BUỘC (TỰ SUY LUẬN NGẦM):
        - /extract: Đọc kỹ câu hỏi, gạt bỏ các thông tin nhiễu (số thứ tự câu, menu web).
        - FACTCHECK: Cảnh giác bẫy chữ "KHÔNG", "NGOẠI TRỪ".
        - INVERT: Tại sao 3 đáp án còn lại SAI?
        - OODA: Chốt đáp án đúng nhất.
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
                      "max_tokens": 100,
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
