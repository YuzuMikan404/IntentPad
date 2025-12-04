package com.github.intent.pad

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
                        onDel = { scope.launch(Dispatchers.IO) { dao.delete(it) } },
                        onPin = { ShortcutUtils.pinShortcut(this, it); Toast.makeText(this, "Pinned", Toast.LENGTH_SHORT).show() },
                        onTest = { ShortcutUtils.sendBroadcast(this, it.actionName); Toast.makeText(this, "Sent", Toast.LENGTH_SHORT).show() }
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
    onDel: (ShortcutEntity) -> Unit,
    onPin: (ShortcutEntity) -> Unit,
    onTest: (ShortcutEntity) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Intent Pad") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) },
        floatingActionButton = { FloatingActionButton(onClick = { showDialog = true }) { Icon(Icons.Default.Add, "Add") } }
    ) { padding ->
        if (shortcuts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Press + to create") }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(140.dp), contentPadding = PaddingValues(16.dp), modifier = Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shortcuts) { item ->
                    ShortcutCard(item, { onTest(item) }, { onPin(item) }, { onDel(item) })
                }
            }
        }
        if (showDialog) EditDialog({ showDialog = false }, { onAdd(it); showDialog = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortcutCard(item: ShortcutEntity, onClick: () -> Unit, onLong: () -> Unit, onDel: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Card(colors = CardDefaults.cardColors(containerColor = Color(item.colorHex)), modifier = Modifier.height(140.dp).fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { showMenu = true })) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(item.iconEmoji, style = MaterialTheme.typography.displayMedium)
                Text(item.label, style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Pin to Home") }, onClick = { onLong(); showMenu = false })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { onDel(); showMenu = false }, leadingIcon = { Icon(Icons.Default.Delete, null) })
        }
    }
}

@Composable
fun EditDialog(onDismiss: () -> Unit, onSave: (ShortcutEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🚀") }
    val colors = listOf(0xFF1E88E5, 0xFFD81B60, 0xFF43A047, 0xFFFB8C00, 0xFF8E24AA, 0xFF546E7A)
    var selColor by remember { mutableStateOf(colors[0]) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Trigger") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Label") })
                OutlinedTextField(action, { action = it }, label = { Text("Intent Action") })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedTextField(emoji, { if(it.length<=2) emoji=it }, label = { Text("Icon") }, modifier = Modifier.width(100.dp))
                    Row { colors.forEach { c -> Box(Modifier.size(32.dp).clip(CircleShape).background(Color(c)).combinedClickable{ selColor = c }) } }
                }
            }
        },
        confirmButton = { Button(onClick = { if(name.isNotBlank()) onSave(ShortcutEntity(0, name, action, emoji, selColor)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
