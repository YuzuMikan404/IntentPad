package com.github.intent.pad.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val actionName: String,
    val iconEmoji: String,
    val colorHex: Long
)

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts ORDER BY id DESC")
    fun getAll(): Flow<List<ShortcutEntity>>

    @Insert
    suspend fun insert(shortcut: ShortcutEntity)

    @Delete
    suspend fun delete(shortcut: ShortcutEntity)
}

@Database(entities = [ShortcutEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shortcutDao(): ShortcutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "intent_pad_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
