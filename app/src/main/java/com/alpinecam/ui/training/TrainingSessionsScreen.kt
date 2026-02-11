package com.alpinecam.ui.training

import android.app.Application
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.alpinecam.data.db.Discipline
import com.alpinecam.data.db.SessionWithDetails
import com.alpinecam.data.db.TrainingSession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrainingSessionsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AlpineCamDatabase.getInstance(app).trainingSessionDao()
    val sessions = dao.getAllWithDetails()

    fun createSession(discipline: Discipline): Long {
        var id = 0L
        viewModelScope.launch {
            id = dao.insert(
                TrainingSession(
                    date = System.currentTimeMillis(),
                    discipline = discipline,
                )
            )
        }
        return id
    }

    fun deleteSession(session: SessionWithDetails) {
        viewModelScope.launch {
            dao.delete(TrainingSession(id = session.id, date = session.date, discipline = session.discipline))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingSessionsScreen(
    onBackClick: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onNewSession: (Long) -> Unit,
    viewModel: TrainingSessionsViewModel = viewModel(),
) {
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())
    var showNewSessionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Träningar") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewSessionDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ny träning")
            }
        },
    ) { padding ->
        if (sessions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Inga träningar ännu", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionItem(
                        session = session,
                        onClick = { onSessionClick(session.id) },
                        onDelete = { viewModel.deleteSession(session) },
                    )
                }
            }
        }
    }

    if (showNewSessionDialog) {
        NewSessionDialog(
            onDismiss = { showNewSessionDialog = false },
            onCreate = { discipline ->
                showNewSessionDialog = false
                viewModel.viewModelScope.launch {
                    val dao = AlpineCamDatabase.getInstance(viewModel.getApplication()).trainingSessionDao()
                    val id = dao.insert(
                        TrainingSession(date = System.currentTimeMillis(), discipline = discipline)
                    )
                    onNewSession(id)
                }
            },
        )
    }
}

@Composable
private fun SessionItem(
    session: SessionWithDetails,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale("sv")) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "${session.discipline.name} — ${dateFormat.format(Date(session.date))}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${session.skierCount} åkare, ${session.videoCount} videos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Ta bort")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewSessionDialog(
    onDismiss: () -> Unit,
    onCreate: (Discipline) -> Unit,
) {
    var selectedDiscipline by remember { mutableStateOf(Discipline.SL) }
    val disciplines = Discipline.entries

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ny träning") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Välj gren:")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    disciplines.forEachIndexed { index, discipline ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = disciplines.size),
                            onClick = { selectedDiscipline = discipline },
                            selected = selectedDiscipline == discipline,
                        ) {
                            Text(discipline.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(selectedDiscipline) }) {
                Text("Skapa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        },
    )
}
