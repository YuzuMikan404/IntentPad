package com.github.intent.pad

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput // ★追加
import androidx.compose.ui.platform.LocalContext // ★追加
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.intent.pad.data.AppDatabase
import com.github.intent.pad.data.ShortcutEntity
import com.github.intent.pad.utils.ShortcutUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)
        val dao = db.shortcutDao()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val shortcuts by dao.getAll().collectAsState(initial = emptyList())
                    val scope = rememberCoroutineScope()
                    MainScreen(
                        shortcuts = shortcuts,
                        onAdd = { scope.launch(Dispatchers.IO) { dao.insert(it) } },
                        onUpdate = { scope.launch(Dispatchers.IO) { dao.insert(it) } }, // RoomはidがあればUpdate, なければInsert
                        onDel = { scope.launch(Dispatchers.IO) { dao.delete(it) } },
                        onPin = { ShortcutUtils.pinShortcut(this, it); Toast.makeText(this, "ホームに追加", Toast.LENGTH_SHORT).show() },
                        onTest = { ShortcutUtils.sendBroadcast(this, it.actionName); Toast.makeText(this, "インテント送信: ${it.label}", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    shortcuts: List<ShortcutEntity>,
    onAdd: (ShortcutEntity) -> Unit,
    onUpdate: (ShortcutEntity) -> Unit, // ★変更
    onDel: (ShortcutEntity) -> Unit,
    onPin: (ShortcutEntity) -> Unit,
    onTest: (ShortcutEntity) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ShortcutEntity?>(null) } // ★編集対象

    Scaffold(
        topBar = { TopAppBar(title = { Text("インテントパッド") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) },
        floatingActionButton = { FloatingActionButton(onClick = { showDialog = true }) { Icon(Icons.Default.Add, "追加") } }
    ) { padding ->
        if (shortcuts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("＋ボタンでトリガーを作成", style = MaterialTheme.typography.bodyLarge) }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(140.dp), contentPadding = PaddingValues(16.dp), modifier = Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shortcuts) { item ->
                    ShortcutCard(
                        item = item,
                        onTest = { onTest(item) },
                        onEdit = { itemToEdit = item }, // ★ダブルタップで編集対象をセット
                        onPin = { onPin(item) },
                        onDel = { onDel(item) }
                    )
                }
            }
        }

        // 新規作成 or 編集ダイアログを表示
        if (showDialog || itemToEdit != null) {
            EditDialog(
                item = itemToEdit, // nullなら新規作成、あれば編集
                onDismiss = { 
                    showDialog = false
                    itemToEdit = null
                },
                onSave = { newItem ->
                    if (itemToEdit == null) {
                        onAdd(newItem)
                    } else {
                        onUpdate(newItem)
                    }
                    showDialog = false
                    itemToEdit = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortcutCard(item: ShortcutEntity, onTest: () -> Unit, onEdit: () -> Unit, onPin: () -> Unit, onDel: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current // トースト表示に必要

    Box {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(item.colorHex)), 
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth()
                .pointerInput(Unit) { // ★タップイベントのカスタム処理
                    detectTapGestures(
                        onTap = { onTest() }, // シングルタップでテスト
                        onLongPress = { showMenu = true }, // 長押しでメニュー
                        onDoubleTap = { onEdit() } // ★ダブルタップで編集
                    )
                }
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(item.iconEmoji, style = MaterialTheme.typography.displayMedium)
                Text(item.label, style = MaterialTheme.typography.titleMedium, color = Color.White, textAlign = TextAlign.Center)
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("ホームに追加") }, onClick = { onPin(); showMenu = false })
            DropdownMenuItem(text = { Text("編集 (ダブルタップでも可)") }, onClick = { onEdit(); showMenu = false }) // ★編集メニューを追加
            DropdownMenuItem(text = { Text("削除") }, onClick = { onDel(); showMenu = false }, leadingIcon = { Icon(Icons.Default.Delete, null) })
        }
    }
}

// ★item: ShortcutEntity? を受け取るように変更
@Composable
fun EditDialog(item: ShortcutEntity?, onDismiss: () -> Unit, onSave: (ShortcutEntity) -> Unit) {
    // 既存の値を初期値としてセット
    var name by remember { mutableStateOf(item?.label ?: "") }
    var action by remember { mutableStateOf(item?.actionName ?: "") }
    var emoji by remember { mutableStateOf(item?.iconEmoji ?: "🚀") }
    val colors = listOf(0xFF1E88E5, 0xFFD81B60, 0xFF43A047, 0xFFFB8C00, 0xFF8E24AA, 0xFF546E7A)
    var selColor by remember { mutableStateOf(item?.colorHex ?: colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "新規トリガー作成" else "トリガーを編集") }, // ★日本語化
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("表示名") }) // ★日本語化
                OutlinedTextField(action, { action = it }, label = { Text("インテントアクション名") }) // ★日本語化
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedTextField(emoji, { if(it.length<=2) emoji=it }, label = { Text("アイコン") }, modifier = Modifier.width(100.dp)) // ★日本語化
                    Row { colors.forEach { c -> Box(Modifier.size(32.dp).clip(CircleShape).background(Color(c)).combinedClickable{ selColor = c }) } }
                }
            }
        },
        confirmButton = { 
            Button(onClick = { 
                if(name.isNotBlank() && action.isNotBlank()) {
                    // 編集の場合、元のIDを保持して保存
                    onSave(ShortcutEntity(id = item?.id ?: 0, label = name, actionName = action, iconEmoji = emoji, colorHex = selColor))
                } else {
                    Toast.makeText(LocalContext.current, "表示名とアクション名は必須です。", Toast.LENGTH_SHORT).show()
                }
            }) { Text(if (item == null) "作成" else "保存") } // ★日本語化
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } } // ★日本語化
    )
}
