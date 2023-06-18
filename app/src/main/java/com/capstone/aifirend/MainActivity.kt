package com.capstone.aifirend

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 버튼에 이벤트 연결
        findViewById<Button>(R.id.start_chat_button).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
        findViewById<Button>(R.id.start_chat_voice_button).setOnClickListener {
            startActivity(Intent(this, VoiceChatActivity::class.java))
        }
        findViewById<ImageButton>(R.id.setting_button).setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        //상태바 투명 적용
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val w = window
            w.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
    }

    override fun onResume() {
        super.onResume()
        LoadSetting()
    }

    private fun LoadSetting()
    {
        var prefs = getSharedPreferences("setting", Context.MODE_PRIVATE)
        SettingValue.camera = prefs.getBoolean("camera", true)
        SettingValue.mute = prefs.getBoolean("mute", false)
        SettingValue.volume = prefs.getInt("volume", 100)
        SettingValue.speed = prefs.getInt("speed", 10)
        SettingValue.save = prefs.getBoolean("save", false)
    }
}