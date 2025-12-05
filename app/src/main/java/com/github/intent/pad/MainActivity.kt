package com.github.intent.pad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.github.intent.pad.ui.theme.IntentPadTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            IntentPadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
        
        // 共有インテントの処理
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: android.content.Intent?) {
        // 共有インテントの処理ロジック
        intent?.let {
            when (it.action) {
                android.content.Intent.ACTION_SEND -> {
                    if (it.type == "text/plain") {
                        it.getStringExtra(android.content.Intent.EXTRA_TEXT)?.let { text ->
                            viewModel.addSharedText(text)
                        }
                    }
                }
            }
        }
    }
}
