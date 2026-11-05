package com.ashiru.personalrobot

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var prefs: SharedPreferences
    private lateinit var offlineBrain: OfflineBrain
    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private lateinit var memory: EditText
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("ashiru_memory", MODE_PRIVATE)
        offlineBrain = OfflineBrain(this)
        tts = TextToSpeech(this, this)
        window.statusBarColor = Color.rgb(13, 17, 35)
        window.navigationBarColor = Color.rgb(13, 17, 35)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(13, 17, 35))
        }
        val title = TextView(this).apply {
            text = "ASHIRU AI"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(title, match())
        val robot = TextView(this).apply {
            text = "🤖"
            textSize = 92f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 12)
        }
        root.addView(robot, match())
        status = TextView(this).apply {
            text = "Sannu! Ni ce ASHIRU AI. Me zan taimaka maka da shi?"
            textSize = 17f
            setTextColor(Color.rgb(210, 220, 255))
            gravity = Gravity.CENTER
        }
        root.addView(status, match())
        val speak = Button(this).apply {
            text = "🎙 Yi magana da ni"
            setOnClickListener { listen() }
        }
        root.addView(speak, match())
        val overlay = Button(this).apply {
            text = "Sanya robot a saman screen"
            setOnClickListener {
                if (android.provider.Settings.canDrawOverlays(this@MainActivity)) {
                    startService(Intent(this@MainActivity, RobotOverlayService::class.java))
                    say("Na fito a saman screen.")
                } else {
                    startActivity(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:$packageName")))
                }
            }
        }
        root.addView(overlay, match())
        val heading = TextView(this).apply {
            text = "Abubuwan da nake koya daga gare ka"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 20, 0, 8)
        }
        root.addView(heading, match())
        memory = EditText(this).apply {
            hint = "Misali: Ka kira ni Ashiru; ina son amsa a Hausa"
            setText(prefs.getString("memory", ""))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setBackgroundColor(Color.rgb(35, 43, 68))
            setPadding(14, 12, 14, 12)
        }
        root.addView(memory, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110))
        val save = Button(this).apply {
            text = "Ajiye abin da na koya"
            setOnClickListener {
                prefs.edit().putString("memory", memory.text.toString()).apply()
                offlineBrain.remember(memory.text.toString())
                say("Na ajiye abin da ka koya min a wayarka.")
            }
        }
        root.addView(save, match())
        val privacy = TextView(this).apply {
            text = "Sirri: Abubuwan da ka rubuta a nan suna ajiyewa a wayarka. Ba na karanta sauran apps sai ka ba ni izini ta hanyar Android."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(4, 18, 4, 0)
        }
        root.addView(privacy, match())
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun match() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 10 }

    private fun listen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 40)
            return
        }
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ha-NG")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Ina sauraronka...")
            }
            startActivityForResult(intent, 41)
        } catch (_: Exception) {
            status.text = "Ba a samu speech recognition a wayarka ba."
        }
    }

    @Deprecated("Deprecated by Android; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 41 && resultCode == RESULT_OK) {
            val words = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            status.text = "Ka ce: $words"
            respond(words)
        }
    }

    private fun respond(words: String) {
        val q = words.lowercase(Locale.ROOT)
        val answer = when {
            q.contains("me ka tuna") || q.contains("show memory") ->
                offlineBrain.getMemories().joinToString("\\n").ifBlank { "Har yanzu babu abin da ka ajiye a memory." }
            q.contains("sannu") || q.contains("hello") -> "Sannu da zuwa! Ina nan domin taimaka maka."
            q.contains("waye") && (q.contains("kai") || q.contains("ke")) -> "Ni ce ASHIRU AI, robot mataimakiyarka."
            q.contains("na gode") -> "Babu komai. Ina farin cikin taimaka maka."
            q.contains("tuna") -> offlineBrain.findMemory(words) ?: offlineBrain.answer(words)
                ?: "Ban sami wannan bayanin a ƙwaƙwalwata ba tukuna."
            else -> offlineBrain.answer(words)
                ?: "Na ji tambayarka, amma ban da isasshen bayani a ilimina na offline tukuna. Ka iya koya min ta akwatin memory."
        }
        status.text = answer
        say(answer)
    }

    private fun say(text: String) {
        if (ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ashiru_reply")
        status.text = text
    }

    override fun onInit(result: Int) {
        ttsReady = result == TextToSpeech.SUCCESS
        if (ttsReady) tts.language = Locale("ha", "NG")
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}
