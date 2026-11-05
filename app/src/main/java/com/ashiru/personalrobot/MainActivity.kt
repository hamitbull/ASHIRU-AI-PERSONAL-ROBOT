package com.ashiru.personalrobot

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var brain: OfflineBrain
    private lateinit var tts: TextToSpeech
    private lateinit var chat: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var question: EditText
    private lateinit var status: TextView
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        brain = OfflineBrain(this)
        tts = TextToSpeech(this, this)
        window.statusBarColor = Color.rgb(10, 15, 32)
        window.navigationBarColor = Color.rgb(10, 15, 32)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 12)
            setBackgroundColor(Color.rgb(10, 15, 32))
        }
        val header = TextView(this).apply {
            text = "🤖  ASHIRU AI\nMataimaki na sirri • Offline Brain"
            textSize = 21f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            setPadding(8, 8, 8, 16)
        }
        root.addView(header, match())
        status = TextView(this).apply {
            text = "A shirye nake. Za ka iya rubuta min ko ka yi magana."
            textSize = 14f; gravity = Gravity.CENTER
            setTextColor(Color.rgb(180, 210, 255)); setPadding(8, 8, 8, 12)
        }
        root.addView(status, match())
        scroll = ScrollView(this).apply { isFillViewport = true }
        chat = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(4, 4, 4, 12) }
        scroll.addView(chat)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        addMessage("ASHIRU AI", "Sannu! Zan iya amsa tambayoyin da ke cikin ilimina na offline, in nemo memory ɗinka, kuma in yi magana idan wayarka tana da Hausa TTS.")

        val inputRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        question = EditText(this).apply {
            hint = "Rubuta tambayarka..."; maxLines = 3; textSize = 16f
            setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY); setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.rgb(35, 43, 68))
        }
        inputRow.addView(question, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        inputRow.addView(Button(this).apply { text = "Aika"; setOnClickListener { sendQuestion() } })
        root.addView(inputRow, match())

        val tools = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tools.addView(toolButton("🎙 Murya") { listen() }, weighted())
        tools.addView(toolButton("🧠 Memory") { showMemories() }, weighted())
        tools.addView(toolButton("🧹 Goge") { confirmClearMemories() }, weighted())
        root.addView(tools, match())
        val learnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        learnRow.addView(toolButton("Ajiye sabon memory") { saveMemory() }, weighted())
        learnRow.addView(toolButton("Floating robot") { toggleOverlay() }, weighted())
        root.addView(learnRow, match())

        val privacy = TextView(this).apply {
            text = "Sirri: memory yana cikin ajiyar sirrin wannan app a wayarka. Ba na karanta wasu apps, saƙonni ko screen ba tare da izini bayyananne ba."
            textSize = 11f; setTextColor(Color.LTGRAY); setPadding(4, 8, 4, 0)
        }
        root.addView(privacy, match())
        setContentView(root)
    }

    private fun match() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 7 }
    private fun weighted() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    private fun toolButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 11f; setPadding(2, 4, 2, 4); setOnClickListener { action() }
    }

    private fun addMessage(who: String, message: String) {
        val bubble = TextView(this).apply {
            text = "$who\n$message"; textSize = 15f; setTextColor(Color.WHITE)
            setPadding(14, 10, 14, 10)
            setBackgroundColor(if (who == "Kai" || who == "Kai (murya)") Color.rgb(39, 67, 102) else Color.rgb(35, 43, 68))
        }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = 8; chat.addView(bubble, lp)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun sendQuestion() {
        val text = question.text.toString().trim()
        if (text.isEmpty()) return
        question.text.clear(); addMessage("Kai", text); respond(text)
    }

    private fun respond(words: String) {
        val q = words.lowercase(Locale.ROOT)
        val answer = when {
            q.contains("me ka tuna") || q.contains("show memory") || q.contains("show my memory") ->
                brain.getMemories().joinToString("\n• ", prefix = "Abubuwan da na ajiye:\n• ").ifBlank { "Babu memory tukuna." }
            q.contains("waye kai") || q.contains("who are you") ->
                "Ni ne ASHIRU AI, mataimaki na sirri. Ina amfani da ilimin da aka haɗa da app da memory ɗin da ka zaɓa ka ajiye."
            q.contains("sannu") || q == "hello" || q == "hi" -> "Sannu! Me kake son mu yi yau?"
            q.contains("na gode") || q.contains("thank you") -> "Babu komai. Ina farin cikin taimaka maka."
            q.contains("goge memory") || q.contains("forget all") -> "Domin goge memory, danna maɓallin Goge sannan ka tabbatar."
            else -> brain.findMemory(words) ?: brain.answer(words)
                ?: "Ban sami amsar wannan tambayar a ilimina na offline ba tukuna. Ka iya koya min ta maɓallin Ajiye sabon memory. Bayanan yanzu ko cikakken AI na cloud suna buƙatar internet."
        }
        addMessage("ASHIRU AI", answer); status.text = "Na amsa ta amfani da ilimin offline/memory."; speak(answer)
    }

    private fun saveMemory() {
        val edit = EditText(this).apply { hint = "Rubuta abin da kake so in tuna..."; minLines = 3; setPadding(12, 10, 12, 10) }
        AlertDialog.Builder(this).setTitle("Koya min / Ajiye memory")
            .setMessage("Wannan bayanin zai kasance a cikin wayarka kawai.").setView(edit)
            .setPositiveButton("Ajiye") { _, _ ->
                val value = edit.text.toString().trim()
                if (value.isNotEmpty()) { brain.remember(value); addMessage("ASHIRU AI", "Na ajiye wannan a memory na wayarka.") }
            }.setNegativeButton("Soke", null).show()
    }

    private fun showMemories() {
        val memories = brain.getMemories()
        val content = if (memories.isEmpty()) "Babu memory tukuna." else memories.mapIndexed { index, s -> "${index + 1}. $s" }.joinToString("\n\n")
        AlertDialog.Builder(this).setTitle("Memory na wayarka").setMessage(content).setPositiveButton("Rufe", null).show()
    }

    private fun confirmClearMemories() {
        AlertDialog.Builder(this).setTitle("Goge duk memory?")
            .setMessage("Wannan zai goge duk bayanan da ka koya wa robot daga ajiyar wayarka. Ba za a iya dawo da su ba.")
            .setNegativeButton("A'a", null).setPositiveButton("Eh, goge") { _, _ ->
                brain.forgetAllMemories(); addMessage("ASHIRU AI", "An goge memory na sirri daga wannan app.")
            }.show()
    }

    private fun toggleOverlay() {
        if (android.provider.Settings.canDrawOverlays(this)) {
            startService(Intent(this, RobotOverlayService::class.java))
            status.text = "Floating robot ya fara aiki. Taɓa shi domin komawa app."
        } else startActivity(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName")))
    }

    private fun listen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 40); return
        }
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ha-NG")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Ina sauraronka...")
            }
            startActivityForResult(intent, 41)
        } catch (_: Exception) { addMessage("ASHIRU AI", "Ba a samu speech recognition a wayarka ba. Ka gwada rubutu.") }
    }

    @Deprecated("Retained for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 41 && resultCode == RESULT_OK) {
            val words = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (words.isNotBlank()) { addMessage("Kai (murya)", words); respond(words) }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 40 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) listen()
        else if (requestCode == 40) status.text = "An hana microphone. Za ka iya amfani da rubutu."
    }

    private fun speak(text: String) { if (ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ashiru_reply") }

    override fun onInit(result: Int) {
        ttsReady = result == TextToSpeech.SUCCESS
        if (ttsReady) {
            val ha = tts.setLanguage(Locale("ha", "NG"))
            if (ha == TextToSpeech.LANG_MISSING_DATA || ha == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.UK); status.text = "Hausa TTS bai samu ba; an gwada English voice."
            }
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}