package com.nexus.premium

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import org.json.JSONObject

class NexusAccessibilityService : AccessibilityService() {
    companion object { var instance: NexusAccessibilityService? = null }

    override fun onServiceConnected() { super.onServiceConnected(); instance = this }

    // Hàm quét CHỈ LẤY CÂU HỎI ĐANG HIỂN THỊ
    fun scanCurrentQuestion(): String {
        val rootNode = rootInActiveWindow ?: return ""
        val sb = StringBuilder()
        extractText(rootNode, sb)
        return sb.toString().trim()
    }

    private fun extractText(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null) return
        if (node.text != null) sb.append(node.text).append("\n")
        for (i in 0 until node.childCount) extractText(node.getChild(i), sb)
    }

    fun solveCurrentQuestion(apiKeys: List<String>, autoNext: Boolean) {
        val screenText = scanCurrentQuestion()
        if (screenText.length < 20) { 
            NexusEvents.onStatus?.invoke("KHÔNG THẤY ĐỀ"); 
            return 
        }

        NexusEvents.onStatus?.invoke("ĐANG PHÂN TÍCH...")

        CoroutineScope(Dispatchers.Main).launch {
            val result = ApiClient.callBatch(apiKeys, screenText, "openai/gpt-oss-120b")
            
            if (result != null && result.contains("{")) {
                try {
                    val json = JSONObject(result)
                    val firstKey = json.keys().next()
                    val ans = json.getString(firstKey)
                    
                    NexusEvents.onStatus?.invoke("ĐÁP ÁN: $ans")
                    NexusEvents.onResult?.invoke(ans)

                    // Click đáp án
                    val clicked = autoClickAnswer(ans)
                    if (clicked && autoNext) {
                        NexusEvents.onStatus?.invoke("ĐÃ CHỌN $ans. ĐANG NEXT...")
                        delay(1000) // Chờ 1 giây cho web load
                        clickNextButton()
                    } else if (!clicked) {
                        NexusEvents.onStatus?.invoke("KHÔNG TÌM THẤY NÚT CHỌN")
                    }
                } catch (e: Exception) { 
                    NexusEvents.onStatus?.invoke("LỖI JSON") 
                }
            } else { 
                NexusEvents.onStatus?.invoke("LỖI API") 
            }
        }
    }

    // Thuật toán Click thông minh (Tìm thẻ cha, thẻ anh em)
    fun autoClickAnswer(answer: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return findAndClick(rootNode, answer)
    }

    private fun findAndClick(node: AccessibilityNodeInfo?, answer: String): Boolean {
        if (node == null) return false
        val text = node.text?.toString()?.trim()?.uppercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.trim()?.uppercase() ?: ""
        
        // Tìm chính xác hoặc bắt đầu bằng "A.", "A)"
        if (text == answer || text.startsWith("$answer.") || text.startsWith("$answer)") || contentDesc == answer) {
            var clickNode: AccessibilityNodeInfo? = node
            while (clickNode != null) {
                if (clickNode.isClickable) { clickNode.performAction(AccessibilityNodeInfo.ACTION_CLICK); return true }
                clickNode = clickNode.parent
            }
        }
        
        // Nếu không click được, thử tìm thẻ input radio bên cạnh
        val parent = node.parent
        if (parent != null) {
            for (i in 0 until parent.childCount) {
                val sibling = parent.getChild(i)
                if (sibling != null && sibling.isClickable && (sibling.text?.toString()?.contains(answer, true) == true)) {
                    sibling.performAction(AccessibilityNodeInfo.ACTION_CLICK); return true
                }
            }
        }

        for (i in 0 until node.childCount) { if (findAndClick(node.getChild(i), answer)) return true }
        return false
    }

    // Tự động tìm và click nút Next
    fun clickNextButton(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nextKeywords = listOf("Tiếp", "Next", "Tiếp theo", "Câu tiếp", ">", "Next Question")
        return findAndClickByText(rootNode, nextKeywords)
    }

    private fun findAndClickByText(node: AccessibilityNodeInfo?, keywords: List<String>): Boolean {
        if (node == null) return false
        val text = node.text?.toString()?.trim() ?: ""
        val desc = node.contentDescription?.toString()?.trim() ?: ""
        
        for (kw in keywords) {
            if (text.equals(kw, true) || desc.equals(kw, true) || text.contains(kw, true)) {
                var clickNode: AccessibilityNodeInfo? = node
                while (clickNode != null) {
                    if (clickNode.isClickable) { clickNode.performAction(AccessibilityNodeInfo.ACTION_CLICK); return true }
                    clickNode = clickNode.parent
                }
            }
        }
        for (i in 0 until node.childCount) { if (findAndClickByText(node.getChild(i), keywords)) return true }
        return false
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
