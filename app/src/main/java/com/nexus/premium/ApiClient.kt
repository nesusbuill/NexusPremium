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
        Bạn là NEXUS PRIME - Hội đồng 5 chuyên gia.
        Nhiệm vụ: Giải CHÍNH XÁC TUYỆT ĐỐI câu hỏi trắc nghiệm sau.
        
        BẮT BUỘC ÁP DỤNG 10 KỸ NĂNG NGHIÊN CỨU & ĐÀO SÂU (TỰ SUY LUẬN NGẦM):
        1. /extract & /digest: Lọc bỏ từ rác của web (Menu, 1/14...). Chỉ lấy câu hỏi và đáp án.
        2. /investigate: Điều tra bẫy chữ "KHÔNG", "NGOẠI TRỪ", "CHỈ".
        3. /deepdive: Đào sâu ngữ cảnh câu hỏi.
        4. COMPARE: Đặt 4 đáp án cạnh nhau, so sánh từng cặp.
        5. FACTCHECK: Kiểm chứng lại kiến thức, sự kiện.
        6. /sources: Đảm bảo kiến thức có nguồn gốc vững chắc.
        7. TIMELINE: Nếu là câu hỏi lịch sử, hãy sắp xếp dòng thời gian.
        8. /deepdive & INVERT: Tìm lý do tại sao 3 đáp án kia SAI.
        9. XRAY: Nhìn xuyên qua đáp án gây nhiễu.
        10. DOSSIER & OODA: Chốt đáp án đúng nhất.

        YÊU CẦU ĐẦU RA:
        - Chỉ trả về 1 đối tượng JSON duy nhất. KHÔNG markdown, KHÔNG giải thích.
        - Định dạng: {"1": "A"} (Nếu web đánh số 1) hoặc {"answer": "A"}.
        - KHÔNG trả về "?".
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
