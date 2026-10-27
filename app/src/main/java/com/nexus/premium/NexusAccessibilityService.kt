package com.nexus.premium

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import org.json.JSONObject

class NexusAccessibilityService : AccessibilityService() {
    companion object { var instance: NexusAccessibilityService? = null }

    override fun onServiceConnected() { super.onServiceConnected(); instance = this }

    fun scanScreenText(): String {
        val rootNode = rootInActiveWindow ?: return ""
        val sb = StringBuilder()
        extractText(rootNode, sb)
        val fullText = sb.toString()
        val regex = Regex("(?i)(Câu|Bài|Question)\\s*1[\\s\\.\\:]")
        val match = regex.find(fullText)
        return if (match != null) fullText.substring(match.range.first) else fullText
    }

    private fun extractText(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null) return
        if (node.text != null) sb.append(node.text).append("\n")
        for (i in 0 until node.childCount) extractText(node.getChild(i), sb)
    }

    fun processAllQuestions(apiKeys: List<String>, fullText: String) {
        NexusEvents.onStatus?.invoke("ĐANG CHIA LÔ...")
        val regex = Regex("(?i)(Câu|Bài|Question)\\s*\\d+")
        val matches = regex.findAll(fullText).toList()
        if (matches.isEmpty()) { NexusEvents.onStatus?.invoke("KHÔNG THẤY ĐỀ"); return }

        val batchSize = 5 
        val batches = mutableListOf<String>()
        for (i in matches.indices step batchSize) {
            val start = matches[i].range.first
            val end = if (i + batchSize < matches.size) matches[i + batchSize].range.first else fullText.length
            batches.add(fullText.substring(start, end))
        }

        NexusEvents.onStatus?.invoke("ĐANG GIẢI ${batches.size} LÔ...")

        CoroutineScope(Dispatchers.Main).launch {
            val results = mutableListOf<String?>()
            coroutineScope {
                val tasks = batches.map { batchText ->
                    async(Dispatchers.IO) { ApiClient.callBatch(apiKeys, batchText, "openai/gpt-oss-120b") }
                }
                tasks.forEach { results.add(it.await()) }
            }

            val mergedResult = JSONObject()
            var successCount = 0
            results.forEach { jsonString ->
                if (jsonString != null && jsonString.contains("{")) {
                    try {
                        val json = JSONObject(jsonString)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            mergedResult.put(key, json.getString(key))
                            successCount++
                        }
                    } catch (e: Exception) { }
                }
            }

            if (successCount > 0) {
                NexusEvents.onStatus?.invoke("XONG $successCount CÂU")
                NexusEvents.onResult?.invoke(mergedResult.toString())
                val keys = mergedResult.keys()
                while (keys.hasNext()) {
                    val ans = mergedResult.getString(keys.next())
                    if (ans.length == 1 && ans.matches(Regex("[A-D]"))) autoClickAnswer(ans)
                }
            } else { NexusEvents.onStatus?.invoke("LỖI API") }
        }
    }

    fun autoClickAnswer(answer: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return findAndClick(rootNode, answer)
    }

    private fun findAndClick(node: AccessibilityNodeInfo?, answer: String): Boolean {
        if (node == null) return false
        val text = node.text?.toString()?.trim()?.uppercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.trim()?.uppercase() ?: ""
        
        if (text == answer || text.startsWith("$answer.") || text.startsWith("$answer)") || contentDesc == answer) {
            var clickNode: AccessibilityNodeInfo? = node
            while (clickNode != null) {
                if (clickNode.isClickable) { clickNode.performAction(AccessibilityNodeInfo.ACTION_CLICK); return true }
                clickNode = clickNode.parent
            }
        }
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
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
