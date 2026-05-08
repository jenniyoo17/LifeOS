package com.example.rememberclaw

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.app.TimePickerDialog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var chatView: TextView
    private lateinit var pendingCount: TextView
    private lateinit var completedCount: TextView
    private lateinit var totalCount: TextView
    private lateinit var inputText: EditText

    private lateinit var sendBtn: Button
    private lateinit var micBtn: Button
    private lateinit var reminderBtn: Button
    private lateinit var diaryBtn: Button

    private lateinit var tts: TextToSpeech

    private val httpClient = OkHttpClient()
    private val SPEECH_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // VIEWS
        chatView = findViewById(R.id.chatView)
        pendingCount = findViewById(R.id.pendingCount)
        completedCount = findViewById(R.id.completedCount)
        totalCount = findViewById(R.id.totalCount)
        inputText = findViewById(R.id.inputText)

        // DARK MODE TOGGLE
        val darkModeBtn = findViewById<Button>(R.id.darkModeBtn)
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        darkModeBtn.text = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) "☀️" else "🌙"
        darkModeBtn.setOnClickListener {
            val isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            if (isNight) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                darkModeBtn.text = "🌙"
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                darkModeBtn.text = "☀️"
            }
        }

        sendBtn = findViewById(R.id.sendBtn)
        micBtn = findViewById(R.id.micBtn)
        reminderBtn = findViewById(R.id.reminderBtn)
        diaryBtn = findViewById(R.id.diaryBtn)

        // AI VOICE
        tts = TextToSpeech(this) {
            tts.language = Locale.US
        }

        // MORNING BRIEFING
        val briefing = "Good morning ☀️ Stay productive today."
        chatView.append("\n🌤 $briefing\n")
        tts.speak(briefing, TextToSpeech.QUEUE_FLUSH, null, null)

        // SEND MESSAGE
        sendBtn.setOnClickListener {
            val msg = inputText.text.toString()
            if (msg.isNotEmpty()) {
                chatView.append("\n🧑 You: $msg\n")
                inputText.setText("")
                sendToAI(msg)
            }
        }

        // MIC INPUT
        micBtn.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        }

        // REMINDER
        reminderBtn.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                try {
                    val intent = Intent(this, ReminderReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                    val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    val timeStr = String.format("%02d:%02d", hour, minute)
                    chatView.append("\n🔔 Reminder set for $timeStr\n")
                    tts.speak("Reminder set for $timeStr", TextToSpeech.QUEUE_FLUSH, null, null)
                } catch (e: Exception) {
                    chatView.append("\n❌ Reminder Error\n")
                }
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        // OPEN DIARY
        diaryBtn.setOnClickListener {
            startActivity(Intent(this, DiaryActivity::class.java))
        }
    }

    private fun sendToAI(message: String) {
        val url = "https://YOUR-NGROK-URL.ngrok-free.app/chat"
        val json = JSONObject()
        json.put("message", message)
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("ngrok-skip-browser-warning", "true")
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { chatView.append("\n❌ Error: ${e.message}\n") }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseStr = response.body?.string() ?: "{}"
                    val json = JSONObject(responseStr)
                    val intent = json.optString("intent", "chat")
                    val reply = json.optString("reply", "...")
                    val data = json.optJSONObject("data")

                    runOnUiThread {
                        chatView.append("\n🤖 AI: $reply\n")
                        tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null)
                    }

                    when (intent) {
                        "set_reminder" -> {
                            val hour = data?.optInt("hour", -1) ?: -1
                            val minute = data?.optInt("minute", 0) ?: 0
                            if (hour != -1) setReminderFromAI(hour, minute)
                        }
                        "save_note" -> {
                            val note = data?.optString("note", "") ?: ""
                            val status = data?.optString("status", "pending") ?: "pending"
                            val category = data?.optString("category", "🏠 Personal") ?: "🏠 Personal"
                            if (note.isNotEmpty()) saveNoteFromAI(note, status, category)
                        }
                        "get_tasks" -> {
                            val tasks = DatabaseHelper(this@MainActivity).getAllNotes()
                            val summary = if (tasks.isEmpty()) "No tasks saved yet."
                            else tasks.joinToString("\n") { "${if (it.status == "pending") "📌" else "✅"} ${it.note}" }
                            runOnUiThread { chatView.append("\n📋 Your Tasks:\n$summary\n") }
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread { chatView.append("\n❌ Error: ${e.message}\n") }
                }
            }
        })
    }

    private fun setReminderFromAI(hour: Int, minute: Int) {
        try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            val intent = Intent(this, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            val timeStr = String.format("%02d:%02d", hour, minute)
            runOnUiThread { chatView.append("\n🔔 Reminder set for $timeStr\n") }
        } catch (e: Exception) {
            runOnUiThread { chatView.append("\n❌ Reminder Error\n") }
        }
    }

    private fun saveNoteFromAI(note: String, status: String, category: String) {
        DatabaseHelper(this).insertNote(note, status, category)
        runOnUiThread {
            chatView.append("\n💾 Saved: $note [$category]\n")
            refreshDashboard()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()
    }

    private fun refreshDashboard() {
        val tasks = DatabaseHelper(this).getAllNotes()
        pendingCount.text = tasks.count { it.status == "pending" }.toString()
        completedCount.text = tasks.count { it.status == "completed" }.toString()
        totalCount.text = tasks.size.toString()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            inputText.setText(result?.get(0))
        }
    }
}