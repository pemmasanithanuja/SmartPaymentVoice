package com.example.smartpaymentvoice

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var statusText: TextView
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val pad = (24 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left + pad, bars.top + pad, bars.right + pad, bars.bottom + pad)
            insets
        }

        tts = TextToSpeech(this, this)

        statusText = findViewById(R.id.statusText)
        val accessButton = findViewById<Button>(R.id.accessButton)
        val voiceSwitch = findViewById<SwitchCompat>(R.id.voiceSwitch)
        val languageGroup = findViewById<RadioGroup>(R.id.languageGroup)
        val testButton = findViewById<Button>(R.id.testButton)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        voiceSwitch.isChecked = prefs.getBoolean("voice_enabled", true)
        voiceSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("voice_enabled", isChecked).apply()
        }

        val savedLanguage = prefs.getString("language", "en")
        languageGroup.check(
            if (savedLanguage == "te") R.id.langTelugu else R.id.langEnglish
        )
        languageGroup.setOnCheckedChangeListener { _, checkedId ->
            val language = if (checkedId == R.id.langTelugu) "te" else "en"
            prefs.edit().putString("language", language).apply()
        }

        accessButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        testButton.setOnClickListener {
            speakTest(prefs.getString("language", "en") == "te")
        }
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
    }

    private fun speakTest(useTelugu: Boolean) {
        if (!ttsReady) {
            Toast.makeText(this, "Voice engine not ready", Toast.LENGTH_SHORT).show()
            return
        }

        var message = "500 rupees received from Rahul"
        var locale = Locale.forLanguageTag("en-IN")

        if (useTelugu) {
            val result = tts?.setLanguage(Locale.forLanguageTag("te-IN"))
            if (result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                message = "Rahul నుండి 500 రూపాయలు వచ్చాయి"
                locale = Locale.forLanguageTag("te-IN")
            } else {
                Toast.makeText(
                    this,
                    "Telugu voice not installed. Speaking English.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        tts?.setLanguage(locale)
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "test")
    }

    override fun onResume() {
        super.onResume()
        val enabled = Settings.Secure
            .getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        statusText.text =
            if (enabled) "Notification access: ON" else "Notification access: OFF"
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}