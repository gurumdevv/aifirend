package com.capstone.aifirend

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

class ChatDAO(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private val SQL_CREATE = "CREATE TABLE chat_history (id INTEGER PRIMARY KEY, type TEXT, message TEXT)"
    private val SQL_DROP = "DROP TABLE IF EXISTS chat_history"

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DROP)
        onCreate(db)
    }
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "ChatLog.db"
    }

    public  fun DropChat()
    {
        writableDatabase?.delete(
            "chat_history",
            null,
            null
        )
    }

    public fun InsertChat(item: MessageRVModal)
    {
        writableDatabase?.insert("chat_history", null,
            ContentValues().apply {
                put("type", item.sender)
                put("message", item.message)
            }
        )
    }

    public fun FetchAllChat(): ArrayList<MessageRVModal>
    {
        var cursor = readableDatabase.query(
            "chat_history",
            arrayOf("id", "type", "message"),
            null,
            null,
            null,
            null,
            "id ASC"
        )

        var result = arrayListOf<MessageRVModal>()

        with (cursor)
        {
            while (cursor.moveToNext())
            {
                var item: MessageRVModal = MessageRVModal(getString(2), getString(1))
                result.add(item)
            }
        }

        return result
    }
}