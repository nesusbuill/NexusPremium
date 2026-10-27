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

    // ÁP DỤNG 10 KỸ NĂNG NGHIÊN CỨU & ĐÀO SÂU
    private val SYSTEM_PROMPT = """
        Bạn là NEXUS PRIME - Hội đồng 5 chuyên gia.
        Nhiệm vụ: Giải CHÍNH XÁC TUYỆT ĐỐI các câu hỏi trắc nghiệm.
        
        BẮT BUỘC SUY LUẬN NGẦM 5 BƯỚC (KHÔNG IN RA):
        1. /extract & /digest: Lọc bỏ các từ rác của trang web (như "1/14", "Lịch sử", "Ôn tập"). Trích xuất chính xác nội dung câu hỏi và 4 đáp án.
        2. /investigate & FACTCHECK: Tìm bẫy chữ "KHÔNG", "NGOẠI TRỪ", "CHỈ". Kiểm chứng lại kiến thức.
        3. INVERT: Tìm lý do tại sao 3 đáp án kia SAI để loại trừ.
        4. COMPARE & XRAY: Đặt 4 đáp án cạnh nhau, tìm ra điểm khác biệt cốt lõi.
        5. OODA: Quan sát - Định hướng - Quyết định - Hành động. Chốt đáp án.

        YÊU CẦU ĐẦU RA:
        - Trả về 1 đối tượng JSON duy nhất.
        - Định dạng: {"1": "A", "2": "a-Đúng, b-Sai", "3": "Đúng"}
        - Key là SỐ THỨ TỰ CÂU. KHÔNG trả về "?". KHÔNG giải thích.
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
