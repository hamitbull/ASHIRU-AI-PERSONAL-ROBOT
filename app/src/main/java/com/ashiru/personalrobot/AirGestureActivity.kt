package com.ashiru.personalrobot

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.widget.LinearLayout
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import java.util.concurrent.Executors

class AirGestureActivity : Activity() {
    private lateinit var preview:PreviewView
    private lateinit var status:TextView
    private val cameraExecutor=Executors.newSingleThreadExecutor()
    private var landmarker:HandLandmarker?=null
    private var previousY:Float?=null
    private var lastGestureAt=0L
    private var lastLabel=""

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.BLACK)}
        status=TextView(this).apply{text="Ka nuna hannunka a gaban camera.";setTextColor(Color.WHITE);textSize=16f;setPadding(12,12,12,12)}
        preview=PreviewView(this)
        root.addView(status,LinearLayout.LayoutParams(-1,-2));root.addView(preview,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.CAMERA),61) else startCamera()
    }

    private fun startCamera(){
        try{
            val base=BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build()
            val options=HandLandmarker.HandLandmarkerOptions.builder().setBaseOptions(base)
                .setRunningMode(RunningMode.VIDEO).setNumHands(2)
                .setMinHandDetectionConfidence(0.55f).setMinTrackingConfidence(0.55f).build()
            landmarker=HandLandmarker.createFromOptions(this,options)
        }catch(e:Exception){status.text="Ba a samu hand model ba: ${e.message}";return}
        val providerFuture=ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider=providerFuture.get()
            val previewUseCase=Preview.Builder().build().also{it.setSurfaceProvider(preview.surfaceProvider)}
            val analysis=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(cameraExecutor){image->
                try{
                    val bitmap=image.toBitmap()
                    val mp=BitmapImageBuilder(bitmap).build()
                    val result=landmarker?.detectForVideo(mp,SystemClock.uptimeMillis())
                    val hand=result?.landmarks()?.firstOrNull()
                    if(hand!=null && hand.size>=21){
                        val wrist=hand[0]
                        val indexTip=hand[8];val indexPip=hand[6]
                        val middleTip=hand[12];val middlePip=hand[10]
                        val ringTip=hand[16];val ringPip=hand[14]
                        val pinkyTip=hand[20];val pinkyPip=hand[18]
                        val fingers=listOf(indexTip.y()<indexPip.y(),middleTip.y()<middlePip.y(),ringTip.y()<ringPip.y(),pinkyTip.y()<pinkyPip.y()).count{it}
                        val now=SystemClock.uptimeMillis()
                        val old=previousY
                        val label=when{
                            fingers>=2 && now-lastGestureAt>1000 -> "DOUBLE-FINGER: yatsu biyu sun mike"
                            old!=null && wrist.y()<old-0.10f && now-lastGestureAt>900 -> "SWIPE UP: motsi sama"
                            old!=null && wrist.y()>old+0.10f && now-lastGestureAt>900 -> "SWIPE DOWN: motsi kasa"
                            else -> "An gano hannu • yatsu masu mike: $fingers"
                        }
                        previousY=wrist.y()
                        if(label!=lastLabel && (label.startsWith("SWIPE")||label.startsWith("DOUBLE")||label.contains("An gano"))){
                            lastLabel=label
                            if(label.startsWith("SWIPE")||label.startsWith("DOUBLE"))lastGestureAt=now
                            runOnUiThread{status.text=label}
                        }
                    }else{previousY=null;runOnUiThread{status.text="Nuna tafin hannunka ga camera..."}}
                }catch(_:Exception){}finally{image.close()}
            }
            try{
                provider.unbindAll()
                provider.bindToLifecycle(this,CameraSelector.DEFAULT_FRONT_CAMERA,previewUseCase,analysis)
            }catch(e:Exception){status.text="Camera ta kasa farawa: ${e.message}"}
        },ContextCompat.getMainExecutor(this))
    }
    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==61 && grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)startCamera() else status.text="An hana camera permission."
    }
    override fun onDestroy(){try{landmarker?.close()}catch(_:Exception){};cameraExecutor.shutdown();super.onDestroy()}
}