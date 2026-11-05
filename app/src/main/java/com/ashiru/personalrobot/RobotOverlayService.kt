package com.ashiru.personalrobot

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView

class RobotOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var robot: TextView? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = 20; y = 180 }

        var startX = 0f; var startY = 0f; var initialX = 0; var initialY = 0
        robot = TextView(this).apply {
            text = "🤖"; textSize = 38f; setPadding(14, 10, 14, 10); setBackgroundColor(0xCC202B48.toInt())
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { startX = event.rawX; startY = event.rawY; initialX = params.x; initialY = params.y; true }
                    MotionEvent.ACTION_UP -> {
                        val moved = kotlin.math.abs(event.rawX - startX) > 12 || kotlin.math.abs(event.rawY - startY) > 12
                        if (!moved) startActivity(Intent(this@RobotOverlayService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX - (event.rawX - startX).toInt()
                        params.y = initialY + (event.rawY - startY).toInt()
                        try { windowManager.updateViewLayout(this, params) } catch (_: Exception) {}
                        true
                    }
                    else -> false
                }
            }
        }
        windowManager.addView(robot, params)
    }

    override fun onDestroy() {
        robot?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        robot = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}