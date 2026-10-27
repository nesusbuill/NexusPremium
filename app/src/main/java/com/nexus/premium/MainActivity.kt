package com.nexus.premium

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    
    private lateinit var tvNotiStatus: TextView
    private lateinit var tvAccStatus: TextView
    private lateinit var tvOverlayStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Gọi file XML giao diện mới

        val prefs = getSharedPreferences("nexus", MODE_PRIVATE)
        val etKey1 = findViewById<EditText>(R.id.etKey1)
        val etKey2 = findViewById<EditText>(R.id.etKey2)
        val etKey3 = findViewById<EditText>(R.id.etKey3)
        
        etKey1.setText(prefs.getString("groqKey1", ""))
        etKey2.setText(prefs.getString("groqKey2", ""))
        etKey3.setText(prefs.getString("groqKey3", ""))

        tvNotiStatus = findViewById(R.id.tvNotiStatus)
        tvAccStatus = findViewById(R.id.tvAccStatus)
        tvOverlayStatus = findViewById(R.id.tvOverlayStatus)

        findViewById<Button>(R.id.btnSaveKey).setOnClickListener {
            prefs.edit().apply {
                putString("groqKey1", etKey1.text.toString())
                putString("groqKey2", etKey2.text.toString())
                putString("groqKey3", etKey3.text.toString())
                apply()
            }
            Toast.makeText(this, "Đã lưu cấu hình!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnNoti).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        findViewById<Button>(R.id.btnAcc).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnOverlay).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else {
                Toast.makeText(this, "Đã có quyền Cửa sổ nổi!", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            checkAndStartService()
        }
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật trạng thái quyền mỗi khi quay lại App
        updatePermissionsStatus()
    }

    private fun updatePermissionsStatus() {
        // Check Noti
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                tvNotiStatus.text = "1. Quyền Thông Báo (Đã Bật) ✅"
                tvNotiStatus.setTextColor(android.graphics.Color.parseColor("#00FF41"))
            } else {
                tvNotiStatus.text = "1. Quyền Thông Báo (Chưa Bật) ❌"
                tvNotiStatus.setTextColor(android.graphics.Color.parseColor("#FF0055"))
            }
        } else {
            tvNotiStatus.text = "1. Quyền Thông Báo (Không cần) ✅"
        }

        // Check Accessibility (Giả lập check bằng cách xem Service có null không)
        if (NexusAccessibilityService.instance != null) {
            tvAccStatus.text = "2. Quyền Trợ Năng (Đã Bật) ✅"
            tvAccStatus.setTextColor(android.graphics.Color.parseColor("#00FF41"))
        } else {
            tvAccStatus.text = "2. Quyền Trợ Năng (Chưa Bật) ❌"
            tvAccStatus.setTextColor(android.graphics.Color.parseColor("#FF0055"))
        }

        // Check Overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                tvOverlayStatus.text = "3. Quyền Cửa Sổ Nổi (Đã Bật) ✅"
                tvOverlayStatus.setTextColor(android.graphics.Color.parseColor("#00FF41"))
            } else {
                tvOverlayStatus.text = "3. Quyền Cửa Sổ Nổi (Chưa Bật) ❌"
                tvOverlayStatus.setTextColor(android.graphics.Color.parseColor("#FF0055"))
            }
        }
    }

    private fun checkAndStartService() {
        if (NexusAccessibilityService.instance == null) {
            Toast.makeText(this, "Vui lòng bật quyền Trợ Năng trước!", Toast.LENGTH_LONG).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Vui lòng cấp quyền Cửa sổ nổi!", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val serviceIntent = Intent(this, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent) else startService(serviceIntent)
            Toast.makeText(this, "NEXUS ĐÃ KHỞI CHẠY!", Toast.LENGTH_SHORT).show()
            finish() // Tắt app, nhường màn hình cho cửa sổ nổi
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
