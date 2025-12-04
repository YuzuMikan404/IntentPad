package com.github.intent.pad.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts ORDER BY id DESC")
    fun getAll(): Flow<List<ShortcutEntity>>

    @Insert suspend fun insert(shortcut: ShortcutEntity)
    @Delete suspend fun delete(shortcut: ShortcutEntity)
}

@Database(entities = [ShortcutEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shortcutDao(): ShortcutDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "intent_pad_db").build().also { INSTANCE = it }
            }
        }
    }
}
