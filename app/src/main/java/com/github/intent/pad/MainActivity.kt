@file:OptIn(ExperimentalFoundationApi::class)

package com.github.intent.pad

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.github.intent.pad.data.AppDatabase
import com.github.intent.pad.data.ShortcutEntity
import com.github.intent.pad.utils.ShortcutUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)
        val dao = db.shortcutDao()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val shortcuts by dao.getAll().collectAsState(initial = emptyList())
                    val scope = rememberCoroutineScope()
                    var gridColumns by remember { mutableStateOf(2) }
                    
                    MainScreen(
                        shortcuts = shortcuts,
                        gridColumns = gridColumns,
                        onGridColumnsChange = { gridColumns = it },
                        onAdd = { scope.launch(Dispatchers.IO) { dao.insert(it) } },
                        onUpdate = { scope.launch(Dispatchers.IO) { dao.insert(it) } },
                        onDel = { scope.launch(Dispatchers.IO) { dao.delete(it) } },
                        onDeleteAll = { scope.launch(Dispatchers.IO) { dao.deleteAll() } },
                        onPin = { ShortcutUtils.pinShortcut(this, it) },
                        onTest = { entity ->
                            if (entity.isToggle) {
                                // トグル機能
                                val actionToSend = if (entity.isActive) {
                                    entity.actionName
                                } else {
                                    entity.secondaryActionName ?: entity.actionName
                                }
                                ShortcutUtils.sendBroadcast(this, actionToSend)
                                // 状態を反転
                                val updated = entity.copy(isActive = !entity.isActive)
                                scope.launch(Dispatchers.IO) { dao.insert(updated) }
                            } else {
                                // 通常機能
                                ShortcutUtils.sendBroadcast(this, entity.actionName)
                            }
                        }
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
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit,
    onAdd: (ShortcutEntity) -> Unit,
    onUpdate: (ShortcutEntity) -> Unit,
    onDel: (ShortcutEntity) -> Unit,
    onDeleteAll: () -> Unit,
    onPin: (ShortcutEntity) -> Unit,
    onTest: (ShortcutEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ShortcutEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("インテントパッド") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    // 設定ボタン
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                    // メニューボタン
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "メニュー")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("全削除") },
                            onClick = {
                                onDeleteAll()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.DeleteForever, null) }
                        )
                    }
                }
            )
        },
        floatingActionButton = { 
            FloatingActionButton(onClick = { showDialog = true }) { 
                Icon(Icons.Default.Add, "追加") 
            } 
        }
    ) { padding ->
        if (shortcuts.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding), 
                contentAlignment = Alignment.Center
            ) { 
                Text("＋ボタンでトリガーを作成", style = MaterialTheme.typography.bodyLarge) 
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(shortcuts) { item ->
                    ShortcutCard(
                        item = item,
                        onTest = { onTest(item) },
                        onEdit = { itemToEdit = item },
                        onPin = { onPin(item) },
                        onDel = { onDel(item) }
                    )
                }
            }
        }

        // 編集ダイアログ
        if (showDialog || itemToEdit != null) {
            EditDialog(
                item = itemToEdit,
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

        // 設定ダイアログ
        if (showSettings) {
            SettingsDialog(
                gridColumns = gridColumns,
                onGridColumnsChange = onGridColumnsChange,
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
fun ShortcutCard(
    item: ShortcutEntity,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onDel: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(item.colorHex)),
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTest() },
                        onLongPress = { showMenu = true },
                        onDoubleTap = { onEdit() }
                    )
                }
        ) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 画像アイコンか絵文字かを表示
                if (!item.imageIconUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = item.imageIconUri,
                        contentDescription = "アイコン",
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Text(item.iconEmoji, style = MaterialTheme.typography.displayMedium)
                }
                
                Text(
                    item.label, 
                    style = MaterialTheme.typography.titleMedium, 
                    color = Color.White, 
                    textAlign = TextAlign.Center
                )
                
                // トグル機能の状態表示
                if (item.isToggle) {
                    Text(
                        if (item.isActive) "ON" else "OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("ホームに追加") }, 
                onClick = { onPin(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("編集 (ダブルタップでも可)") }, 
                onClick = { onEdit(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("削除") }, 
                onClick = { onDel(); showMenu = false },
                leadingIcon = { Icon(Icons.Default.Delete, null) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditDialog(
    item: ShortcutEntity?,
    onDismiss: () -> Unit,
    onSave: (ShortcutEntity) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = item != null

    // 状態管理
    var name by remember { mutableStateOf(item?.label ?: "") }
    var action by remember { mutableStateOf(item?.actionName ?: "") }
    var emoji by remember { mutableStateOf(item?.iconEmoji ?: "🚀") }
    var imageUri by remember { mutableStateOf<String?>(item?.imageIconUri) }
    var isToggle by remember { mutableStateOf(item?.isToggle ?: false) }
    var secondaryAction by remember { mutableStateOf(item?.secondaryActionName ?: "") }
    
    val colors = listOf(0xFF1E88E5, 0xFFD81B60, 0xFF43A047, 0xFFFB8C00, 0xFF8E24AA, 0xFF546E7A)
    var selColor by remember { mutableStateOf(item?.colorHex ?: colors[0]) }

    // 画像選択ランチャー
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it.toString()
            emoji = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditMode) "トリガーを編集" else "新規トリガー作成") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 基本設定
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("表示名*") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = action,
                    onValueChange = { action = it },
                    label = { Text("インテントアクション*") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // トグル機能
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isToggle,
                        onCheckedChange = { isToggle = it }
                    )
                    Text("トグル機能（ON/OFF切り替え）")
                }
                
                if (isToggle) {
                    OutlinedTextField(
                        value = secondaryAction,
                        onValueChange = { secondaryAction = it },
                        label = { Text("OFF時のアクション") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // アイコン設定
                Text("アイコン設定", style = MaterialTheme.typography.labelMedium)
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("画像を選択")
                    }
                    
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { if (it.length <= 2) emoji = it },
                        label = { Text("絵文字") },
                        modifier = Modifier.width(100.dp)
                    )
                }
                
                // アイコンプレビュー
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(selColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!imageUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "プレビュー",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (emoji.isNotBlank()) {
                            Text(emoji, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    
                    Text(
                        when {
                            !imageUri.isNullOrEmpty() -> "画像アイコン"
                            emoji.isNotBlank() -> "絵文字: $emoji"
                            else -> "アイコン未設定"
                        }
                    )
                }
                
                // 色選択
                Text("背景色", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    colors.forEach { c -> 
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .combinedClickable { selColor = c }
                                .then(
                                    if (selColor == c) Modifier.background(
                                        Color.Black.copy(alpha = 0.2f)
                                    ) else Modifier
                                )
                        )
                    } 
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && action.isNotBlank()) {
                        val newItem = ShortcutEntity(
                            id = item?.id ?: 0,
                            label = name,
                            actionName = action,
                            iconEmoji = emoji,
                            colorHex = selColor,
                            imageIconUri = imageUri,
                            isToggle = isToggle,
                            secondaryActionName = if (isToggle && secondaryAction.isNotBlank()) 
                                secondaryAction else null,
                            isActive = item?.isActive ?: true
                        )
                        onSave(newItem)
                    }
                }
            ) { 
                Text(if (isEditMode) "保存" else "作成") 
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("キャンセル") 
            } 
        }
    )
}

@Composable
fun SettingsDialog(
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("レイアウト設定") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("グリッド列数: $gridColumns")
                
                Slider(
                    value = gridColumns.toFloat(),
                    onValueChange = { onGridColumnsChange(it.toInt()) },
                    valueRange = 1f..4f,
                    steps = 2
                )
                
                // プレビュー
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(gridColumns) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(Color.Gray.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("適用")
            }
        }
    )
}import com.github.intent.pad.utils.ShortcutUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStreamReader

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
                        onUpdate = { scope.launch(Dispatchers.IO) { dao.insert(it) } },
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
    onUpdate: (ShortcutEntity) -> Unit,
    onDel: (ShortcutEntity) -> Unit,
    onPin: (ShortcutEntity) -> Unit,
    onTest: (ShortcutEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ShortcutEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    // データの書き出し（エクスポート）
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val json = Gson().toJson(shortcuts)
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray())
                    Toast.makeText(context, "データをエクスポートしました", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "エクスポート失敗: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // データの読み込み（インポート）
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = InputStreamReader(inputStream)
                    val type = object : TypeToken<List<ShortcutEntity>>() {}.type
                    val importedList: List<ShortcutEntity> = Gson().fromJson(reader, type)

                    scope.launch(Dispatchers.IO) {
                        importedList.forEach { item ->
                            // IDを0にリセットして新規データとして挿入
                            onAdd(item.copy(id = 0))
                        }
                        // メインスレッドでToastを表示
                        launch(Dispatchers.Main) { 
                            Toast.makeText(context, "${importedList.size}件のデータをインポートしました", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                // GsonParseExceptionやIOExceptionに対応
                Toast.makeText(context, "インポート失敗: ファイル形式を確認してください", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("インテントパッド") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "メニュー")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("エクスポート (JSON保存)") },
                            onClick = {
                                exportLauncher.launch("intent_pad_data.json")
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("インポート (JSON読み込み)") },
                            onClick = {
                                importLauncher.launch(arrayOf("application/json"))
                                showMenu = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showDialog = true }) { Icon(Icons.Default.Add, "追加") } }
    ) { padding ->
        if (shortcuts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("＋ボタンでトリガーを作成", style = MaterialTheme.typography.bodyLarge) }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(140.dp), contentPadding = PaddingValues(16.dp), modifier = Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shortcuts) { item ->
                    ShortcutCard(
                        item = item,
                        onTest = { onTest(item) },
                        onEdit = { itemToEdit = item },
                        onPin = { onPin(item) },
                        onDel = { onDel(item) }
                    )
                }
            }
        }

        if (showDialog || itemToEdit != null) {
            EditDialog(
                item = itemToEdit,
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

@Composable
fun ShortcutCard(item: ShortcutEntity, onTest: () -> Unit, onEdit: () -> Unit, onPin: () -> Unit, onDel: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(item.colorHex)),
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTest() },
                        onLongPress = { showMenu = true },
                        onDoubleTap = { onEdit() }
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
            DropdownMenuItem(text = { Text("編集 (ダブルタップでも可)") }, onClick = { onEdit(); showMenu = false })
            DropdownMenuItem(text = { Text("削除") }, onClick = { onDel(); showMenu = false }, leadingIcon = { Icon(Icons.Default.Delete, null) })
        }
    }
}

// ✅ 修正2: この関数の前に @OptIn アノテーションを追加
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditDialog(item: ShortcutEntity?, onDismiss: () -> Unit, onSave: (ShortcutEntity) -> Unit) {
    val context = LocalContext.current
    val isEditMode = item != null

    var name by remember { mutableStateOf(item?.label ?: "") }
    var action by remember { mutableStateOf(item?.actionName ?: "") }
    var emoji by remember { mutableStateOf(item?.iconEmoji ?: "🚀") }
    val colors = listOf(0xFF1E88E5, 0xFFD81B60, 0xFF43A047, 0xFFFB8C00, 0xFF8E24AA, 0xFF546E7A)
    var selColor by remember { mutableStateOf(item?.colorHex ?: colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditMode) "トリガーを編集" else "新規トリガー作成") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("表示名") })
                OutlinedTextField(action, { action = it }, label = { Text("インテントアクション名") })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(emoji, { if(it.length<=2) emoji=it }, label = { Text("アイコン") }, modifier = Modifier.width(100.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        colors.forEach { c -> 
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .combinedClickable { selColor = c }
                                    .then(
                                        if (selColor == c) Modifier.background(Color.Black.copy(alpha=0.2f)) else Modifier
                                    )
                            )
                        } 
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if(name.isNotBlank() && action.isNotBlank()) {
                    onSave(ShortcutEntity(
                        id = item?.id ?: 0, 
                        label = name, 
                        actionName = action, 
                        iconEmoji = emoji, 
                        colorHex = selColor
                    ))
                } else {
                    Toast.makeText(context, "表示名とアクション名は必須です。", Toast.LENGTH_SHORT).show()
                }
            }) { Text(if (isEditMode) "保存" else "作成") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}
