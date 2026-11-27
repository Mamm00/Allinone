package com.hyperlauncher.ai.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteOpenHelper

/**
 * SQLCipher-encrypted vault (AES-256). The database file is encrypted at rest;
 * the passphrase is never stored — only a SHA-256 hash is kept in
 * EncryptedSharedPreferences to verify unlock attempts.
 *
 * vault_entries table: id, title, body, source_package, created_at
 */
class VaultHelper(context: Context) {

    private var helper: VaultOpenHelper? = null
    private var db: SQLiteDatabase? = null

    private val appContext = context.applicationContext

    @Synchronized
    fun unlock(passphrase: String): Boolean {
        if (passphrase.length < VaultCrypto.MIN_PASSPHRASE_LENGTH) return false
        return try {
            close()
            SQLiteDatabase.loadLibs(appContext)
            helper = VaultOpenHelper(appContext)
            db = helper!!.getWritableDatabase(passphrase)
            true
        } catch (e: Exception) {
            close()
            false
        }
    }

    @Synchronized
    fun isUnlocked(): Boolean = db != null

    @Synchronized
    fun lock() {
        close()
    }

    @Synchronized
    private fun close() {
        try { db?.close() } catch (_: Exception) {}
        try { helper?.close() } catch (_: Exception) {}
        db = null
        helper = null
    }

    @Synchronized
    fun insertNote(title: String, body: String, sourcePackage: String?): Long {
        val database = db ?: return -1
        val values = ContentValues().apply {
            put("title", title)
            put("body", body)
            put("source_package", sourcePackage)
            put("created_at", System.currentTimeMillis())
        }
        return database.insert("vault_entries", null, values)
    }

    @Synchronized
    fun getAllNotes(): List<VaultNote> {
        val database = db ?: return emptyList()
        val notes = mutableListOf<VaultNote>()
        var cursor: Cursor? = null
        try {
            cursor = database.query(
                "vault_entries", null, null, null, null, null, "created_at DESC"
            )
            while (cursor.moveToNext()) {
                notes.add(
                    VaultNote(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: "",
                        body = cursor.getString(cursor.getColumnIndexOrThrow("body")) ?: "",
                        sourcePackage = cursor.getString(cursor.getColumnIndexOrThrow("source_package")),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
        } finally {
            cursor?.close()
        }
        return notes
    }

    @Synchronized
    fun deleteNote(id: Long): Int {
        val database = db ?: return 0
        return database.delete("vault_entries", "id = ?", arrayOf(id.toString()))
    }

    @Synchronized
    fun clearAll(): Int {
        val database = db ?: return 0
        return database.delete("vault_entries", null, null)
    }

}

data class VaultNote(
    val id: Long,
    val title: String,
    val body: String,
    val sourcePackage: String?,
    val createdAt: Long
)

private class VaultOpenHelper(
    context: Context
) : SQLiteOpenHelper(context, "vault.db", null, 1, null) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS vault_entries (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "body TEXT NOT NULL, " +
                "source_package TEXT, " +
                "created_at INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS vault_entries")
        onCreate(db)
    }
}
