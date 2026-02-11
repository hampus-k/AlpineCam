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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import com.alpinecam.data.db.Skier
import com.alpinecam.data.db.SkierGroup
import com.alpinecam.data.db.SkierWithGroup
import kotlinx.coroutines.launch

class SkiersViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AlpineCamDatabase.getInstance(app)
    private val skierDao = db.skierDao()
    private val groupDao = db.groupDao()

    val skiers = skierDao.getAllWithGroup()
    val groups = groupDao.getAll()

    fun addSkier(name: String, groupId: Long?) {
        viewModelScope.launch { skierDao.insert(Skier(name = name, groupId = groupId)) }
    }

    fun updateSkier(id: Long, name: String, groupId: Long?) {
        viewModelScope.launch { skierDao.update(Skier(id = id, name = name, groupId = groupId)) }
    }

    fun deleteSkier(skier: SkierWithGroup) {
        viewModelScope.launch { skierDao.delete(Skier(id = skier.id, name = skier.name, groupId = skier.groupId)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkiersScreen(
    onBackClick: () -> Unit,
    viewModel: SkiersViewModel = viewModel(),
) {
    val skiers by viewModel.skiers.collectAsState(initial = emptyList())
    val groups by viewModel.groups.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSkier by remember { mutableStateOf<SkierWithGroup?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Åkare") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Lägg till åkare")
            }
        },
    ) { padding ->
        if (skiers.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Inga åkare ännu",
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
                items(skiers, key = { it.id }) { skier ->
                    SkierItem(
                        skier = skier,
                        onEdit = { editingSkier = skier },
                        onDelete = { viewModel.deleteSkier(skier) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        SkierDialog(
            title = "Ny åkare",
            initialName = "",
            initialGroupId = null,
            groups = groups,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, groupId ->
                viewModel.addSkier(name, groupId)
                showAddDialog = false
            },
        )
    }

    editingSkier?.let { skier ->
        SkierDialog(
            title = "Redigera åkare",
            initialName = skier.name,
            initialGroupId = skier.groupId,
            groups = groups,
            onDismiss = { editingSkier = null },
            onConfirm = { name, groupId ->
                viewModel.updateSkier(skier.id, name, groupId)
                editingSkier = null
            },
        )
    }
}

@Composable
private fun SkierItem(
    skier: SkierWithGroup,
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
            Column {
                Text(skier.name, style = MaterialTheme.typography.titleMedium)
                skier.groupName?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkierDialog(
    title: String,
    initialName: String,
    initialGroupId: Long?,
    groups: List<SkierGroup>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedGroupId by remember { mutableStateOf(initialGroupId) }
    var groupExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Namn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                ExposedDropdownMenuBox(
                    expanded = groupExpanded,
                    onExpandedChange = { groupExpanded = it },
                ) {
                    OutlinedTextField(
                        value = groups.find { it.id == selectedGroupId }?.name ?: "Ingen grupp",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Grupp") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = groupExpanded,
                        onDismissRequest = { groupExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ingen grupp") },
                            onClick = {
                                selectedGroupId = null
                                groupExpanded = false
                            },
                        )
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    selectedGroupId = group.id
                                    groupExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), selectedGroupId) },
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
