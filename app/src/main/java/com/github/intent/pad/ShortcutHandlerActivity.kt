package com.github.intent.pad

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class ShortcutHandlerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val action = intent?.action
        val data = intent?.data
        
        // ここでショートカットから起動された時の処理を行う
        // 必要に応じてMainActivityを起動
        
        val mainIntent = Intent(this, MainActivity::class.java)
        startActivity(mainIntent)
        finish()
    }
}
