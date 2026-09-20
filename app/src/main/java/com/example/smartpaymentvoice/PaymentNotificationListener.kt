package com.example.smartpaymentvoice

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class PaymentNotificationListener : NotificationListenerService(),
    TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var lastSpoken = ""
    private var lastSpokenTime = 0L

    private val paymentApps = setOf(
        "com.phonepe.app",
        "com.google.android.apps.nbu.paisa.user",
        "net.one97.paytm",
        "in.org.npci.upiapp"
    )

    private val amountRegex =
        Regex("""(?:₹|Rs\.?|INR)\s?([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val phoneLikeRegex = Regex("""^\+?[\d\s\-]{7,}$""")
    private val numberMaskRegex = Regex("""\+?\d[\d\s\-]{6,}\d""")
    private val upiMaskRegex = Regex("""[\w.\-]+@\w+""")
    private val creditRegex =
        Regex("""to you|paid you|received|credited""", RegexOption.IGNORE_CASE)
    private val blockRegex = Regex(
        """\b(otp|pin|debited|failed|declined|pending|requests?|requested)\b|you paid|paid to""",
        RegexOption.IGNORE_CASE
    )
    private val senderBeforePaidRegex =
        Regex("""^(.+?)\s+(?:paid|sent)\s+you""", RegexOption.IGNORE_CASE)
    private val senderAfterFromRegex = Regex(
        """\bfrom\s+([A-Za-z][A-Za-z .'\-]{1,40}?)(?:\s+(?:on|via|using|to|in|at|for)\b|[.,]|$)""",
        RegexOption.IGNORE_CASE
    )
    private val genericTitleWords = listOf(
        "received", "payment", "phonepe", "google pay", "paytm", "money", "credited", "bhim"
    )

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        Log.d("PaymentListener", "TTS ready=$ttsReady")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in paymentApps) return
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        if (!prefs.getBoolean("voice_enabled", true)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        if (title.isEmpty() && text.isEmpty()) return


        val payment = parsePayment(title, text) ?: return
        val sender = payment.first
        val amount = payment.second.replace(",", "")
        Log.d("PaymentListener", "payment detected")

        val unitEn = if (amount == "1" || amount == "1.00") "rupee" else "rupees"
        val english =
            if (sender == null) "$amount $unitEn received"
            else "$amount $unitEn received from $sender"
        val telugu =
            if (sender == null) "$amount రూపాయలు వచ్చాయి"
            else "$sender నుండి $amount రూపాయలు వచ్చాయి"

        speak(english, telugu, prefs.getString("language", "en") == "te")
    }

    private fun parsePayment(title: String, text: String): Pair<String?, String>? {
        val combined = "$title $text"
        if (blockRegex.containsMatchIn(combined)) return null
        if (!creditRegex.containsMatchIn(combined)) return null
        val amount = amountRegex.find(text)?.groupValues?.get(1)
            ?: amountRegex.find(title)?.groupValues?.get(1)
            ?: return null
        return extractSender(title, text) to amount
    }

    private fun extractSender(title: String, text: String): String? {
        val fromTitle = title.substringBefore(":").trim()
        val fromText = senderBeforePaidRegex.find(text)?.groupValues?.get(1)
            ?: senderAfterFromRegex.find(text)?.groupValues?.get(1)
        return cleanName(fromTitle) ?: cleanName(fromText)
    }

    private fun cleanName(raw: String?): String? {
        val name = raw?.trim().orEmpty()
        if (name.isEmpty() || name.length > 40) return null
        if (name.contains("@")) return null
        if (phoneLikeRegex.matches(name)) return null
        if (name.count { it.isDigit() } >= 5) return null
        val lower = name.lowercase()
        if (genericTitleWords.any { lower.contains(it) }) return null
        return if (name == name.uppercase()) {
            lower.split(" ").filter { it.isNotEmpty() }
                .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
        } else name
    }

    private fun mask(s: String): String =
        s.replace(numberMaskRegex, "<number>").replace(upiMaskRegex, "<upi>")

    private fun speak(english: String, telugu: String, useTelugu: Boolean) {
        if (!ttsReady) return

        var message = english
        var locale = Locale.forLanguageTag("en-IN")
        if (useTelugu) {
            val result = tts?.setLanguage(Locale.forLanguageTag("te-IN"))
            if (result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                message = telugu
                locale = Locale.forLanguageTag("te-IN")
            } else {
                Log.d("PaymentListener", "Telugu voice missing, using English")
            }
        }
        tts?.setLanguage(locale)

        val now = System.currentTimeMillis()
        if (message == lastSpoken && now - lastSpokenTime < 5000) return
        lastSpoken = message
        lastSpokenTime = now
        tts?.speak(message, TextToSpeech.QUEUE_ADD, null, "payment")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}