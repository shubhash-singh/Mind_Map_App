package com.ragnar.eduapp.data.repository

class DBHelper {
}


/*
package com.ragnar.eduapp.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Simple SQLite database for users and chat messages.
 */
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "eduapp.db"
        private const val DATABASE_VERSION = 1

        // Table names
        const val TABLE_USER = "users"
        const val TABLE_CHAT = "chats"

        // User columns
        const val USER_ID = "id"
        const val USER_NAME = "name"
        const val USER_EMAIL = "email"

        // Chat columns
        const val CHAT_ID = "id"
        const val CHAT_USER_ID = "user_id"
        const val CHAT_USER_MESSAGE = "user_message"
        const val CHAT_AI_RESPONSE = "ai_response"
        const val CHAT_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUserTable = """
            CREATE TABLE $TABLE_USER (
                $USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $USER_NAME TEXT NOT NULL,
                $USER_EMAIL TEXT UNIQUE NOT NULL
            )
        """.trimIndent()

        val createChatTable = """
            CREATE TABLE $TABLE_CHAT (
                $CHAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $CHAT_USER_ID INTEGER NOT NULL,
                $CHAT_USER_MESSAGE TEXT,
                $CHAT_AI_RESPONSE TEXT,
                $CHAT_TIMESTAMP INTEGER DEFAULT (strftime('%s','now')),
                FOREIGN KEY($CHAT_USER_ID) REFERENCES $TABLE_USER($USER_ID)
            )
        """.trimIndent()

        db.execSQL(createUserTable)
        db.execSQL(createChatTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHAT")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        onCreate(db)
    }

    // ------------------------
    //  User Helper Methods
    // ------------------------

    fun addUser(name: String, email: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(USER_NAME, name)
            put(USER_EMAIL, email)
        }
        return db.insert(TABLE_USER, null, values)
    }

    fun getUserByEmail(email: String): Cursor {
        val db = readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_USER WHERE $USER_EMAIL = ?", arrayOf(email))
    }

    fun getAllUsers(): Cursor {
        val db = readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_USER", null)
    }

    fun deleteUser(email: String): Int {
        val db = writableDatabase
        return db.delete(TABLE_USER, "$USER_EMAIL = ?", arrayOf(email))
    }

    // ------------------------
    //  Chat Helper Methods
    // ------------------------

    fun addChat(userId: Int, userMessage: String, aiResponse: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(CHAT_USER_ID, userId)
            put(CHAT_USER_MESSAGE, userMessage)
            put(CHAT_AI_RESPONSE, aiResponse)
        }
        return db.insert(TABLE_CHAT, null, values)
    }

    fun getChatsByUserId(userId: Int): Cursor {
        val db = readableDatabase
        return db.rawQuery(
            "SELECT * FROM $TABLE_CHAT WHERE $CHAT_USER_ID = ? ORDER BY $CHAT_TIMESTAMP ASC",
            arrayOf(userId.toString())
        )
    }

    fun clearChatsForUser(userId: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_CHAT, "$CHAT_USER_ID = ?", arrayOf(userId.toString()))
    }

    fun clearAllChats(): Int {
        val db = writableDatabase
        return db.delete(TABLE_CHAT, null, null)
    }
}

 */