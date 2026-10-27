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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 50, 50, 50) }
        
        layout.addView(TextView(this).apply { text = "NEXUS PREMIUM\n"; textSize = 22f; setTextColor(android.graphics.Color.parseColor("#00F3FF")) })
        
        val prefs = getSharedPreferences("nexus", MODE_PRIVATE)
        
        val inputKey1 = EditText(this).apply { hint = "Nhập Groq Key 1 (Bắt buộc)"; setText(prefs.getString("groqKey1", "")) }
        val inputKey2 = EditText(this).apply { hint = "Nhập Groq Key 2 (Tùy chọn)"; setText(prefs.getString("groqKey2", "")) }
        val inputKey3 = EditText(this).apply { hint = "Nhập Groq Key 3 (Tùy chọn)"; setText(prefs.getString("groqKey3", "")) }
        
        layout.addView(inputKey1)
        layout.addView(inputKey2)
        layout.addView(inputKey3)
        
        layout.addView(Button(this).apply {
            text = "1. LƯU TẤT CẢ KEY"
            setOnClickListener {
                prefs.edit().apply {
                    putString("groqKey1", inputKey1.text.toString())
                    putString("groqKey2", inputKey2.text.toString())
                    putString("groqKey3", inputKey3.text.toString())
                    apply()
                }
                Toast.makeText(this@MainActivity, "Đã lưu Key!", Toast.LENGTH_SHORT).show()
            }
        })
        
        layout.addView(Button(this).apply {
            text = "2. BẬT QUYỀN TRỢ NĂNG"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        
        layout.addView(Button(this).apply {
            text = "3. BẬT CỬA SỔ NỔI (CHẠY APP)"
            setOnClickListener { checkAndRequestPermissions() }
        })
        
        setContentView(layout)
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                Toast.makeText(this, "Hãy cấp quyền 'Hiển thị trên ứng dụng khác'!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {}
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {}
        }

        try {
            val serviceIntent = Intent(this, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent) else startService(serviceIntent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khởi động: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
