package com.example.rememberclaw

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class DiaryActivity : AppCompatActivity() {

    private lateinit var noteInput: EditText
    private lateinit var micBtn: Button
    private lateinit var savePendingBtn: Button
    private lateinit var saveCompletedBtn: Button
    private lateinit var askBtn: Button
    private lateinit var categorySpinner: Spinner
    private lateinit var pendingContainer: LinearLayout
    private lateinit var completedContainer: LinearLayout
    private lateinit var noPendingText: TextView
    private lateinit var noCompletedText: TextView
    private lateinit var summaryText: TextView
    private lateinit var pendingBadge: TextView
    private lateinit var completedBadge: TextView

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tts: TextToSpeech

    private val SPEECH_REQUEST_CODE = 200
    private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    private var editingId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary)

        noteInput = findViewById(R.id.noteInput)
        micBtn = findViewById(R.id.micBtn)
        savePendingBtn = findViewById(R.id.savePendingBtn)
        saveCompletedBtn = findViewById(R.id.saveCompletedBtn)
        askBtn = findViewById(R.id.askBtn)
        categorySpinner = findViewById(R.id.categorySpinner)
        pendingContainer = findViewById(R.id.pendingContainer)
        completedContainer = findViewById(R.id.completedContainer)
        noPendingText = findViewById(R.id.noPendingText)
        noCompletedText = findViewById(R.id.noCompletedText)
        summaryText = findViewById(R.id.summaryText)
        pendingBadge = findViewById(R.id.pendingBadge)
        completedBadge = findViewById(R.id.completedBadge)

        val categories = listOf("🏠 Personal", "💼 Work", "📚 Study", "💪 Health", "💰 Finance", "🎯 Goals")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        dbHelper = DatabaseHelper(this)
        tts = TextToSpeech(this) { tts.language = Locale.US }

        loadNotes()

        micBtn.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        }

        savePendingBtn.setOnClickListener {
            val note = noteInput.text.toString()
            val category = categorySpinner.selectedItem.toString()
            if (note.isNotEmpty()) {
                if (editingId != -1) {
                    dbHelper.updateNote(editingId, note, category)
                    editingId = -1
                    resetButtons()
                } else {
                    dbHelper.insertNote(note, "pending", category)
                }
                loadNotes()
                tts.speak("Task saved", TextToSpeech.QUEUE_FLUSH, null, null)
                noteInput.setText("")
            }
        }

        saveCompletedBtn.setOnClickListener {
            val note = noteInput.text.toString()
            val category = categorySpinner.selectedItem.toString()
            if (note.isNotEmpty()) {
                if (editingId != -1) {
                    dbHelper.updateNote(editingId, note, category)
                    editingId = -1
                    resetButtons()
                } else {
                    dbHelper.insertNote(note, "completed", category)
                }
                loadNotes()
                tts.speak("Task saved", TextToSpeech.QUEUE_FLUSH, null, null)
                noteInput.setText("")
            }
        }

        askBtn.setOnClickListener {
            val question = noteInput.text.toString().lowercase()
            val tasks = dbHelper.getAllNotes()
            var answer = ""

            if (question.contains("pending") || question.contains("not done")) {
                val p = tasks.filter { it.status == "pending" }
                answer = if (p.isNotEmpty()) "Pending: " + p.joinToString(", ") { it.note } else "No pending tasks!"
            } else if (question.contains("completed") || question.contains("done")) {
                val c = tasks.filter { it.status == "completed" }
                answer = if (c.isNotEmpty()) "Completed: " + c.joinToString(", ") { it.note } else "No completed tasks!"
            } else {
                for (task in tasks) {
                    for (word in question.split(" ")) {
                        if (word.length > 3 && task.note.lowercase().contains(word)) {
                            answer = "I remember: ${task.note}"; break
                        }
                    }
                    if (answer.isNotEmpty()) break
                }
                if (answer.isEmpty()) answer = if (tasks.isNotEmpty()) "You have ${tasks.size} memories." else "No memories yet!"
            }

            Toast.makeText(this, "🧠 $answer", Toast.LENGTH_LONG).show()
            tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, null)
            noteInput.setText("")
        }
    }

    private fun loadNotes() {
        val tasks = dbHelper.getAllNotes()
        val pending = tasks.filter { it.status == "pending" }
        val completed = tasks.filter { it.status == "completed" }

        summaryText.text = "${pending.size} pending • ${completed.size} completed"
        pendingBadge.text = pending.size.toString()
        completedBadge.text = completed.size.toString()

        pendingContainer.removeAllViews()
        if (pending.isEmpty()) {
            noPendingText.visibility = TextView.VISIBLE
        } else {
            noPendingText.visibility = TextView.GONE
            pending.forEachIndexed { index, task ->
                val card = createTaskCard(task, "#D4854A", "#2A1A0A")
                val anim = AnimationUtils.loadAnimation(this, R.anim.fade_in)
                anim.startOffset = (index * 80).toLong()
                card.startAnimation(anim)
                pendingContainer.addView(card)
            }
        }

        completedContainer.removeAllViews()
        if (completed.isEmpty()) {
            noCompletedText.visibility = TextView.VISIBLE
        } else {
            noCompletedText.visibility = TextView.GONE
            completed.forEachIndexed { index, task ->
                val card = createTaskCard(task, "#5A9B57", "#0A1A0A")
                val anim = AnimationUtils.loadAnimation(this, R.anim.fade_in)
                anim.startOffset = (index * 80).toLong()
                card.startAnimation(anim)
                completedContainer.addView(card)
            }
        }
    }

    private fun createTaskCard(task: NoteItem, accentHex: String, bgHex: String): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(16, 14, 16, 14)
        card.setBackgroundResource(R.drawable.card_background)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = 10
        card.layoutParams = params

        // Top row
        val topRow = LinearLayout(this)
        topRow.orientation = LinearLayout.HORIZONTAL

        val categoryTag = TextView(this)
        categoryTag.text = task.category
        categoryTag.textSize = 11f
        categoryTag.setTextColor(Color.parseColor(accentHex))
        categoryTag.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val timeTag = TextView(this)
        timeTag.text = dateFormat.format(Date(task.timestamp))
        timeTag.textSize = 10f
        timeTag.setTextColor(Color.parseColor("#9A8468"))

        topRow.addView(categoryTag)
        topRow.addView(timeTag)

        // Note text
        val noteText = TextView(this)
        noteText.text = task.note
        noteText.textSize = 15f
        noteText.setTextColor(Color.parseColor("#F0DFC0"))
        noteText.setPadding(0, 6, 0, 0)

        card.addView(topRow)
        card.addView(noteText)

        // TAP = edit
        card.setOnClickListener {
            noteInput.setText(task.note)
            editingId = task.id
            savePendingBtn.text = "💾 Update"
            saveCompletedBtn.text = "💾 Update"
            val anim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
            noteInput.startAnimation(anim)
        }

        // LONG PRESS = delete
        card.setOnLongClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete this memory?")
                .setMessage(task.note)
                .setPositiveButton("Delete") { _, _ ->
                    dbHelper.deleteNote(task.id)
                    loadNotes()
                    tts.speak("Memory deleted", TextToSpeech.QUEUE_FLUSH, null, null)
                }
                .setNegativeButton("Keep", null)
                .show()
            true
        }

        return card
    }

    private fun resetButtons() {
        savePendingBtn.text = "📌 Pending"
        saveCompletedBtn.text = "✅ Done"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            noteInput.setText(result?.get(0))
        }
    }
}