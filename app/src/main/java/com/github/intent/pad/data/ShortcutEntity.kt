package com.github.intent.pad.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String = "",
    val actionName: String = "",
    val iconEmoji: String = "🚀",
    val colorHex: Long = 0xFF1E88E5,
    // 新機能: 画像アイコン用
    val imageIconUri: String? = null,
    // 新機能: トグル機能用
    val isToggle: Boolean = false,
    val secondaryActionName: String? = null,
    val isActive: Boolean = true,
    // 新機能: レイアウト設定用
    val layoutColumns: Int = 2
)
