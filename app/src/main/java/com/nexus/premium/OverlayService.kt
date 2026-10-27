package com.nexus.premium

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.TextView
import org.json.JSONObject

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isViewAdded = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onCreate() {
        super.onCreate()
        startForegroundSafely()

        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            floatingView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
            
            val tvStatus = floatingView!!.findViewById<TextView>(R.id.tvStatus)
            val tvResult = floatingView!!.findViewById<TextView>(R.id.tvResult)
            val btnSolve = floatingView!!.findViewById<Button>(R.id.btnSolve)
            val btnClose = floatingView!!.findViewById<Button>(R.id.btnClose)

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.START; x = 100; y = 200 }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
                stopSelf(); return
            }

            windowManager?.addView(floatingView, params)
            isViewAdded = true

            floatingView!!.setOnTouchListener(object : View.OnTouchListener {
                var initialX = 0; var initialY = 0; var initialTouchX = 0f; var initialTouchY = 0f
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> { initialX = params.x; initialY = params.y; initialTouchX = event.rawX; initialTouchY = event.rawY }
                        MotionEvent.ACTION_MOVE -> {
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager?.updateViewLayout(floatingView, params)
                        }
                    }
                    return false
                }
            })

            btnSolve.setOnClickListener {
                val accessibility = NexusAccessibilityService.instance
                if (accessibility == null) { tvStatus.text = "BẬT TRỢ NĂNG!"; return@setOnClickListener }
                val screenText = accessibility.scanScreenText()
                if (screenText.length < 20) { tvStatus.text = "KHÔNG THẤY ĐỀ"; return@setOnClickListener }
                tvResult.text = "⏳"
                
                val key1 = getSharedPreferences("nexus", MODE_PRIVATE).getString("groqKey1", "") ?: ""
                val key2 = getSharedPreferences("nexus", MODE_PRIVATE).getString("groqKey2", "") ?: ""
                val key3 = getSharedPreferences("nexus", MODE_PRIVATE).getString("groqKey3", "") ?: ""
                val apiKeys = listOf(key1, key2, key3).filter { it.isNotBlank() }
                
                if (apiKeys.isEmpty()) { tvStatus.text = "CHƯA NHẬP KEY!"; return@setOnClickListener }
                accessibility.processAllQuestions(apiKeys, screenText)
            }

            btnClose.setOnClickListener { stopSelf() }

            NexusEvents.onStatus = { msg -> tvStatus.text = msg }
            NexusEvents.onResult = { res ->
                if (res.startsWith("{")) {
                    try {
                        val json = JSONObject(res)
                        val firstKey = json.keys().next()
                        tvResult.text = json.getString(firstKey)
                    } catch (e: Exception) { tvResult.text = "LỖI" }
                } else { tvResult.text = res }
            }
        } catch (e: Exception) { stopSelf() }
    }

    private fun startForegroundSafely() {
        val channelId = "nexus_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Nexus Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("NEXUS ĐANG CHẠY").setSmallIcon(android.R.drawable.ic_menu_camera).build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {}
    }

    override fun onDestroy() { 
        super.onDestroy(); 
        if (isViewAdded && floatingView != null) { try { windowManager?.removeView(floatingView) } catch (e: Exception) {} }
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
