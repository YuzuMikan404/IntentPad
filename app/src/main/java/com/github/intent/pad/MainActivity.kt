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
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
                    var gridColumns by remember { mutableStateOf(3) }
                    var gridSpacing by remember { mutableStateOf(12) }
                    var cardHeight by remember { mutableStateOf(140) }
                    
                    MainScreen(
                        shortcuts = shortcuts,
                        gridColumns = gridColumns,
                        gridSpacing = gridSpacing,
                        cardHeight = cardHeight,
                        onGridColumnsChange = { gridColumns = it },
                        onGridSpacingChange = { gridSpacing = it },
                        onCardHeightChange = { cardHeight = it },
                        onAdd = { scope.launch(Dispatchers.IO) { dao.insert(it) } },
                        onUpdate = { scope.launch(Dispatchers.IO) { dao.insert(it) } },
                        onDel = { scope.launch(Dispatchers.IO) { dao.delete(it) } },
                        onDeleteAll = {
                            scope.launch(Dispatchers.IO) {
                                dao.deleteAll()
                            }
                        },
                        onPin = { ShortcutUtils.pinShortcut(this, it) },
                        onTest = { entity ->
                            if (entity.isToggle) {
                                val actionToSend = if (entity.isActive) {
                                    entity.actionName
                                } else {
                                    entity.secondaryActionName ?: entity.actionName
                                }
                                ShortcutUtils.sendBroadcast(this, actionToSend)
                                val updated = entity.copy(isActive = !entity.isActive)
                                scope.launch(Dispatchers.IO) { dao.insert(updated) }
                            } else {
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
    gridSpacing: Int,
    cardHeight: Int,
    onGridColumnsChange: (Int) -> Unit,
    onGridSpacingChange: (Int) -> Unit,
    onCardHeightChange: (Int) -> Unit,
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

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val json = Gson().toJson(shortcuts)
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray())
                    android.widget.Toast.makeText(context, "データをエクスポートしました", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "エクスポート失敗: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = InputStreamReader(inputStream)
                    val type = object : TypeToken<List<ShortcutEntity>>() {}.type
                    val importedList: List<ShortcutEntity> = Gson().fromJson(reader, type)

                    scope.launch(Dispatchers.IO) {
                        importedList.forEach { item ->
                            onAdd(item.copy(id = 0))
                        }
                        launch(Dispatchers.Main) { 
                            android.widget.Toast.makeText(context, "${importedList.size}件のデータをインポートしました", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "インポート失敗: ファイル形式を確認してください", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("インテントパッド") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "メニュー")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("エクスポート (JSON保存)") },
                            onClick = {
                                exportLauncher.launch("intent_pad_data.json")
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Save, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("インポート (JSON読み込み)") },
                            onClick = {
                                importLauncher.launch(arrayOf("application/json"))
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Upload, null) }
                        )
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
            FloatingActionButton(
                onClick = { 
                    showDialog = true
                    itemToEdit = null
                }
            ) { 
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
                contentPadding = PaddingValues(gridSpacing.dp),
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(gridSpacing.dp),
                horizontalArrangement = Arrangement.spacedBy(gridSpacing.dp)
            ) {
                items(shortcuts) { item ->
                    ShortcutCard(
                        item = item,
                        cardHeight = cardHeight,
                        onTest = { onTest(item) },
                        onEdit = { 
                            itemToEdit = item
                            showDialog = true
                        },
                        onPin = { onPin(item) },
                        onDel = { onDel(item) }
                    )
                }
            }
        }

        if (showDialog) {
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

        if (showSettings) {
            SettingsDialog(
                gridColumns = gridColumns,
                gridSpacing = gridSpacing,
                cardHeight = cardHeight,
                onGridColumnsChange = onGridColumnsChange,
                onGridSpacingChange = onGridSpacingChange,
                onCardHeightChange = onCardHeightChange,
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
fun ShortcutCard(
    item: ShortcutEntity,
    cardHeight: Int,
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
                .height(cardHeight.dp)
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

    var name by remember { mutableStateOf(item?.label ?: "") }
    var action by remember { mutableStateOf(item?.actionName ?: "") }
    var emoji by remember { mutableStateOf(item?.iconEmoji ?: "🚀") }
    var imageUri by remember { mutableStateOf<String?>(item?.imageIconUri) }
    var isToggle by remember { mutableStateOf(item?.isToggle ?: false) }
    var secondaryAction by remember { mutableStateOf(item?.secondaryActionName ?: "") }
    
    val colors = listOf(
        0xFF1E88E5, 0xFFD81B60, 0xFF43A047, 0xFFFB8C00, 
        0xFF8E24AA, 0xFF546E7A, 0xFF00ACC1, 0xFF7CB342,
        0xFFF4511E, 0xFF5E35B1, 0xFF3949AB, 0xFFC0CA33
    )
    var selColor by remember { mutableStateOf(item?.colorHex ?: colors[0]) }

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
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("表示名*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank()
                )
                
                OutlinedTextField(
                    value = action,
                    onValueChange = { action = it },
                    label = { Text("インテントアクション*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = action.isBlank()
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isToggle,
                        onCheckedChange = { isToggle = it }
                    )
                    Text("トグル機能（ON/OFF切り替え）", style = MaterialTheme.typography.bodyMedium)
                }
                
                if (isToggle) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ON時のアクション: $action", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = secondaryAction,
                            onValueChange = { secondaryAction = it },
                            label = { Text("OFF時のアクション") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("省略するとONと同じアクションを使用") }
                        )
                    }
                }
                
                Divider()
                
                Text("アイコン設定", style = MaterialTheme.typography.titleSmall)
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("画像を選択")
                    }
                    
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { if (it.length <= 2) emoji = it },
                        label = { Text("絵文字") },
                        modifier = Modifier.width(100.dp),
                        placeholder = { Text("例: 🚀") }
                    )
                }
                
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
                
                Divider()
                
                Text("背景色", style = MaterialTheme.typography.titleSmall)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(colors) { c -> 
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .combinedClickable { selColor = c }
                                .border(
                                    width = if (selColor == c) 3.dp else 0.dp,
                                    color = if (selColor == c) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
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
                            secondaryActionName = if (isToggle) secondaryAction else null,
                            isActive = item?.isActive ?: true
                        )
                        onSave(newItem)
                    }
                },
                enabled = name.isNotBlank() && action.isNotBlank()
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
    gridSpacing: Int,
    cardHeight: Int,
    onGridColumnsChange: (Int) -> Unit,
    onGridSpacingChange: (Int) -> Unit,
    onCardHeightChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("レイアウト設定") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("グリッド列数", style = MaterialTheme.typography.titleMedium)
                        Text("${gridColumns}列", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = gridColumns.toFloat(),
                        onValueChange = { onGridColumnsChange(it.toInt()) },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(gridColumns) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
                
                Divider()
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("グリッド間隔", style = MaterialTheme.typography.titleMedium)
                        Text("${gridSpacing}dp", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = gridSpacing.toFloat(),
                        onValueChange = { onGridSpacingChange(it.toInt()) },
                        valueRange = 4f..24f,
                        steps = 4
                    )
                }
                
                Divider()
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("カード高さ", style = MaterialTheme.typography.titleMedium)
                        Text("${cardHeight}dp", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = cardHeight.toFloat(),
                        onValueChange = { onCardHeightChange(it.toInt()) },
                        valueRange = 80f..200f,
                        steps = 5
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardHeight.dp)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("カードプレビュー", style = MaterialTheme.typography.bodySmall)
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
}
