package com.nexus.premium

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import org.json.JSONObject

class NexusAccessibilityService : AccessibilityService() {
    companion object { var instance: NexusAccessibilityService? = null }

    override fun onServiceConnected() { super.onServiceConnected(); instance = this }

    // 1. HÀM QUÉT TEXT "TUYỆT ĐỐI" (Chỉ lấy câu hỏi hiện tại)
    fun scanCurrentQuestion(): String {
        val rootNode = rootInActiveWindow ?: return ""
        val sb = StringBuilder()
        extractText(rootNode, sb)
        
        var fullText = sb.toString().trim()
        
        // Tìm vị trí bắt đầu của câu hỏi hiện tại (VD: "1.", "Câu 1", "Bài 1")
        // Regex tìm số ở đầu dòng (có thể có chữ Câu/Question phía trước)
        val match = Regex("(?m)(?:Câu|Bài|Question)?\\s*\\b1\\b[\\.\\:\\s]").find(fullText)
        
        if (match != null) {
            // Cắt từ vị trí câu 1 đến hết (vì web thường chỉ hiển thị 1 câu)
            fullText = fullText.substring(match.range.first)
            
            // Cắt bỏ phần rác phía dưới (nếu có chữ "Câu 2" hoặc "Next")
            val endMatch = Regex("(?i)(Câu\\s*2|Bài\\s*2|Next|Tiếp theo)").find(fullText)
            if (endMatch != null) {
                fullText = fullText.substring(0, endMatch.range.first)
            }
        }
        return fullText.trim()
    }

    private fun extractText(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null) return
        if (node.text != null) sb.append(node.text).append("\n")
        for (i in 0 until node.childCount) extractText(node.getChild(i), sb)
    }

    fun solveCurrentQuestion(apiKeys: List<String>, autoNext: Boolean) {
        val screenText = scanCurrentQuestion()
        if (screenText.length < 10) { 
            NexusEvents.onStatus?.invoke("KHÔNG THẤY ĐỀ"); 
            return 
        }

        NexusEvents.onStatus?.invoke("ĐANG SUY LUẬN...")

        CoroutineScope(Dispatchers.Main).launch {
            val result = ApiClient.callBatch(apiKeys, screenText, "openai/gpt-oss-120b")
            
            if (result != null && result.contains("{")) {
                try {
                    val json = JSONObject(result)
                    val ans = json.optString("1", json.optString("answer", ""))
                    
                    if (ans.isNotEmpty() && ans != "?") {
                        NexusEvents.onResult?.invoke(ans)
                        NexusEvents.onStatus?.invoke("ĐANG CLICK $ans...")

                        // Click đáp án bằng Gesture
                        val clicked = clickAnswerByGesture(ans)
                        if (clicked) {
                            NexusEvents.onStatus?.invoke("ĐÃ CHỌN $ans")
                            if (autoNext) {
                                NexusEvents.onStatus?.invoke("CHỜ 1.5s ĐỂ NEXT...")
                                delay(1500)
                                val nextSuccess = clickNextButton()
                                if (nextSuccess) {
                                    NexusEvents.onStatus?.invoke("ĐÃ NEXT!")
                                } else {
                                    NexusEvents.onStatus?.invoke("KHÔNG TÌM THẤY NÚT NEXT")
                                }
                            }
                        } else {
                            NexusEvents.onStatus?.invoke("LỖI: KHÔNG THẤY NÚT $ans")
                        }
                    } else {
                        NexusEvents.onStatus?.invoke("AI KHÔNG CHẮC CHẮN")
                    }
                } catch (e: Exception) { 
                    NexusEvents.onStatus?.invoke("LỖI JSON") 
                }
            } else { 
                NexusEvents.onStatus?.invoke("LỖI API") 
            }
        }
    }

    // 2. HÀM CLICK BẰNG TỌA ĐỘ (GESTURE) - Vượt mọi rào cản React/Vue
    fun clickAnswerByGesture(answer: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val targetNode = findNodeByText(root, answer) ?: return false
        
        val rect = Rect()
        targetNode.getBoundsInScreen(rect)
        
        // Lấy tọa độ trung tâm của nút
        val x = rect.centerX().toFloat()
        val y = rect.centerY().toFloat()
        
        return performClickGesture(x, y)
    }

    private fun findNodeByText(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val nodeText = node.text?.toString()?.trim()?.uppercase() ?: ""
        val nodeDesc = node.contentDescription?.toString()?.trim()?.uppercase() ?: ""
        
        // Khớp chính xác A, A., A)
        if (nodeText == text || nodeText.startsWith("$text.") || nodeText.startsWith("$text)") ||
            nodeDesc == text || nodeDesc.startsWith("$text.")) {
            // Phải đảm bảo thẻ này có thể click được (hoặc cha của nó)
            var clickable = node
            while (clickable != null) {
                if (clickable.isClickable) return clickable
                clickable = clickable.parent
            }
        }
        
        for (i in 0 until node.childCount) {
            val found = findNodeByText(node.getChild(i), text)
            if (found != null) return found
        }
        return null
    }

    private fun performClickGesture(x: Float, y: Float): Boolean {
        val path = Path()
        path.moveTo(x, y)
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
            
        return dispatchGesture(gesture, null, null)
    }

    // 3. HÀM TỰ ĐỘNG NEXT (Đa năng + Tự cuộn trang)
    fun clickNextButton(): Boolean {
        val root = rootInActiveWindow ?: return false
        val keywords = listOf("Tiếp", "Next", "Tiếp theo", "Câu sau", "Next Question", ">", "→")
        
        // Thử tìm trực tiếp
        val node = findNextNode(root, keywords)
        if (node != null) {
            // Click bằng gesture cho chắc chắn
            val rect = Rect()
            node.getBoundsInScreen(rect)
            return performClickGesture(rect.centerX().toFloat(), rect.centerY().toFloat())
        }
        
        // Nếu không thấy, tự động cuộn xuống dưới (để tìm nút bị khuất)
        root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        
        // Chờ 500ms cho trang load rồi tìm lại
        Thread.sleep(500)
        val scrollNode = rootInActiveWindow?.let { findNextNode(it, keywords) }
        if (scrollNode != null) {
            val rect = Rect()
            scrollNode.getBoundsInScreen(rect)
            return performClickGesture(rect.centerX().toFloat(), rect.centerY().toFloat())
        }
        
        return false
    }

    private fun findNextNode(node: AccessibilityNodeInfo?, keywords: List<String>): AccessibilityNodeInfo? {
        if (node == null) return null
        val text = node.text?.toString()?.trim() ?: ""
        val desc = node.contentDescription?.toString()?.trim() ?: ""
        
        for (kw in keywords) {
            if (text.equals(kw, true) || desc.equals(kw, true) || text.contains(kw, true)) {
                var clickable = node
                while (clickable != null) {
                    if (clickable.isClickable) return clickable
                    clickable = clickable.parent
                }
            }
        }
        for (i in 0 until node.childCount) {
            val found = findNextNode(node.getChild(i), keywords)
            if (found != null) return found
        }
        return null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
