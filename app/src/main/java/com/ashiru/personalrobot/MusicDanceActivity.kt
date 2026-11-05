package com.ashiru.personalrobot

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.concurrent.thread
import kotlin.math.abs

class MusicDanceActivity : Activity() {
    @Volatile private var running=false
    private var recorder:AudioRecord?=null
    private lateinit var robot:TextView
    private lateinit var info:TextView
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=android.view.Gravity.CENTER;setPadding(20,20,20,20);setBackgroundColor(Color.rgb(10,15,32))}
        robot=TextView(this).apply{text="🤖";textSize=100f;setTextColor(Color.WHITE)}
        info=TextView(this).apply{text="Ka kunna kiɗa a speaker, sannan danna Fara Dance.";setTextColor(Color.WHITE);textSize=16f}
        root.addView(robot);root.addView(info)
        root.addView(Button(this).apply{text="Fara Dance";setOnClickListener{startDance()}})
        root.addView(Button(this).apply{text="Tsaya";setOnClickListener{stopDance()}})
        setContentView(root)
    }
    private fun startDance(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),51);return}
        if(running)return
        val rate=16000;val size=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
        if(size<=0){info.text="Ba a iya kunna audio sensor ba.";return}
        try{recorder=AudioRecord(MediaRecorder.AudioSource.MIC,rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size)}catch(_:Exception){info.text="Microphone ya kasa farawa.";return}
        running=true
        thread(name="AshiruMusicDance"){
            val buffer=ShortArray(size/2)
            try{
                recorder?.startRecording()
                while(running){
                    val n=recorder?.read(buffer,0,buffer.size)?:0
                    var peak=0
                    for(i in 0 until n)peak=maxOf(peak,abs(buffer[i].toInt()))
                    val strength=(peak/9000f).coerceIn(0f,1f)
                    runOnUiThread{
                        val sign=if(System.currentTimeMillis()/180%2L==0L)1f else -1f
                        robot.rotation=sign*(4f+strength*22f)
                        robot.scaleX=1f+strength*0.35f;robot.scaleY=1f+strength*0.25f
                        info.text="Ina bin sautin kiɗa • mic level ${(strength*100).toInt()}%"
                    }
                }
            }catch(_:Exception){}finally{try{recorder?.stop()}catch(_:Exception){}}
        }
    }
    private fun stopDance(){
        running=false
        try{recorder?.stop()}catch(_:Exception){}
        try{recorder?.release()}catch(_:Exception){}
        recorder=null;robot.rotation=0f;robot.scaleX=1f;robot.scaleY=1f;info.text="An dakatar da dance."
    }
    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==51&&grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)startDance()
    }
    override fun onDestroy(){stopDance();super.onDestroy()}
}