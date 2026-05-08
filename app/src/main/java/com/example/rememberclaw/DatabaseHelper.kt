package com.example.rememberclaw

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "DiaryDB", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE diary (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "note TEXT," +
                    "status TEXT," +
                    "category TEXT," +
                    "timestamp INTEGER)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS diary")
        onCreate(db)
    }

    fun insertNote(note: String, status: String, category: String) {
        val db = writableDatabase
        val values = ContentValues()
        values.put("note", note)
        values.put("status", status)
        values.put("category", category)
        values.put("timestamp", System.currentTimeMillis())
        db.insert("diary", null, values)
    }

    fun deleteNote(id: Int) {
        val db = writableDatabase
        db.delete("diary", "id = ?", arrayOf(id.toString()))
    }

    fun updateNote(id: Int, note: String, category: String) {
        val db = writableDatabase
        val values = ContentValues()
        values.put("note", note)
        values.put("category", category)
        db.update("diary", values, "id = ?", arrayOf(id.toString()))
    }

    fun getAllNotes(): List<NoteItem> {
        val list = mutableListOf<NoteItem>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, note, status, category, timestamp FROM diary ORDER BY timestamp DESC", null
        )
        while (cursor.moveToNext()) {
            list.add(
                NoteItem(
                    id = cursor.getInt(0),
                    note = cursor.getString(1),
                    status = cursor.getString(2),
                    category = cursor.getString(3),
                    timestamp = cursor.getLong(4)
                )
            )
        }
        cursor.close()
        return list
    }
}

data class NoteItem(
    val id: Int,
    val note: String,
    val status: String,
    val category: String,
    val timestamp: Long
)