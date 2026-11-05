package com.ashiru.personalrobot

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var brain: OfflineBrain
    private lateinit var tts: TextToSpeech
    private lateinit var chat: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var question: EditText
    private lateinit var status: TextView
    private val executor = Executors.newSingleThreadExecutor()
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        brain = OfflineBrain(this); tts = TextToSpeech(this, this)
        window.statusBarColor = Color.rgb(10, 15, 32); window.navigationBarColor = Color.rgb(10, 15, 32)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 14, 16, 10); setBackgroundColor(Color.rgb(10, 15, 32)) }
        root.addView(TextView(this).apply { text = "🤖 ASHIRU AI\nPersonal Robot • Hausa / English"; textSize = 21f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); setPadding(6, 8, 6, 12) }, match())
        status = TextView(this).apply { text = "Offline brain a shirye yake."; gravity = Gravity.CENTER; setTextColor(Color.rgb(180,210,255)); textSize = 13f }
        root.addView(status, match())
        scroll = ScrollView(this).apply { isFillViewport = true }
        chat = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(4,4,4,10) }
        scroll.addView(chat); root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        addMessage("ASHIRU AI","Sannu! Ka tambaye ni. Idan ka saita AI API, zan iya amfani da cloud AI; idan babu internet zan yi amfani da ilimin offline.")
        val input = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        question = EditText(this).apply { hint = "Rubuta tambaya..."; maxLines = 3; setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY); setPadding(10,8,10,8); setBackgroundColor(Color.rgb(35,43,68)) }
        input.addView(question, LinearLayout.LayoutParams(0,-2,1f))
        input.addView(Button(this).apply { text = "Aika"; setOnClickListener { sendQuestion() } })
        root.addView(input, match())
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(toolButton("🎙 Murya") { listen() }, weighted())
        row1.addView(toolButton("AI Settings") { aiSettings() }, weighted())
        row1.addView(toolButton("Memory") { showMemories() }, weighted())
        root.addView(row1, match())
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(toolButton("Ajiye memory") { saveMemory() }, weighted())
        row2.addView(toolButton("Accessibility") { accessibilitySetup() }, weighted())
        row2.addView(toolButton("Karanta screen") { readScreen() }, weighted())
        root.addView(row2, match())
        val row3 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row3.addView(toolButton("Camera gestures") { startActivity(Intent(this@MainActivity, AirGestureActivity::class.java)) }, weighted())
        row3.addView(toolButton("Music dance") { startActivity(Intent(this@MainActivity, MusicDanceActivity::class.java)) }, weighted())
        row3.addView(toolButton("Floating robot") { toggleOverlay() }, weighted())
        root.addView(row3, match())
        root.addView(toolButton("Goge duk memory") { confirmClearMemories() }, match())
        root.addView(TextView(this).apply { text = "Sirri: AI API key ana ɓoye shi da Android Keystore a wayarka. Screen reading sai ka kunna Accessibility da kanka. Camera da microphone suna aiki ne bayan izini."; textSize=10f; setTextColor(Color.LTGRAY); setPadding(4,5,4,0) }, match())
        setContentView(root)
    }

    private fun match() = LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=5 }
    private fun weighted() = LinearLayout.LayoutParams(0,-2,1f)
    private fun toolButton(label:String, action:()->Unit) = Button(this).apply { text=label; textSize=10f; setPadding(1,3,1,3); setOnClickListener { action() } }
    private fun addMessage(who:String, message:String) {
        val bubble=TextView(this).apply { text="$who\n$message"; textSize=14f; setTextColor(Color.WHITE); setPadding(12,9,12,9); setBackgroundColor(if(who=="Kai") Color.rgb(39,67,102) else Color.rgb(35,43,68)) }
        chat.addView(bubble, LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=7 })
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }
    private fun sendQuestion() { val s=question.text.toString().trim(); if(s.isEmpty()) return; question.text.clear(); addMessage("Kai",s); askAi(s) }

    private fun askAi(prompt:String) {
        status.text="AI na duba tambayarka..."
        executor.execute {
            val cloud=AiClient(this).ask(prompt)
            val answer=cloud ?: brain.findMemory(prompt) ?: brain.answer(prompt)
                ?: "Ban sami wannan a ilimina na offline ba. Ka saita AI API a AI Settings domin tambayoyi masu zurfi."
            runOnUiThread { addMessage("ASHIRU AI",answer); status.text=if(cloud!=null) "An amsa ta AI online." else "An amsa ta offline."; speak(answer) }
        }
    }
    private fun aiSettings() {
        val prefs=getSharedPreferences("ai_config",MODE_PRIVATE)
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(18,4,18,4) }
        val endpoint=EditText(this).apply { hint="API URL (OpenAI-compatible)"; setSingleLine(); setText(prefs.getString("endpoint","https://api.openai.com/v1/chat/completions")) }
        val model=EditText(this).apply { hint="Model name"; setSingleLine(); setText(prefs.getString("model","gpt-4o-mini")) }
        val key=EditText(this).apply { hint="API key (bar shi idan ka riga ka saka)"; setSingleLine(); inputType=129 }
        box.addView(endpoint); box.addView(model); box.addView(key)
        AlertDialog.Builder(this).setTitle("AI connection")
            .setMessage("Saka endpoint, model da API key daga mai bada AI service. Kada ka saka key a GitHub. Amfani da cloud na iya haifar da kuɗin provider.")
            .setView(box).setPositiveButton("Ajiye") { _,_ ->
                prefs.edit().putString("endpoint",endpoint.text.toString().trim()).putString("model",model.text.toString().trim()).apply()
                if(key.text.toString().isNotBlank()) SecurePrefs.save(this,key.text.toString().trim())
                status.text="An ajiye saitin AI a wayarka."
            }.setNeutralButton("Goge key") { _,_ -> SecurePrefs.clear(this) }
            .setNegativeButton("Soke",null).show()
    }
    private fun saveMemory() {
        val edit=EditText(this).apply { hint="Abin da kake so robot ya tuna"; minLines=3 }
        AlertDialog.Builder(this).setTitle("Ajiye memory").setView(edit).setPositiveButton("Ajiye") { _,_ ->
            val v=edit.text.toString().trim(); if(v.isNotEmpty()){brain.remember(v);addMessage("ASHIRU AI","Na ajiye memory a wayarka.")}
        }.setNegativeButton("Soke",null).show()
    }
    private fun showMemories() {
        val values=brain.getMemories()
        AlertDialog.Builder(this).setTitle("Memory").setMessage(if(values.isEmpty()) "Babu memory." else values.mapIndexed { i,v -> "${i+1}. $v" }.joinToString("\n\n")).setPositiveButton("Rufe",null).show()
    }
    private fun confirmClearMemories() {
        AlertDialog.Builder(this).setTitle("Goge duk memory?").setMessage("Ba za a iya dawo da su ba.")
            .setNegativeButton("A'a",null).setPositiveButton("Goge") { _,_ -> brain.forgetAllMemories(); addMessage("ASHIRU AI","An goge memory.") }.show()
    }
    private fun accessibilitySetup() {
        AlertDialog.Builder(this).setTitle("Kunna Accessibility")
            .setMessage("Android Settings zai bude. Ka zaɓi ASHIRU AI Screen Reader sannan ka kunna shi da kanka. App ɗin zai karanta rubutun screen ne kawai idan ka danna Karanta screen.")
            .setNegativeButton("Soke",null).setPositiveButton("Buɗe Settings") { _,_ -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }.show()
    }
    private fun readScreen() {
        val value=ScreenReaderService.latestText
        if(!value.isNullOrBlank()) {
            addMessage("ASHIRU AI","Rubutun da aka karanta daga app ɗin da ka zaɓa:\\n"+value)
            ScreenReaderService.latestText=null
        } else {
            ScreenReaderService.requestNextRead=true
            addMessage("ASHIRU AI","Na shirya karantawa. Ka koma app ɗin da kake son karantawa; bayan ya nuna rubutu, ka dawo nan ka danna Karanta screen.")
        }
    }
    private fun toggleOverlay() {
        if(Settings.canDrawOverlays(this)) startService(Intent(this,RobotOverlayService::class.java))
        else startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:$packageName")))
    }
    private fun listen() {
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),40);return}
        try { startActivityForResult(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ha-NG")
        },41) } catch(_:Exception) { addMessage("ASHIRU AI","Speech recognition bai samu ba.") }
    }
    @Deprecated("Legacy speech callback")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==41 && resultCode==RESULT_OK){val s=data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty();if(s.isNotBlank()){addMessage("Kai",s);askAi(s)}}
    }
    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){super.onRequestPermissionsResult(requestCode,permissions,grantResults)}
    private fun speak(s:String){if(ttsReady)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"ashiru_reply")}
    override fun onInit(result:Int){ttsReady=result==TextToSpeech.SUCCESS;if(ttsReady){val h=tts.setLanguage(Locale("ha","NG"));if(h==TextToSpeech.LANG_NOT_SUPPORTED||h==TextToSpeech.LANG_MISSING_DATA)tts.setLanguage(Locale.UK)}}
    override fun onDestroy(){executor.shutdownNow();if(::tts.isInitialized){tts.stop();tts.shutdown()};super.onDestroy()}
}