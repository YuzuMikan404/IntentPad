package com.github.intent.pad

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.github.intent.pad.utils.ShortcutUtils

class ShortcutHandlerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionName = intent.getStringExtra("TARGET_ACTION")
        if (!actionName.isNullOrEmpty()) {
            ShortcutUtils.sendBroadcast(this, actionName)
            Toast.makeText(this, "Sent: $actionName", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
