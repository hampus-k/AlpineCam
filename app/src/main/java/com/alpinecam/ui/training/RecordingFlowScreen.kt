package com.alpinecam.ui.training

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpinecam.data.db.AlpineCamDatabase
import com.alpinecam.data.db.Discipline
import com.alpinecam.data.db.SessionSkier
import com.alpinecam.data.db.SkierWithGroup
import com.alpinecam.data.db.TrainingSession
import com.alpinecam.data.db.VideoRecording
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RecordingStep {
    PICK_DISCIPLINE,
    PICK_SKIER,
    RECORDING,
    CONFIRM_SAVE,
}

data class RecordingFlowState(
    val step: RecordingStep = RecordingStep.PICK_DISCIPLINE,
    val sessionId: Long? = null,
    val discipline: Discipline? = null,
    val selectedSkier: SkierWithGroup? = null,
    val lastRecordedUri: Uri? = null,
)

class RecordingFlowViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AlpineCamDatabase.getInstance(app)
    private val sessionDao = db.trainingSessionDao()
    private val skierDao = db.skierDao()
    private val videoDao = db.videoRecordingDao()

    val allSkiers = skierDao.getAllWithGroup()

    private val _flowState = MutableStateFlow(RecordingFlowState())
    val flowState: StateFlow<RecordingFlowState> = _flowState.asStateFlow()

    fun initWithSession(sessionId: Long) {
        viewModelScope.launch {
            val session = sessionDao.getById(sessionId) ?: return@launch
            _flowState.value = RecordingFlowState(
                step = RecordingStep.PICK_SKIER,
                sessionId = sessionId,
                discipline = session.discipline,
            )
        }
    }

    fun selectDiscipline(discipline: Discipline) {
        viewModelScope.launch {
            val sessionId = sessionDao.insert(
                TrainingSession(date = System.currentTimeMillis(), discipline = discipline)
            )
            _flowState.value = _flowState.value.copy(
                step = RecordingStep.PICK_SKIER,
                sessionId = sessionId,
                discipline = discipline,
            )
        }
    }

    fun selectSkier(skier: SkierWithGroup) {
        val sessionId = _flowState.value.sessionId ?: return
        viewModelScope.launch {
            sessionDao.addSkierToSession(SessionSkier(sessionId = sessionId, skierId = skier.id))
        }
        _flowState.value = _flowState.value.copy(
            step = RecordingStep.RECORDING,
            selectedSkier = skier,
        )
    }

    fun onVideoRecorded(uri: Uri) {
        _flowState.value = _flowState.value.copy(
            step = RecordingStep.CONFIRM_SAVE,
            lastRecordedUri = uri,
        )
    }

    fun confirmSave() {
        val state = _flowState.value
        val sessionId = state.sessionId ?: return
        val skierId = state.selectedSkier?.id ?: return
        val uri = state.lastRecordedUri ?: return

        viewModelScope.launch {
            videoDao.insert(
                VideoRecording(
                    sessionId = sessionId,
                    skierId = skierId,
                    videoUri = uri.toString(),
                )
            )
        }
        // Go back to skier selection for next run
        _flowState.value = state.copy(
            step = RecordingStep.PICK_SKIER,
            selectedSkier = null,
            lastRecordedUri = null,
        )
    }

    fun discardAndRetry() {
        _flowState.value = _flowState.value.copy(
            step = RecordingStep.RECORDING,
            lastRecordedUri = null,
        )
    }

    fun discardAndPickSkier() {
        _flowState.value = _flowState.value.copy(
            step = RecordingStep.PICK_SKIER,
            selectedSkier = null,
            lastRecordedUri = null,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingFlowScreen(
    sessionId: Long?,
    onOpenCamera: (sessionId: Long, skierId: Long, skierName: String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: RecordingFlowViewModel = viewModel(),
) {
    val flowState by viewModel.flowState.collectAsState()
    val allSkiers by viewModel.allSkiers.collectAsState(initial = emptyList())

    LaunchedEffect(sessionId) {
        if (sessionId != null && sessionId > 0) {
            viewModel.initWithSession(sessionId)
        }
    }

    when (flowState.step) {
        RecordingStep.PICK_DISCIPLINE -> {
            DisciplinePickerScreen(
                onDisciplineSelected = { viewModel.selectDiscipline(it) },
                onBackClick = onBackClick,
            )
        }
        RecordingStep.PICK_SKIER -> {
            SkierPickerScreen(
                discipline = flowState.discipline,
                skiers = allSkiers,
                onSkierSelected = { viewModel.selectSkier(it) },
                onBackClick = onBackClick,
            )
        }
        RecordingStep.RECORDING -> {
            val sid = flowState.sessionId ?: return
            val skier = flowState.selectedSkier ?: return
            onOpenCamera(sid, skier.id, skier.name)
        }
        RecordingStep.CONFIRM_SAVE -> {
            ConfirmSaveDialog(
                skierName = flowState.selectedSkier?.name ?: "",
                onSave = { viewModel.confirmSave() },
                onRetry = { viewModel.discardAndRetry() },
                onDiscard = { viewModel.discardAndPickSkier() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisciplinePickerScreen(
    onDisciplineSelected: (Discipline) -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Välj gren") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Vilken gren tränar ni?", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Discipline.entries.forEach { discipline ->
                val label = when (discipline) {
                    Discipline.SL -> "Slalom (SL)"
                    Discipline.GS -> "Storslalom (GS)"
                    Discipline.SG -> "Super-G (SG)"
                    Discipline.DH -> "Störtlopp (DH)"
                    Discipline.AC -> "Alpin kombination (AC)"
                }
                Button(
                    onClick = { onDisciplineSelected(discipline) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(label)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkierPickerScreen(
    discipline: Discipline?,
    skiers: List<SkierWithGroup>,
    onSkierSelected: (SkierWithGroup) -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(discipline?.let { "Välj åkare — ${it.name}" } ?: "Välj åkare")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka")
                    }
                },
            )
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
                    "Inga åkare registrerade",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "Lägg till åkare under Administration först",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(skiers, key = { it.id }) { skier ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSkierSelected(skier) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(skier.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                skier.groupName?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmSaveDialog(
    skierName: String,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Spara inspelning?") },
        text = {
            Text("Vill du spara denna video på $skierName?")
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Icon(Icons.Default.Check, contentDescription = null)
                Text("  Spara")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRetry) {
                    Icon(Icons.Default.Videocam, contentDescription = null)
                    Text("  Filma igen")
                }
                TextButton(onClick = onDiscard) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Text("  Kassera")
                }
            }
        },
    )
}
