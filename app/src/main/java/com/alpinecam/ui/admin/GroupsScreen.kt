package com.alpinecam.ui.admin

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpinecam.data.db.AlpineCamDatabase
import com.alpinecam.data.db.SkierGroup
import kotlinx.coroutines.launch

class GroupsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AlpineCamDatabase.getInstance(app).groupDao()
    val groups = dao.getAll()

    fun addGroup(name: String) {
        viewModelScope.launch { dao.insert(SkierGroup(name = name)) }
    }

    fun updateGroup(group: SkierGroup) {
        viewModelScope.launch { dao.update(group) }
    }

    fun deleteGroup(group: SkierGroup) {
        viewModelScope.launch { dao.delete(group) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    onBackClick: () -> Unit,
    viewModel: GroupsViewModel = viewModel(),
) {
    val groups by viewModel.groups.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<SkierGroup?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grupper") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Lägg till grupp")
            }
        },
    ) { padding ->
        if (groups.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Inga grupper ännu",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Tryck + för att lägga till",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(groups, key = { it.id }) { group ->
                    GroupItem(
                        group = group,
                        onEdit = { editingGroup = group },
                        onDelete = { viewModel.deleteGroup(group) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        GroupDialog(
            title = "Ny grupp",
            initialName = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                viewModel.addGroup(name)
                showAddDialog = false
            },
        )
    }

    editingGroup?.let { group ->
        GroupDialog(
            title = "Redigera grupp",
            initialName = group.name,
            onDismiss = { editingGroup = null },
            onConfirm = { name ->
                viewModel.updateGroup(group.copy(name = name))
                editingGroup = null
            },
        )
    }
}

@Composable
private fun GroupItem(
    group: SkierGroup,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(group.name, style = MaterialTheme.typography.titleMedium)
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Redigera")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Ta bort")
                }
            }
        }
    }
}

@Composable
private fun GroupDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Gruppnamn") },
                placeholder = { Text("t.ex. U14") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        },
    )
}
