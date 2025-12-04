package com.github.intent.pad.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.github.intent.pad.MainActivity
import com.github.intent.pad.data.ShortcutEntity

object ShortcutUtils {

    fun sendBroadcast(context: Context, action: String) {
        val intent = Intent(action)
        context.sendBroadcast(intent)
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun pinShortcut(context: Context, shortcutEntity: ShortcutEntity) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        
        val shortcut = ShortcutInfo.Builder(context, "shortcut_${shortcutEntity.id}")
            .setShortLabel(shortcutEntity.label)
            .setLongLabel(shortcutEntity.label)
            .setIcon(Icon.createWithResource(context, android.R.drawable.ic_dialog_info))
            .setIntent(
                Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("shortcut_id", shortcutEntity.id)
                }
            )
            .build()
        
        shortcutManager?.requestPinShortcut(shortcut, null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createDynamicShortcut(context: Context, shortcutEntity: ShortcutEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            
            val shortcut = ShortcutInfo.Builder(context, "dynamic_${shortcutEntity.id}")
                .setShortLabel(shortcutEntity.label)
                .setLongLabel(shortcutEntity.label)
                .setIcon(Icon.createWithResource(context, android.R.drawable.ic_dialog_info))
                .setIntent(
                    Intent(context, MainActivity::class.java).apply {
                        action = "com.github.intent.pad.ACTION_SHORTCUT"
                        putExtra("shortcut_id", shortcutEntity.id)
                    }
                )
                .build()
            
            shortcutManager?.dynamicShortcuts = listOf(shortcut)
        }
    }

    fun isShortcutPinnedSupported(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
               context.getSystemService(ShortcutManager::class.java)?.isRequestPinShortcutSupported == true
    }
}
