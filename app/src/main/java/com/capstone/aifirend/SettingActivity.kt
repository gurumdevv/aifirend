package com.capstone.aifirend

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class SettingActivity : AppCompatActivity() {
    private var camera_switch: Switch? = null
    private var mute_switch: Switch? = null
    private var volume_seekbar: SeekBar? = null
    private var speed_seekbar: SeekBar? = null
    private var save_switch: Switch? = null
    private var chatDAO: ChatDAO = ChatDAO(this)
    private lateinit var viewContainer : View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        viewContainer = findViewById(R.id.setting_inner_container)
        //Status bar Transparency
        this.setStatusBarTransparent()

        viewContainer.setPadding(
            0,
            this.statusBarHeight(),
            0,
            this.navigationHeight()
        )

        camera_switch = findViewById(R.id.camera_switch)
        mute_switch = findViewById(R.id.mute_switch)
        volume_seekbar = findViewById(R.id.volume_seekbar)
        speed_seekbar = findViewById(R.id.speed_seekbar)
        save_switch = findViewById(R.id.save_switch)

        camera_switch?.setOnCheckedChangeListener {view, value ->
            var prefs = getSharedPreferences("setting", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("camera", value).apply()
        }
        mute_switch?.setOnCheckedChangeListener {view, value ->
            var prefs = getSharedPreferences("setting", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("mute", value).apply()
        }
        volume_seekbar?.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                var prefs = getSharedPreferences("setting", Context.MODE_PRIVATE)
                prefs.edit().putInt("volume", seekBar.progress).apply()
            }
        })
        speed_seekbar?.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                var prefs = getSharedPreferences("setting", Context.MODE_PRIVATE)
                prefs.edit().putInt("speed", seekBar.progress).apply()
            }
        })
        save_switch?.setOnCheckedChangeListener {view, value ->
            var prefs = getSharedPreferences("setting", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("save", value).apply()
        }
        findViewById<Button>(R.id.clear_history_button).setOnClickListener {
            val dialog: AlertDialog.Builder = AlertDialog.Builder(this@SettingActivity, R.style.Theme_CustomDialog)
            dialog.setTitle("대화 내역 삭제")
            dialog.setMessage("대화 내역을 삭제 하시겠습니까?")
            //dialog.setIcon(R.mipmap.ic_launcher)
            //dialog.setView(R.layout.dialog_clear_history)

            dialog.setPositiveButton("삭제", {view, result ->
                chatDAO.DropChat()
                Toast.makeText(this, "대화 내역을 삭제했습니다.", Toast.LENGTH_SHORT).show()
            })
            dialog.setNegativeButton("취소", null)

            dialog.show()
        }

        Initialize()
    }

    //상태바 및 네비게이션바 투명화
    fun Activity.setStatusBarTransparent() {
        window.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
        if(Build.VERSION.SDK_INT >= 30) {	// API 30 에 적용
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    fun Context.statusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")

        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId)
        else 0
    }

    fun Context.navigationHeight(): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")

        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId)
        else 0
    }

    private fun Initialize()
    {
        //var prefs = getSharedPreferences("setting", Context.MODE_PRIVATE)

        camera_switch?.isChecked = SettingValue.camera
        mute_switch?.isChecked = SettingValue.mute
        volume_seekbar?.progress = SettingValue.volume
        speed_seekbar?.progress = SettingValue.speed
        save_switch?.isChecked = SettingValue.save
    }
}