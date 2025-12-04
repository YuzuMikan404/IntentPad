package com.github.intent.pad.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val actionName: String,
    val iconEmoji: String,
    val colorHex: Long,
    val imageIconUri: String? = null,
    val isToggle: Boolean = false,
    val secondaryActionName: String? = null,
    val isActive: Boolean = true
)

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts ORDER BY id DESC")
    fun getAll(): Flow<List<ShortcutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shortcut: ShortcutEntity)

    @Delete
    suspend fun delete(shortcut: ShortcutEntity)

    @Query("DELETE FROM shortcuts")
    suspend fun deleteAll()
}

@Database(entities = [ShortcutEntity::class], version = 2, exportSchema = false)
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
                )
                .fallbackToDestructiveMigration() // バージョン変更時にDBを再作成
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
