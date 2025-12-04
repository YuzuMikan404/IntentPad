    }

    private fun createIconBitmap(text: String, color: Int): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawCircle(size/2f, size/2f, size/2f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = size * 0.5f
        paint.textAlign = Paint.Align.CENTER
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        canvas.drawText(text, size/2f, (size/2f) + (bounds.height()/2f)/1.5f, paint)
        return bitmap
    }
}
package com.github.intent.pad.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.github.intent.pad.ShortcutHandlerActivity
import com.github.intent.pad.data.ShortcutEntity

object ShortcutUtils {
    fun sendBroadcast(context: Context, action: String) {
        val intent = Intent(action)
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        context.sendBroadcast(intent)
    }

    fun pinShortcut(context: Context, item: ShortcutEntity) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            val targetIntent = Intent(context, ShortcutHandlerActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                putExtra("TARGET_ACTION", item.actionName)
                data = android.net.Uri.parse("intentpad://${item.id}")
            }
            val iconBitmap = createIconBitmap(item.iconEmoji, item.colorHex.toInt())
            val shortcutInfo = ShortcutInfoCompat.Builder(context, "pad_${item.id}")
                .setShortLabel(item.label)
                .setIcon(IconCompat.createWithBitmap(iconBitmap))
                .setIntent(targetIntent)
                .build()
            ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
        }
    }

    private fun createIconBitmap(text: String, color: Int): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawCircle(size/2f, size/2f, size/2f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = size * 0.5f
        paint.textAlign = Paint.Align.CENTER
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        canvas.drawText(text, size/2f, (size/2f) + (bounds.height()/2f)/1.5f, paint)
        return bitmap
    }
}
