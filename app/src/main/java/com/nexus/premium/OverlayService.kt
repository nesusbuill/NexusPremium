package com.nexus.premium

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isViewAdded = false
    private var isDragging = false

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
            val btnFab = floatingView!!.findViewById<Button>(R.id.btnFab)
            val panelView = floatingView!!.findViewById<LinearLayout>(R.id.panelView)
            val btnClosePanel = floatingView!!.findViewById<Button>(R.id.btnClosePanel)
            val btnNext = floatingView!!.findViewById<Button>(R.id.btnNext)
            val switchAutoNext = floatingView!!.findViewById<Switch>(R.id.switchAutoNext)

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

            // Logic kéo thả mượt (Chống giật)
            val dragListener = View.OnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) { // Ngưỡng kéo để tránh nhầm với click
                            isDragging = true
                            params.x = initialX + dx.toInt()
                            params.y = initialY + dy.toInt()
                            if (params.x < 0) params.x = 0
                            if (params.y < 0) params.y = 0
                            windowManager?.updateViewLayout(floatingView, params)
                            true
                        } else false
                    }
                    else -> false
                }
            }

            btnFab.setOnTouchListener(dragListener)
            btnFab.setOnClickListener {
                if (!isDragging) {
                    btnFab.visibility = View.GONE
                    panelView.visibility = View.VISIBLE
                }
            }

            btnClosePanel.setOnClickListener {
                panelView.visibility = View.GONE
                btnFab.visibility = View.VISIBLE
            }

            btnNext.setOnClickListener {
                val acc = NexusAccessibilityService.instance
                if (acc != null) {
                    tvStatus.text = "ĐANG NEXT..."
                    val success = acc.clickNextButton()
                    if (!success) tvStatus.text = "KHÔNG TÌM THẤY NÚT NEXT"
                }
            }

            btnSolve.setOnClickListener {
                val accessibility = NexusAccessibilityService.instance
                if (accessibility == null) { tvStatus.text = "BẬT TRỢ NĂNG!"; return@setOnClickListener }
                tvResult.text = "⏳"
                
                val prefs = getSharedPreferences("nexus", MODE_PRIVATE)
                val apiKeys = listOf(
                    prefs.getString("groqKey1", "") ?: "",
                    prefs.getString("groqKey2", "") ?: "",
                    prefs.getString("groqKey3", "") ?: ""
                ).filter { it.isNotBlank() }
                
                if (apiKeys.isEmpty()) { tvStatus.text = "CHƯA NHẬP KEY!"; return@setOnClickListener }
                
                // Gọi hàm giải 1 câu duy nhất, truyền trạng thái của Switch
                accessibility.solveCurrentQuestion(apiKeys, switchAutoNext.isChecked)
            }

            NexusEvents.onStatus = { msg -> tvStatus.text = msg }
            NexusEvents.onResult = { res -> tvResult.text = res }
            
        } catch (e: Exception) { stopSelf() }
    }

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

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
