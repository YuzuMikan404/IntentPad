package com.github.intent.pad.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val actionName: String,
    val iconEmoji: String,
    val colorHex: Long
)
