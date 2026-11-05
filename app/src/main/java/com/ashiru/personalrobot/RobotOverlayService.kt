package com.ashiru.personalrobot

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class RobotOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var robot: TextView? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        robot = TextView(this).apply {
            text = "🤖"
            textSize = 42f
            setPadding(12, 8, 12, 8)
            setOnClickListener {
                startActivity(Intent(this@RobotOverlayService, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 180
        }
        windowManager.addView(robot, params)
    }

    override fun onDestroy() {
        robot?.let { windowManager.removeView(it) }
        robot = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
